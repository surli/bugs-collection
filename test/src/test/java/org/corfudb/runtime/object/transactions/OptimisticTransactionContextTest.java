package org.corfudb.runtime.object.transactions;

import org.corfudb.runtime.exceptions.TransactionAbortedException;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by mwei on 11/16/16.
 */
public class OptimisticTransactionContextTest extends AbstractTransactionContextTest {

    /** In an optimistic transaction, we should be able to
     *  read our own writes in the same thread.
     */
    @Test
    public void readOwnWrites()
    {
        t(1, this::TXBegin);
        t(1, () -> put("k" , "v"));
        t(1, () -> get("k"))
                            .assertResult()
                            .isEqualTo("v");
        t(1, this::TXEnd);
    }

    /** We should not be able to read writes written optimistically
     * by other threads.
     */
    @Test
    public void otherThreadCannotReadOptimisticWrites()
    {
        t(1, this::TXBegin);
        t(2, this::TXBegin);
        // T1 inserts k,v1 optimistically. Other threads
        // should not see this optimistic put.
        t(1, () -> put("k", "v1"));
        // T2 now reads k. It should not see T1's write.
        t(2, () -> get("k"))
                            .assertResult()
                            .isNull();
        // T2 inserts k,v2 optimistically. T1 should not
        // be able to see this write.
        t(2, () -> put("k", "v2"));
        // T1 now reads "k". It should not see T2's write.
        t(1, () -> get("k"))
                            .assertResult()
                            .isNotEqualTo("v2");
    }

    /** Threads that start a transaction at the same time
     * (with the same timestamp) should cause one thread
     * to abort while the other succeeds.
     */
    @Test
    public void threadShouldAbortAfterConflict()
    {
        // T1 starts non-transactionally.
        t(1, () -> put("k", "v0"));
        t(1, () -> put("k1", "v1"));
        t(1, () -> put("k2", "v2"));
        // Now T1 and T2 both start transactions and read v0.
        t(1, this::TXBegin);
        t(2, this::TXBegin);
        t(1, () -> get("k"))
                    .assertResult()
                    .isEqualTo("v0");
        t(2, () -> get("k"))
                    .assertResult()
                    .isEqualTo("v0");
        // Now T1 modifies k -> v1 and commits.
        t(1, () -> put("k", "v1"));
        t(1, this::TXEnd);
        // And T2 modifies k -> v2 and tries to commit, but
        // should abort.
        t(2, () -> put("k", "v2"));
        t(2, this::TXEnd)
                    .assertThrows()
                    .isInstanceOf(TransactionAbortedException.class);
        // At the end of the transaction, the map should only
        // contain T1's modification.
        assertThat(getMap())
                .containsEntry("k", "v1");
    }

    /** This test makes sure that a single thread can read
     * its own nested transactions after they have committed,
     * and that nested transactions are committed with the
     * parent transaction.
     */
    @Test
    public void nestedTransactionsCanBeReadDuringCommit() {
        // We start without a transaction and put k,v1
        t(1, () -> put("k", "v1"));
        // Now we start a transaction and put k,v2
        t(1, this::TXBegin);
        t(1, () -> put("k", "v2"))
                    .assertResult() // put should return the previous value
                    .isEqualTo("v1"); // which is v1.
        // Now we start a nested transaction. It should
        // read v2.
        t(1, this::TXBegin);
        t(1, () -> get("k"))
                    .assertResult()
                    .isEqualTo("v2");
        // Now we put k,v3
        t(1, () -> put("k", "v3"))
                    .assertResult()
                    .isEqualTo("v2");   // previous value = v2
        // And then we commit.
        t(1, this::TXEnd);
        // And we should be able to read the nested put
        t(1, () -> get("k"))
                .assertResult()
                .isEqualTo("v3");
        // And we commit the parent transaction.
        t(1, this::TXEnd);

        // And now k,v3 should be in the map.
        assertThat(getMap())
                .containsEntry("k", "v3");
    }

    /** This test makes sure that the nested transactions
     * of two threads are not visible to each other.
     */
    @Test
    public void nestedTransactionsAreIsolatedAcrossThreads() {
        // Start a transaction on both threads.
        t(1, this::TXBegin);
        t(2, this::TXBegin);
        // Put k, v1 on T1 and k, v2 on T2.
        t(1, () -> put("k", "v1"));
        t(2, () -> put("k", "v2"));
        // Now, start a nested transaction on both threads.
        t(1, this::TXBegin);
        t(2, this::TXBegin);
        // T1 should see v1 and T2 should see v2.
        t(1, () -> get("k"))
                .assertResult()
                .isEqualTo("v1");
        t(2, () -> get("k"))
                .assertResult()
                .isEqualTo("v2");
        // Now we put k,v3 on T1 and k,v4 on T2
        t(1, () -> put("k", "v3"));
        t(2, () -> put("k", "v4"));
        // And each thread should only see its own modifications.
        t(1, () -> get("k"))
                .assertResult()
                .isEqualTo("v3");
        t(2, () -> get("k"))
                .assertResult()
                .isEqualTo("v4");
        // Now we exit the nested transaction. They should both
        // commit, because they are in optimistic mode.
        t(1, this::TXEnd);
        t(2, this::TXEnd);
        // Check that the parent transaction can only
        // see the correct modifications.
        t(1, () -> get("k"))
                .assertResult()
                .isEqualTo("v3");
        t(2, () -> get("k"))
                .assertResult()
                .isEqualTo("v4");
        // Commit the parent transactions. T2 should abort
        // due to concurrent modification with T1.
        t(1, this::TXEnd);
        t(2, this::TXEnd)
               .assertThrows()
               .isInstanceOf(TransactionAbortedException.class);

        // And the map should contain k,v3 - T1's update.
        assertThat(getMap())
                .containsEntry("k", "v3")
                .doesNotContainEntry("k", "v4");
    }

    /** This test makes sure that a write-only transaction properly
     * commits its updates, even if there are no accesses
     * during the transaction.
     */
    @Test
    public void writeOnlyTransactionCommitsInMemory() {
        // Write twice to the transaction without a read
        TXBegin();
        write("k", "v1");
        write("k", "v2");
        TXEnd();

        // Make sure the object correctly reflects the value
        // of the most recent write.
        assertThat(getMap())
                .containsEntry("k", "v2");
    }

    @Override
    protected void TXBegin() {
        getRuntime().getObjectsView().TXBuild()
                .setType(TransactionType.OPTIMISTIC)
                .begin();
    }
}
