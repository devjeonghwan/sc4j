package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lists the frame types of a channel datagram with their wire codes. The frame is the type (one byte) and its body.
 */
public enum ChannelFrameType {
    /**
     * Tries a path to the other participant. Body: the sender's clock time (eight bytes), which the acknowledgement repeats.
     */
    PROBE(1),
    /**
     * Confirms a probe on the path it came from. Body: the probe's clock time (eight bytes).
     */
    PROBE_ACK(2),
    /**
     * Carries a datagram of the channel's owner. Body: the payload.
     */
    DATA(3),
    /**
     * Keeps the path alive and measures the delay. Body: the sender's clock time (eight bytes), which the answer repeats.
     */
    PING(4),
    /**
     * Answers a ping. Body: the ping's clock time (eight bytes).
     */
    PONG(5),
    /**
     * Tells the other participant that the channel is closed. Body: empty.
     */
    CLOSE(6),
    /**
     * Asks a participant whether it can relay to another one. Body: the other participant's key.
     */
    RELAY_QUERY(7),
    /**
     * Answers a relay query. Body: the other participant's key, whether the sender has a direct open channel to it (one byte) and that
     * channel's measured delay in milliseconds (unsigned 32-bit).
     */
    RELAY_STATUS(8),
    /**
     * Asks the relay to pass a sealed datagram of another channel on. Body: the receiver's key and the sealed datagram, which the relay
     * cannot open.
     */
    RELAY_FORWARD(9),
    /**
     * Passes on a sealed datagram from a participant the relay is connected to. Body: the sender's key and the sealed datagram.
     */
    RELAY_DELIVER(10);

    private static final @NotNull ChannelFrameType @NotNull [] VALUES = values();

    /**
     * Finds a frame type by its wire code.
     *
     * @param code the wire code
     * @return the type, or {@code null} if no type has the code
     */
    public static @Nullable ChannelFrameType find(int code) {
        for (ChannelFrameType type : VALUES) {
            if (type.code == code) {
                return type;
            }
        }

        return null;
    }


    private final int code;

    /**
     * Creates a {@link ChannelFrameType} instance.
     *
     * @param code the wire code
     */
    ChannelFrameType(int code) {
        this.code = code;
    }

    /**
     * @return the wire code
     */
    public int getCode() {
        return this.code;
    }
}
