package com.firefly.codec.http2.decode;

import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.Flags;
import com.firefly.codec.http2.frame.SettingsFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SettingsBodyParser extends BodyParser {
	private static Logger log = LoggerFactory.getLogger("firefly-system");
	private State state = State.PREPARE;
	private int cursor;
	private int length;
	private int settingId;
	private int settingValue;
	private Map<Integer, Integer> settings;

	public SettingsBodyParser(HeaderParser headerParser, Parser.Listener listener) {
		super(headerParser, listener);
	}

	protected void reset() {
		state = State.PREPARE;
		cursor = 0;
		length = 0;
		settingId = 0;
		settingValue = 0;
		settings = null;
	}

	@Override
	protected void emptyBody(ByteBuffer buffer) {
		onSettings(new HashMap<Integer, Integer>());
	}

	@Override
	public boolean parse(ByteBuffer buffer) {
		while (buffer.hasRemaining()) {
			switch (state) {
			case PREPARE: {
				// SPEC: wrong streamId is treated as connection error.
				if (getStreamId() != 0)
					return connectionFailure(buffer, ErrorCode.PROTOCOL_ERROR.code, "invalid_settings_frame");
				length = getBodyLength();
				settings = new HashMap<>();
				state = State.SETTING_ID;
				break;
			}
			case SETTING_ID: {
				if (buffer.remaining() >= 2) {
					settingId = buffer.getShort() & 0xFF_FF;
					state = State.SETTING_VALUE;
					length -= 2;
					if (length <= 0)
						return connectionFailure(buffer, ErrorCode.FRAME_SIZE_ERROR.code, "invalid_settings_frame");
				} else {
					cursor = 2;
					settingId = 0;
					state = State.SETTING_ID_BYTES;
				}
				break;
			}
			case SETTING_ID_BYTES: {
				int currByte = buffer.get() & 0xFF;
				--cursor;
				settingId += currByte << (8 * cursor);
				--length;
				if (length <= 0)
					return connectionFailure(buffer, ErrorCode.FRAME_SIZE_ERROR.code, "invalid_settings_frame");
				if (cursor == 0) {
					state = State.SETTING_VALUE;
				}
				break;
			}
			case SETTING_VALUE: {
				if (buffer.remaining() >= 4) {
					settingValue = buffer.getInt();
					if (log.isDebugEnabled())
						log.debug(String.format("setting %d=%d", settingId, settingValue));
					settings.put(settingId, settingValue);
					state = State.SETTING_ID;
					length -= 4;
					if (length == 0)
						return onSettings(settings);
				} else {
					cursor = 4;
					settingValue = 0;
					state = State.SETTING_VALUE_BYTES;
				}
				break;
			}
			case SETTING_VALUE_BYTES: {
				int currByte = buffer.get() & 0xFF;
				--cursor;
				settingValue += currByte << (8 * cursor);
				--length;
				if (cursor > 0 && length <= 0)
					return connectionFailure(buffer, ErrorCode.FRAME_SIZE_ERROR.code, "invalid_settings_frame");
				if (cursor == 0) {
					if (log.isDebugEnabled())
						log.debug(String.format("setting %d=%d", settingId, settingValue));
					settings.put(settingId, settingValue);
					state = State.SETTING_ID;
					if (length == 0)
						return onSettings(settings);
				}
				break;
			}
			default: {
				throw new IllegalStateException();
			}
			}
		}
		return false;
	}

	protected boolean onSettings(Map<Integer, Integer> settings) {
		SettingsFrame frame = new SettingsFrame(settings, hasFlag(Flags.ACK));
		reset();
		notifySettings(frame);
		return true;
	}

	public static SettingsFrame parseBody(final ByteBuffer buffer) {
		final int bodyLength = buffer.remaining();
		final AtomicReference<SettingsFrame> frameRef = new AtomicReference<>();
		SettingsBodyParser parser = new SettingsBodyParser(null, null) {
			@Override
			protected int getStreamId() {
				return 0;
			}

			@Override
			protected int getBodyLength() {
				return bodyLength;
			}

			@Override
			protected boolean onSettings(Map<Integer, Integer> settings) {
				frameRef.set(new SettingsFrame(settings, false));
				return true;
			}

			@Override
			protected boolean connectionFailure(ByteBuffer buffer, int error, String reason) {
				frameRef.set(null);
				return false;
			}
		};
		if (bodyLength == 0)
			parser.emptyBody(buffer);
		else
			parser.parse(buffer);
		return frameRef.get();
	}

	private enum State {
		PREPARE, SETTING_ID, SETTING_ID_BYTES, SETTING_VALUE, SETTING_VALUE_BYTES
	}
}
