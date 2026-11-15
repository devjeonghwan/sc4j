package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lists the kinds of {@link ChannelSignal} with their wire codes.
 */
public enum ChannelSignalKind {
    /**
     * The participant that opens a channel asks the other one for it ({@link ChannelOffer}).
     */
    OFFER(1),
    /**
     * The other participant accepts the offer ({@link ChannelAnswer}).
     */
    ANSWER(2),
    /**
     * A room's host refuses an offer to join the room ({@link ChannelRefusal}).
     */
    REFUSAL(3);

    private static final @NotNull ChannelSignalKind @NotNull [] VALUES = values();

    /**
     * Finds a kind by its wire code.
     *
     * @param code the wire code
     * @return the kind, or {@code null} if no kind has the code
     */
    public static @Nullable ChannelSignalKind find(int code) {
        for (ChannelSignalKind kind : VALUES) {
            if (kind.code == code) {
                return kind;
            }
        }

        return null;
    }


    private final int code;

    /**
     * Creates a {@link ChannelSignalKind} instance.
     *
     * @param code the wire code
     */
    ChannelSignalKind(int code) {
        this.code = code;
    }

    /**
     * @return the wire code
     */
    public int getCode() {
        return this.code;
    }
}
