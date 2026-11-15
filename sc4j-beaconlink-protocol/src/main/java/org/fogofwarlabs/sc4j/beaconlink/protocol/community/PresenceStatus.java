package org.fogofwarlabs.sc4j.beaconlink.protocol.community;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lists what a player's presence says the player is doing, with the wire codes.
 */
public enum PresenceStatus {
    /**
     * The player is online and can chat.
     */
    AVAILABLE(1),
    /**
     * The player is online but away.
     */
    AWAY(2),
    /**
     * The player is in a room, waiting for its game.
     */
    IN_ROOM(3),
    /**
     * The player is in a game.
     */
    IN_GAME(4);

    private static final @NotNull PresenceStatus @NotNull [] VALUES = values();

    /**
     * Finds a status by its wire code.
     *
     * @param code the wire code
     * @return the status, or {@code null} if no status has the code
     */
    public static @Nullable PresenceStatus find(int code) {
        for (PresenceStatus status : VALUES) {
            if (status.code == code) {
                return status;
            }
        }

        return null;
    }


    private final int code;

    /**
     * Creates a {@link PresenceStatus} instance.
     *
     * @param code the wire code
     */
    PresenceStatus(int code) {
        this.code = code;
    }

    /**
     * @return the wire code
     */
    public int getCode() {
        return this.code;
    }
}
