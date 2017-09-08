package org.corfudb.runtime.view.stream;

import lombok.extern.slf4j.Slf4j;
import org.corfudb.protocols.wireprotocol.DataType;
import org.corfudb.protocols.wireprotocol.LogData;
import org.corfudb.protocols.wireprotocol.TokenResponse;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.runtime.exceptions.OverwriteException;
import org.corfudb.runtime.exceptions.ReplexOverwriteException;
import org.corfudb.runtime.view.Address;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

/** A view of a stream implemented using Replex.
 *
 * In this implementation, addresses in the read queue are stream addresses.
 *
 * TODO: This implementation does not implement bulk reads anymore. This will
 * be addressed once the address space implementation is refactored into an
 * address space view and a replex (branch?) view.
 *
 * All method calls of this class are thread-safe.
 *
 * Created by mwei on 1/5/17.
 */
@Slf4j
public class ReplexStreamView extends AbstractQueuedStreamView {

    /**
     * The number of retries before attempting a hole fill.
     * TODO: this constant should come from the runtime.
     */
    final int NUM_RETRIES = 3;

    /** Create a new replex stream view.
     *
     * @param runtime   The runtime to use for accessing the log.
     * @param streamID  The ID of the stream to view.
     */
    public ReplexStreamView(final CorfuRuntime runtime,
                                 final UUID streamID) {
        super(runtime, streamID);
    }

    /** {@inheritDoc}
     *
     * In Replex, stream addresses are returned by the sequencer. When an
     * overwrite error occurs, we inform the sequencer whether or not we want
     * a new stream address (token) ONLY or both global and stream addresses.
     */
    @Override
    public long append(Object object,
                       Function<TokenResponse, Boolean> acquisitionCallback,
                       Function<TokenResponse, Boolean> deacquisitionCallback) {
        // First, we get a token from the sequencer.
        TokenResponse tokenResponse = runtime.getSequencerView()
                .nextToken(Collections.singleton(streamID), 1);

        // We loop forever until we are interrupted, since we may have to
        // acquire an address several times until we are successful.
        while (true) {
            // Next, we call the acquisitionCallback, if present, informing
            // the client of the token that we acquired.
            if (acquisitionCallback != null) {
                if (!acquisitionCallback.apply(tokenResponse)) {
                    // The client did not like our token, so we end here.
                    // We'll leave the hole to be filled by the client or
                    // someone else.
                    log.debug("Acquisition rejected token={}", tokenResponse);
                    return Address.ABORTED;
                }
            }

            // Now, we do the actual write. We could get an overwrite
            // exception here - any other exception we should pass up
            // to the client.
            try {
                runtime.getAddressSpaceView()
                        .write(tokenResponse.getToken(),
                                Collections.singleton(streamID),
                                object,
                                tokenResponse.getBackpointerMap(),
                                tokenResponse.getStreamAddresses());
                // The write completed successfully, so we return this
                // address to the client.
                return tokenResponse.getToken();
            }
            catch (OverwriteException oe) {
                log.trace("Overwrite occurred at {}", tokenResponse);
                // We got overwritten, so we call the deacquisition callback
                // to inform the client we didn't get the address.
                if (deacquisitionCallback != null) {
                    if (!deacquisitionCallback.apply(tokenResponse)) {
                        log.debug("Deacquisition requested abort");
                        return Address.ABORTED;
                    }
                }
                // Request a new token, informing the sequencer
                // of the overwrite.
                tokenResponse = runtime.getSequencerView()
                        .nextToken(Collections.singleton(streamID),
                              1,
                                // If this is a normal overwrite
                                !(oe instanceof ReplexOverwriteException),
                                // If this is a Replex overwrite
                                oe instanceof ReplexOverwriteException);
            }
        }
    }

    /** {@inheritDoc}
     *
     * In Replex, the queue contains stream addresses. We can't determine
     * the global address until we perform the read.
     *
     * In addition, the implementation of fillReadQueue doesn't fill holes
     * for us, so we need to check and fill them as appropiate.
     * */
    @Override
    protected LogData readAndUpdatePointers(final StreamContext context,
                      final long address, long maxGlobal) {

        LogData ld = runtime.getAddressSpaceView()
                .read(streamID, address, 1L).get(address);

        // Do we have data? If not, retry the given number of times before
        // attempting a hole fill.
        for (int i = 0; i < NUM_RETRIES; i++) {
            ld = runtime.getAddressSpaceView()
                    .read(streamID, address, 1L).get(address);
            if (ld.getType() != DataType.EMPTY) {
                break;
            }
        }

        // If after we retry the data is still empty, let's hole fill.
        if (ld.getType() == DataType.EMPTY) {
            try {
                runtime.getAddressSpaceView()
                        .fillStreamHole(context.id, address);
            } catch (OverwriteException oe) {
                // If we're overwritten while hole filling, that's okay
                // since we're going to re-read anyway
            }
            ld = runtime.getAddressSpaceView()
                    .read(streamID, address, 1L).get(address);
        }

        // We return holes to be filtered out, because
        // returning NULL will cause the implementation of
        // next() to stop.
        if (ld.getType() == DataType.HOLE)
        {
            return ld;
        }

        // Check if we are within maxGlobal.
        if (ld.getGlobalAddress() <= maxGlobal) {
            context.globalPointer = ld.getGlobalAddress();
            context.streamPointer =
                    ld.getStreamAddress(getCurrentContext().id);
            return ld;
        }

        // Otherwise return null.
        return null;
    }

    /** {@inheritDoc}
     *
     * In Replex, filling the read queue is complicated by the fact that
     * our addresses are stream addresses and not global addresses. We use
     * the sequencer to determine what the maximum stream address is and
     * fill to that point.
     *
     * In the future, we might be able to just go to the log unit.
     */
    @Override
    protected void fillReadQueue(final long maxGlobal,
                                 final StreamContext context) {
        TokenResponse tr = runtime.getSequencerView()
                .nextToken(Collections.singleton(getCurrentContext()
                        .id), 0);
        // For each address, starting at the one after the current
        // pointer, add each address up to and including the last
        // issued stream address by the sequencer.
        for (long i = getCurrentContext().streamPointer + 1;
                i <= tr.getStreamAddresses().get(context.id); i++) {
            context.readQueue.add(i);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Just like the backpointer based implementation, we indicate we may have
     * entries available if the read queue contains entries to read -or-
     * if the next token is greater than our log pointer.
     */
    @Override
    public boolean hasNext() {
        return  !getCurrentContext().readQueue.isEmpty() ||
                runtime.getSequencerView()
                        .nextToken(Collections.singleton(getCurrentContext()
                                .id), 0).getToken()
                        > getCurrentContext().globalPointer;
    }
}
