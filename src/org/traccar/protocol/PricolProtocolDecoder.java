/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class PricolProtocolDecoder extends BaseProtocolDecoder {

    public PricolProtocolDecoder(PricolProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedByte(); // header

        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, buf.readBytes(7).toString(StandardCharsets.US_ASCII));
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        buf.readUnsignedByte(); // event type
        buf.readUnsignedByte(); // packet version
        buf.readUnsignedByte(); // device status
        buf.readUnsignedByte(); // gsm status
        buf.readUnsignedByte(); // gps status

        position.setTime(new DateBuilder()
                .setDateReverse(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()).getDate());

        position.setValid(true);

        double lat = buf.getUnsignedShort(buf.readerIndex()) / 100;
        lat += (buf.readUnsignedShort() % 100 * 10000 + buf.readUnsignedShort()) / 600000.0;
        position.setLatitude(buf.readUnsignedByte() == 'S' ? -lat : lat);

        double lon = buf.getUnsignedMedium(buf.readerIndex()) / 100;
        lon += (buf.readUnsignedMedium() % 100 * 10000 + buf.readUnsignedShort()) / 600000.0;
        position.setLongitude(buf.readUnsignedByte() == 'W' ? -lon : lon);

        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));

        position.set(Position.KEY_INPUT, buf.readUnsignedShort());
        position.set(Position.KEY_OUTPUT, buf.readUnsignedByte());

        buf.readUnsignedByte(); // analog alerts
        buf.readUnsignedShort(); // custom alert types

        for (int i = 1; i <= 5; i++) {
            position.set(Position.PREFIX_ADC + i, buf.readUnsignedShort());
        }

        position.set(Position.KEY_ODOMETER, buf.readUnsignedMedium());
        position.set(Position.KEY_RPM, buf.readUnsignedShort());

        if (channel != null) {
            channel.write(ChannelBuffers.copiedBuffer("ACK", StandardCharsets.US_ASCII));
        }

        return position;
    }

}
