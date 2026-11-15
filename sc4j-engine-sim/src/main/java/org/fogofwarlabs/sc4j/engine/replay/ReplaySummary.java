package org.fogofwarlabs.sc4j.engine.replay;

import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.fogofwarlabs.sc4j.engine.game.PlayerSlot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds what a replay list shows of one replay ({@link ReplayFormat#readSummary}): the map name, the length in game frames, the number of
 * players and, when the format reads them, the slots of the players who played (people and computers, not the neutral slot or observers).
 */
public final class ReplaySummary {
    /**
     * Returns the slots of a setup that played: the people and computer players, in slot order.
     *
     * @param slots the slots of a game's setup
     * @return the playing slots
     */
    public static @NotNull List<@NotNull PlayerSlot> findPlayingSlots(@NotNull List<@NotNull PlayerSlot> slots) {
        List<PlayerSlot> playing = new ArrayList<>(slots.size());

        for (PlayerSlot slot : slots) {
            if (slot.getControllerKind() == ControllerKind.HUMAN || slot.getControllerKind() == ControllerKind.COMPUTER) {
                playing.add(slot);
            }
        }

        return playing;
    }


    private final @NotNull String mapName;
    private final long frameCount;
    private final int playerCount;
    private final @NotNull List<@NotNull PlayerSlot> players;

    /**
     * Creates a {@link ReplaySummary} instance of a format that reads only the number of players.
     *
     * @param mapName     the name of the replay's map
     * @param frameCount  the game frames the replay runs
     * @param playerCount the players of the recorded game
     */
    public ReplaySummary(@NotNull String mapName, long frameCount, int playerCount) {
        this.mapName     = mapName;
        this.frameCount  = frameCount;
        this.playerCount = playerCount;
        this.players     = List.of();
    }

    /**
     * Creates a {@link ReplaySummary} instance with the slots of the players who played; the number of players is their number.
     *
     * @param mapName    the name of the replay's map
     * @param frameCount the game frames the replay runs
     * @param players    the slots of the players who played, copied
     */
    public ReplaySummary(@NotNull String mapName, long frameCount, @NotNull List<@NotNull PlayerSlot> players) {
        this.mapName     = mapName;
        this.frameCount  = frameCount;
        this.playerCount = players.size();
        this.players     = List.copyOf(players);
    }

    /**
     * @return the name of the replay's map
     */
    public @NotNull String getMapName() {
        return this.mapName;
    }

    /**
     * @return the game frames the replay runs
     */
    public long getFrameCount() {
        return this.frameCount;
    }

    /**
     * @return the players of the recorded game
     */
    public int getPlayerCount() {
        return this.playerCount;
    }

    /**
     * @return the slots of the players who played, empty when the format reads only their number
     */
    public @NotNull List<@NotNull PlayerSlot> getPlayers() {
        return this.players;
    }
}
