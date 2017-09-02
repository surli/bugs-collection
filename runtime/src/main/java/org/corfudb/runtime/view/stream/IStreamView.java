package org.corfudb.runtime.view.stream;

import org.corfudb.protocols.wireprotocol.LogData;
import org.corfudb.protocols.wireprotocol.TokenResponse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** This interface represents a view on a stream. A stream is an ordered
 * set of log entries which can only be appended to and read in sequential
 * order.
 *
 * Created by mwei on 1/5/17.
 */
public interface IStreamView extends Iterator<LogData> {

    /** Return the ID of the stream this view is for.
     * @return  The ID of the stream.
     */
    UUID getID();

    /** Reset the state of this stream view, causing the next read to
     * start from the beginning of this stream.
     */
    void reset();

    /** Append an object to the stream, returning the global address
     * it was written at.
     * <p>
     * Optionally, provide a method to be called when an address is acquired,
     * and also a method to be called when an address is released (due to
     * an unsuccessful append).
     * </p>
     * @param   object              The object to append.
     * @param   acquisitionCallback A function to call when an address is
     *                              acquired.
     *                              It should return true to continue with the
     *                              append.
     * @param   deacquisitionCallback A function to call when an address is
     *                                released. It should return true to retry
     *                                writing.
     * @return  The (global) address the object was written at.
     */
    long append(Object object,
     Function<TokenResponse, Boolean> acquisitionCallback,
     Function<TokenResponse, Boolean> deacquisitionCallback);

    /** Append an object to the stream, returning the global address it was
     * written at.
     * @param   object
     * @return  The (global) address the object was written at.
     */
    default long append(Object object) {
        return append(object, null, null);
    }

    /** Retrieve the next entry from this stream, up to the tail of the stream
     * If there are no entries present, this function will return NULL. If there
     * are holes present in the log, they will be filled.
     * @return  The next entry in the stream, or NULL, if no entries are
     *          available.
     */
    default LogData next() {
        return nextUpTo(Long.MAX_VALUE);
    }

    /** Retrieve the next entry from this stream, up to the address given or the
     *  tail of the stream. If there are no entries present, this function
     *  will return NULL. If there  are holes present in the log, they will
     *  be filled.
     * @param maxGlobal The maximum global address to read up to.
     * @return          The next entry in the stream, or NULL, if no entries
     *                  are available.
     */
    LogData nextUpTo(long maxGlobal);

    /** Retrieve all of the entries from this stream, up to the tail of this
     *  stream. If there are no entries present, this function
     *  will return an empty list. If there  are holes present in the log,
     *  they will be filled.
     *
     *  Note: the default implementation is thread-safe only if the
     *  implementation of read is synchronized.
     *
     * @return          The next entries in the stream, or an empty list,
     *                  if no entries are available.
     */
    default List<LogData> remaining() { return remainingUpTo(Long.MAX_VALUE); }

    /** Retrieve all of the entries from this stream, up to the address given or
     *  the tail of the stream. If there are no entries present, this function
     *  will return an empty list. If there  are holes present in the log,
     *  they will be filled.
     *
     *  Note: the default implementation is thread-safe only if the
     *  implementation of read is synchronized.
     *
     * @param maxGlobal The maximum global address to read up to.
     * @return          The next entries in the stream, or an empty list,
     *                  if no entries are available.
     */
    default List<LogData> remainingUpTo(long maxGlobal) {
        synchronized (this) {
            final List<LogData> dataList = new ArrayList<>();
            LogData thisData;
            while ((thisData = nextUpTo(maxGlobal)) != null) {
                dataList.add(thisData);
            }
            return dataList;
        }
    }

    /** Returns whether or not there are potentially more entries in this
     * stream - this function may return true even if there are no entries
     * remaining, as addresses may have been acquired by other clients
     * but not written yet, or the addresses were hole-filled, or just failed.
     * @return      True, if there are potentially more entries in the stream.
     */
    boolean hasNext();
}
