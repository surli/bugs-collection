package org.corfudb.runtime.view.stream;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.protocols.logprotocol.StreamCOWEntry;
import org.corfudb.protocols.wireprotocol.LogData;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.runtime.view.Address;

import java.util.NavigableSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/** The abstract queued stream view implements a stream backed by a read queue.
 *
 * A read queue is a priority queue where addresses can be inserted, and are
 * dequeued in ascending order. Subclasses implement the fillReadQueue()
 * function, which defines how the read queue should be filled, and the
 * readAndUpdatePointers() function, which reads an entry and updates the
 * pointers for the stream view.
 *
 * It is up to the implementation as to what type of addresses to put in the
 * read queue (they may be stream or global addresses, for example). However,
 * each implementation must maintain and update the global pointer, since
 * consumers of the stream view expect to filter reads from the view by the
 * global address.
 *
 * This implementation uses "contexts" to properly deal with copy-on-write
 * streams. Every time a stream is copied, a new context is created which
 * redirects requests to the source stream for the copy - each context
 * contains its own queue and pointers. Implementers of fillReadQueue() and
 * readAndUpdatePointers should be careful to use the id of the context,
 * rather than that of the stream view itself.
 *
 * This implementation does not handle bulk reads and depends on IStreamView's
 * implementation of remainingUpTo(), which simply calls nextUpTo() under a lock
 * until it returns null.
 *
 * Created by mwei on 1/6/17.
 */
@Slf4j
public abstract class AbstractQueuedStreamView
        implements IStreamView, AutoCloseable {

    /**
     * The ID of the stream.
     */
    @Getter
    final UUID streamID;

    /**
     * An ordered set of stream contexts, which store information
     * about a stream copied via copy-on-write entries. Streams which
     * have never been copied have only a single context.
     */
    final NavigableSet<StreamContext> streamContexts;

    /**
     * The runtime the stream view was created with.
     */
    final CorfuRuntime runtime;

    /** Create a new queued stream view.
     *
     * @param streamID  The ID of the stream
     * @param runtime   The runtime used to create this view.
     */
    public AbstractQueuedStreamView(final CorfuRuntime runtime,
                                    final UUID streamID) {
        this.streamID = streamID;
        this.runtime = runtime;
        this.streamContexts = new ConcurrentSkipListSet<>();
        this.streamContexts.add(new StreamContext(streamID, Address.MAX));
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void reset() {
        this.streamContexts.clear();
        this.streamContexts.add(new StreamContext(streamID, Address.MAX));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {}

    /** {@inheritDoc}
     */
    public synchronized LogData nextUpTo(long maxGlobal) {
        // Pop the context if it has changed.
        if (getCurrentContext().globalPointer >=
                getCurrentContext().maxAddress) {
            StreamContext last = streamContexts.pollFirst();
            log.trace("Completed context {}@{}, removing.",
                    last.id, last.maxAddress);
        }

        // If we have no entries to read, fill the read queue.
        if (getCurrentContext().readQueue.isEmpty()) {
            fillReadQueue(maxGlobal, getCurrentContext());
            // If we still have no entries to read, there's nothing in
            // the stream (we linearized the read during fillReadQueue()).
            if (getCurrentContext().readQueue.isEmpty()) {
                return null;
            }
        }

        // Otherwise we remove entries one at a time from the read queue,
        // If we exceed maxGlobal or run out of entries, we return null, else
        // we return the data.;

        while ( getCurrentContext().readQueue.size() > 0) {
            final long thisRead = getCurrentContext().readQueue.pollFirst();
            LogData ld = readAndUpdatePointers
                    (getCurrentContext(), thisRead, maxGlobal);
            if (ld == null) {
                // This means we exceeded maxGlobal, so we need to
                // add this read back into the queue and return null
                getCurrentContext().readQueue.add(thisRead);
                return null;
            }
            else if (ld.containsStream(getCurrentContext().id)) {
                // If this is a copy-on-write entry, add a new stream
                // context.
                Object res = ld.getPayload(runtime);
                if (res instanceof StreamCOWEntry) {
                    StreamCOWEntry ce = (StreamCOWEntry) res;
                    streamContexts
                            .add(new StreamContext(ce.getOriginalStream(),
                                    ce.getFollowUntil()));
                    // To follow the underlying stream, we'll call
                    // nextUpTo again.
                    return nextUpTo(maxGlobal);
                } else {
                    // Otherwise return the log data.
                    return ld;
                }
            }
        }

        // No entries were available, so we return null.
        return null;
    }

    /**
     * Retrieve the data at the given address which was previously
     * inserted into the read queue, and update any pointers.
     *
     * For cases where the address is not the global address, this
     * function may return null, to indicate that the address given
     * exceeds the global address.
     *
     * @param context       The context we are reading from.
     * @param address       The address to read.
     * @param maxGlobal     The maximum global address to read to.
     * @return              The entry at the given address.
     */
    abstract protected LogData readAndUpdatePointers
                                    (final StreamContext context,
                                     final long address, final long maxGlobal);

    /**
     * Fill the read queue for the current context. This method is called
     * whenever a client requests a read, but there are no addresses left in
     * the read queue.
     *
     * @param maxGlobal     The maximum global address to read to.
     * @param context       The current stream context.
     */
    abstract protected void fillReadQueue(final long maxGlobal,
                                          final StreamContext context);

    /** Get the current context. */
    protected StreamContext getCurrentContext() {
        return streamContexts.first();
    }

    /** A data class which keeps data for each stream context.
     * Stream contexts represent a copy-on-write context - for each source
     * stream a single context is used up until maxAddress, at which the new
     * *destination* stream is used by popping the context off the stream
     * context stack.
     */
    @ToString
    class StreamContext implements Comparable<StreamContext> {

        /**
         * The ID (stream ID) of this context.
         */
        final UUID id;

        /**
         * The maximum global address that we should follow to, or
         * Address.MAX, if this is the final context.
         */
        final long maxAddress;

        /**
         * A pointer to the current global address, which is the
         * global address of the most recently added entry.
         */
        long globalPointer;

        /**
         * A pointer to the current stream address. This is usually the
         * number of entries returned so far by this context.
         */
        long streamPointer;

        /**
         * A priority queue of potential addresses to be written. The type
         * of address is up to the implementation (i.e., it may not
         * necessarily be a global address).
         */
        final NavigableSet<Long> readQueue
                = new ConcurrentSkipListSet<>();

        /** Create a new stream context with the given ID and maximum address
         * to read to.
         * @param id            The ID of the stream to read from
         * @param maxAddress    The maximum address for the context.
         */
        public StreamContext(UUID id, long maxAddress) {
            this.id = id;
            this.maxAddress = maxAddress;
            this.globalPointer = Address.NEVER_READ;
            this.streamPointer = Address.NEVER_READ;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(StreamContext o) {
            return Long.compare(this.maxAddress, o.maxAddress);
        }
    }

}
