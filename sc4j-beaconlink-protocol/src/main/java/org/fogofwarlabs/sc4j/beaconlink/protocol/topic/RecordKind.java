package org.fogofwarlabs.sc4j.beaconlink.protocol.topic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lists the kinds of the signed records of a topic ({@link TopicRecord}) with their wire codes: the code precedes a record wherever records
 * of several kinds travel together.
 */
public enum RecordKind {
    /**
     * A room advertisement of a mod topic.
     */
    ROOM(1),
    /**
     * A player's presence, in every topic the player takes part in.
     */
    PRESENCE(2),
    /**
     * A chat line of a channel topic.
     */
    CHAT_LINE(3),
    /**
     * A player's sign-out of its other connections, in the player's own topic.
     */
    SIGN_OUT(4);

    private static final @NotNull RecordKind @NotNull [] VALUES = values();

    /**
     * Finds a kind by its wire code.
     *
     * @param code the wire code
     * @return the kind, or {@code null} if no kind has the code
     */
    public static @Nullable RecordKind find(int code) {
        for (RecordKind kind : VALUES) {
            if (kind.code == code) {
                return kind;
            }
        }

        return null;
    }


    private final int code;

    /**
     * Creates a {@link RecordKind} instance.
     *
     * @param code the wire code
     */
    RecordKind(int code) {
        this.code = code;
    }

    /**
     * @return the wire code
     */
    public int getCode() {
        return this.code;
    }
}
