package org.corfudb.protocols.wireprotocol;

import io.netty.buffer.ByteBuf;
import lombok.*;

import java.util.*;

/**
 * Created by mwei on 8/9/16.
 */
@Builder
@AllArgsConstructor
public class WriteRequest implements ICorfuPayload<WriteRequest>, IMetadata {

    @Getter
    final WriteMode writeMode;
    @Getter
    final Map<UUID, Long> streamAddresses;
    @Getter
    final ILogData data;

    @SuppressWarnings("unchecked")
    public WriteRequest(ByteBuf buf) {
        writeMode = ICorfuPayload.fromBuffer(buf, WriteMode.class);
        streamAddresses = null;
        data = ICorfuPayload.fromBuffer(buf, LogData.class);
    }

    public WriteRequest(WriteMode writeMode, Map<UUID, Long> streamAddresses, ByteBuf buf) {
        this(writeMode, DataType.DATA, streamAddresses, buf);
    }

    public WriteRequest(WriteMode writeMode, DataType dataType, Map<UUID, Long> streamAddresses, ByteBuf buf) {
        this.writeMode = writeMode;
        this.streamAddresses = streamAddresses;
        this.data = new LogData(dataType, buf);
    }

    public WriteRequest(ILogData data) {
        writeMode = WriteMode.NORMAL;
        this.streamAddresses = null;
        this.data = data;
    }

    @Override
    public void doSerialize(ByteBuf buf) {
        ICorfuPayload.serialize(buf, writeMode);
        ICorfuPayload.serialize(buf, data);
    }

    @Override
    public EnumMap<LogUnitMetadataType, Object> getMetadataMap() {
        return data.getMetadataMap();
    }
}
