package org.fogofwarlabs.sc4j.engine.save;

import org.jetbrains.annotations.NotNull;

/**
 * Holds what a load list shows of one saved game ({@link SaveFormat#readSummary}): the map name, the game frame it was saved at and the number
 * of players.
 */
public final class SaveSummary {
    private final @NotNull String mapName;
    private final long frame;
    private final int playerCount;

    /**
     * Creates a {@link SaveSummary} instance.
     *
     * @param mapName     the name of the saved game's map
     * @param frame       the game frame the game was saved at
     * @param playerCount the number of players of the saved game
     */
    public SaveSummary(@NotNull String mapName, long frame, int playerCount) {
        this.mapName     = mapName;
        this.frame       = frame;
        this.playerCount = playerCount;
    }

    /**
     * @return the name of the saved game's map
     */
    public @NotNull String getMapName() {
        return this.mapName;
    }

    /**
     * @return the game frame the game was saved at
     */
    public long getFrame() {
        return this.frame;
    }

    /**
     * @return the number of players of the saved game
     */
    public int getPlayerCount() {
        return this.playerCount;
    }
}
