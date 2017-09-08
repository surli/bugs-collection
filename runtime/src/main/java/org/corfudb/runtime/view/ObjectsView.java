package org.corfudb.runtime.view;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.protocols.wireprotocol.TxResolutionInfo;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.runtime.exceptions.AbortCause;
import org.corfudb.runtime.exceptions.TransactionAbortedException;
import org.corfudb.runtime.object.CorfuCompileWrapperBuilder;
import org.corfudb.runtime.object.ICorfuSMR;
import org.corfudb.runtime.object.transactions.AbstractTransactionalContext;
import org.corfudb.runtime.object.transactions.TransactionBuilder;
import org.corfudb.runtime.object.transactions.TransactionType;
import org.corfudb.runtime.object.transactions.TransactionalContext;
import org.corfudb.runtime.view.stream.IStreamView;
import org.corfudb.util.serializer.Serializers;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A view of the objects inside a Corfu instance.
 * Created by mwei on 1/7/16.
 */
@Slf4j
public class ObjectsView extends AbstractView {

    /**
     * The Transaction stream is used to log/write successful transactions from different clients.
     * Transaction data and meta data can be obtained by reading this stream.
     */
    static public UUID TRANSACTION_STREAM_ID = CorfuRuntime.getStreamID("Transaction_Stream");

    @Getter
    @Setter
    boolean transactionLogging = false;


    @Getter
    Map<ObjectID, Object> objectCache = new ConcurrentHashMap<>();

    public ObjectsView(@Nonnull final CorfuRuntime runtime) {
        super(runtime);
    }

    /**
     * Return an object builder which builds a new object.
     *
     * @return An object builder to open an object with.
     */
    public ObjectBuilder<?> build() {
        return new ObjectBuilder(runtime);
    }

    /**
     * Creates a copy-on-append copy of an object.
     *
     * @param obj         The object that should be copied.
     * @param destination The destination ID of the object to be copied.
     * @param <T>         The type of the object being copied.
     * @return A copy-on-append copy of the object.
     */
    @SuppressWarnings("unchecked")
    public <T> T copy(@NonNull T obj, @NonNull UUID destination) {
        ICorfuSMR<T> proxy = (ICorfuSMR<T>)obj;
        ObjectID oid = new ObjectID(destination, proxy.getCorfuSMRProxy().getObjectType());
        return (T) objectCache.computeIfAbsent(oid, x -> {
            IStreamView sv = runtime.getStreamsView().copy(proxy.getCorfuStreamID(),
                    destination, proxy.getCorfuSMRProxy().getVersion());
            try {
                return
                        CorfuCompileWrapperBuilder.getWrapper(proxy.getCorfuSMRProxy().getObjectType(),
                                runtime, sv.getID(), null, Serializers.JSON);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    /**
     * Creates a copy-on-append copy of an object.
     *
     * @param obj         The object that should be copied.
     * @param destination The destination stream name of the object to be copied.
     * @param <T>         The type of the object being copied.
     * @return A copy-on-append copy of the object.
     */
    @SuppressWarnings("unchecked")
    public <T> T copy(@NonNull T obj, @NonNull String destination) {
        return copy(obj, CorfuRuntime.getStreamID(destination));
    }

    /**
     * Begins a transaction on the current thread.
     * Automatically selects the correct transaction strategy.
     * Modifications to objects will not be visible
     * to other threads or clients until TXEnd is called.
     */
    public void TXBegin() {
        TXBuild()
                .setType(TransactionType.OPTIMISTIC)
                .begin();
    }

    /** Builds a new transaction using the transaction
     * builder.
     * @return  A transaction builder to build a transaction with.
     */
    public TransactionBuilder TXBuild() {
        return new TransactionBuilder(runtime);
    }

    /**
     * Aborts a transaction on the current thread.
     * Modifications to objects in the current transactional
     * context will be discarded.
     */
    public void TXAbort() {
        AbstractTransactionalContext context = TransactionalContext.getCurrentContext();
        if (context == null) {
            log.warn("Attempted to abort a transaction, but no transaction active!");
        } else {
            TxResolutionInfo txInfo = new TxResolutionInfo(
                    context.getTransactionID(), context.getSnapshotTimestamp());
            context.abortTransaction(new TransactionAbortedException(
                    txInfo, null, AbortCause.USER));
            TransactionalContext.removeContext();
        }
    }

    /**
     * Query whether a transaction is currently running.
     *
     * @return True, if called within a transactional context,
     * False, otherwise.
     */
    public boolean TXActive() {
        return TransactionalContext.isInTransaction();
    }

    /**
     * End a transaction on the current thread.
     *
     * @throws TransactionAbortedException If the transaction could not be executed successfully.
     *
     * @return The address of the transaction, if it commits.
     */
    public long TXEnd()
            throws TransactionAbortedException {
        AbstractTransactionalContext context = TransactionalContext.getCurrentContext();
        if (context == null) {
            log.warn("Attempted to end a transaction, but no transaction active!");
            return AbstractTransactionalContext.UNCOMMITTED_ADDRESS;
        } else {
            // TODO remove this, doesn't belong here!
            long totalTime = System.currentTimeMillis() - context.getStartTime();
            log.trace("TXCommit[{}] time={} ms",
                    context, totalTime);
            // TODO up to here
                try {
                    return TransactionalContext.getCurrentContext().commitTransaction();
                } finally {
                    TransactionalContext.removeContext();
                }
        }
    }

    /** Given a Corfu object, syncs the object to the most up to date version.
     * If the object is not a Corfu object, this function won't do anything.
     * @param object    The Corfu object to sync.
     */
    public void syncObject(Object object) {
        if (object instanceof ICorfuSMR<?>) {
            ICorfuSMR<?> corfuObject = (ICorfuSMR<?>) object;
            corfuObject.getCorfuSMRProxy().sync();
        }
    }

    /** Given a list of Corfu objects, syncs the objects to the most up to date
     * version, possibly in parallel.
     * @param objects   A list of Corfu objects to sync.
     */
    public void syncObject(Object... objects) {
        Arrays.stream(objects)
                .parallel()
                .filter(x -> x instanceof ICorfuSMR<?>)
                .map(x -> (ICorfuSMR<?>) x)
                .forEach(x -> x.getCorfuSMRProxy().sync());
    }

    @Data
    public static class ObjectID<T> {
        final UUID streamID;
        final Class<T> type;
    }
}
