package org.fogofwarlabs.sc4j.beaconlink.protocol.topic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lists the kinds of Beacon.Link topics with their wire codes. The kind is part of a {@link TopicKey}'s hash, so equal names of two kinds
 * are two topics.
 */
public enum TopicKind {
    /**
     * The topic of one mod, named by its mod id: the rooms and the presence of the players of every version of the mod.
     */
    MOD(1),
    /**
     * The topic of one chat channel, named by its channel name in lower case: the presence of its members and their chat lines. Channels are
     * shared by every mod.
     */
    CHANNEL(2),
    /**
     * The topic of one player, named by the player key in hexadecimal: the player's presence, for the player's friends.
     */
    PLAYER(3);

    private static final @NotNull TopicKind @NotNull [] VALUES = values();

    /**
     * Finds a kind by its wire code.
     *
     * @param code the wire code
     * @return the kind, or {@code null} if no kind has the code
     */
    public static @Nullable TopicKind find(int code) {
        for (TopicKind kind : VALUES) {
            if (kind.code == code) {
                return kind;
            }
        }

        return null;
    }


    private final int code;

    /**
     * Creates a {@link TopicKind} instance.
     *
     * @param code the wire code
     */
    TopicKind(int code) {
        this.code = code;
    }

    /**
     * @return the wire code
     */
    public int getCode() {
        return this.code;
    }
}
