package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Holds one frame of a channel datagram: its type and its body.
 */
public final class ChannelFrame {
    /**
     * Reads a frame. Never throws for malformed input.
     *
     * @param data   the bytes
     * @param offset the first byte of the frame
     * @param length the frame length
     * @return the frame, or {@code null} if it is empty or has an unknown type
     */
    public static @Nullable ChannelFrame read(byte @NotNull [] data, int offset, int length) {
        if (length < 1) {
            return null;
        }

        ChannelFrameType type = ChannelFrameType.find(Byte.toUnsignedInt(data[offset]));

        return type == null ? null : new ChannelFrame(type, Arrays.copyOfRange(data, offset + 1, offset + length));
    }


    private final @NotNull ChannelFrameType type;
    private final byte @NotNull [] body;

    /**
     * Creates a {@link ChannelFrame} instance.
     *
     * @param type the frame type
     * @param body the body; the frame keeps the array
     */
    public ChannelFrame(@NotNull ChannelFrameType type, byte @NotNull [] body) {
        this.type = type;
        this.body = body;
    }

    /**
     * @return the frame type
     */
    public @NotNull ChannelFrameType getType() {
        return this.type;
    }

    /**
     * @return the body length
     */
    public int getBodyLength() {
        return this.body.length;
    }

    /**
     * Creates a reader of the body.
     *
     * @return a reader of the body
     */
    public @NotNull WireReader readBody() {
        return new WireReader(this.body, 0, this.body.length);
    }


    /**
     * Writes the frame: the type and the body.
     *
     * @return the frame bytes
     */
    public byte @NotNull [] write() {
        byte[] frame = new byte[1 + this.body.length];

        frame[0] = (byte) this.type.getCode();
        System.arraycopy(this.body, 0, frame, 1, this.body.length);

        return frame;
    }
}
