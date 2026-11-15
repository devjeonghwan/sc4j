package org.fogofwarlabs.sc4j.engine.replay;

import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.jetbrains.annotations.NotNull;

/**
 * Holds one command of an {@link EngineReplay}: the game loop it ran at, the slot index of its player, its type and its encoded payload.
 */
final class RecordedCommand {
    private final long loop;
    private final int playerIndex;
    private final @NotNull CommandType<?> type;
    private final byte @NotNull [] payload;

    /**
     * Creates a {@link RecordedCommand} instance.
     *
     * @param loop        the game loop, counted from {@code 0}
     * @param playerIndex the slot index of the issuing player, the replay's key of the player
     * @param type        the command type
     * @param payload     the payload as the type's codec wrote it, not copied
     */
    RecordedCommand(long loop, int playerIndex, @NotNull CommandType<?> type, byte @NotNull [] payload) {
        this.loop        = loop;
        this.playerIndex = playerIndex;
        this.type        = type;
        this.payload     = payload;
    }

    /**
     * @return the game loop the command ran at, counted from {@code 0}
     */
    long getLoop() {
        return this.loop;
    }

    /**
     * @return the slot index of the issuing player
     */
    int getPlayerIndex() {
        return this.playerIndex;
    }

    /**
     * @return the command type
     */
    @NotNull CommandType<?> getType() {
        return this.type;
    }

    /**
     * @return the payload as the type's codec wrote it, not a copy
     */
    byte @NotNull [] getPayload() {
        return this.payload;
    }
}
