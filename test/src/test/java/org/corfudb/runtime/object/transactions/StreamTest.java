package org.corfudb.runtime.object.transactions;

import org.corfudb.runtime.collections.ISMRMap;
import org.corfudb.runtime.collections.SMRMap;
import org.corfudb.runtime.exceptions.TransactionAbortedException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * These tests generate workloads with mixed reads/writes on multiple maps.
 *
 * The tests previously surfaced a concurrency bug in navigating streams
 * by concurrent threads. It's good to leave it in.
 *
 * Created by dmalkhi on 12/13/16.
 */
public class StreamTest extends AbstractTransactionsTest {
    @Override
    public void TXBegin() { OptimisticTXBegin(); }



    /**
     * This workload operates over three distinct maps
     */
    ISMRMap<String, Integer> map1, map2, map3;

    private final int NUM_BATCHES = 3;
    private final int BATCH_SZ = 1_0;
    private final int numTasks = NUM_BATCHES * BATCH_SZ;

    /**
     * Set up a repeatable PRNG
     */
    private final int SEED = 434343;
    Random rand = new Random(SEED);

    private final int READ_PERCENT = 80;
    private final int MAX_PERCENT = 100;

    @Test
    public void mixBackpointers() {
        final int smallNumber = 5;

        /**
         * Instantiate three streams with three SMRmap objects
         */
        map1 = instantiateCorfuObject(SMRMap.class, "A"); map1.clear();
        map2 = instantiateCorfuObject(SMRMap.class, "B"); map2.clear();
        map3 = instantiateCorfuObject(SMRMap.class, "foo"); map3.clear();


        // generate multi-stream entries
        WWTXBegin();
        for (int i = 0; i < smallNumber; i++) {
            map1.put("m1" + i, i);
            map2.put("m2" + i, i);
            map3.put("m3" + i, i);
        }
        TXEnd();


        final int concurrency = 4;

        for (int j = 0; j < concurrency; j++)
            t(j, () -> {
                WWTXBegin();
            });

        t(concurrency, () -> { WWTXBegin(); map2.put("foo", 0); TXEnd(); });

        // now, thread 0..concurrency-1 have to go back in
        // the stream of map3 to its snapshot-position
        for (int j = 0; j < concurrency; j++) {
            final int threadNum = j;
            t(threadNum, () -> {
                try {
                    map3.put("bar" + threadNum, 0);
                    TXEnd();
                } catch (NullPointerException ne) {
                    throw new RuntimeException();
                }
            });
        }
    }

    /**
     * This method initiates all the data structures for this program
     */
    public void generateMaps() {
        /**
         * Instantiate three streams with three SMRmap objects
         */
        map1 = instantiateCorfuObject(SMRMap.class, "A");
        map2 = instantiateCorfuObject(SMRMap.class, "B");
        map3 = instantiateCorfuObject(SMRMap.class, "foo");

        // populate maps
        for (int i = 0; i < NUM_BATCHES; i++) {
            WWTXBegin();
            for (int j = 0; j < BATCH_SZ; j++) {
                map1.put("m1" + (i * BATCH_SZ + j), i);
                map2.put("m2" + (i * BATCH_SZ + j), i);
                map3.put("m3" + (i * BATCH_SZ + j), i);
            }
            TXEnd();
        }
    }

    /**
     * generate a workload with mixed R/W TX's
     *   - NUM_BATCHES TX's
     *   - each TX performs BATCH_SZ operations, mixing R/W according to the specified ratio.
     *
     * @param readPrecent ratio of reads (to 100)
     */
    public void concurrentStreamRWLoad(int ignoredThreadNum, int readPrecent) {
        final AtomicInteger aborts = new AtomicInteger(0);

        for (int i = 0; i < NUM_BATCHES; i++) {
            final int fi = i;

            addTestStep(task_num -> {
                WWTXBegin();
            });

            for (int j = 0; j < BATCH_SZ; j++) {
                final int fj = j;

                addTestStep(task_num -> {

                    int r1 = rand.nextInt(numTasks);
                    int r2 = rand.nextInt(numTasks);
                    int r3 = rand.nextInt(numTasks);
                    int accumulator = 0;
                    accumulator += map1.get("m1" + r1);
                    accumulator += map2.get("m2" + r2);
                    accumulator += map3.get("m3" + r3);

                    // perform random put()'s with probability '1 - readPercent/100'
                    if (rand.nextInt(MAX_PERCENT) >= readPrecent) {
                        if (rand.nextInt(2) == 1)
                            map2.put("m2" + rand.nextInt(numTasks), accumulator);
                        else
                            map3.put("m3" + rand.nextInt(numTasks), accumulator);
                    }
                });
            }

            addTestStep(task_num -> {
                try {
                    TXEnd();
                } catch (TransactionAbortedException te) {
                    aborts.getAndIncrement();
                }
            });
        }

        addTestStep(task_num ->
                calculateAbortRate(aborts.get(), numTasks)
        );

    }

    /**
     * This is where activity is started
     */
    @Test
    public void testConcurrentStreamRW() {

        final int NUM_THREADS = 3;
        ArrayList<Thread> tList = new ArrayList<>();

        // generate the maps and populate with elements
        generateMaps();
        concurrentStreamRWLoad(-1, READ_PERCENT);
        scheduleInterleaved(NUM_THREADS, NUM_THREADS);
    }


}
