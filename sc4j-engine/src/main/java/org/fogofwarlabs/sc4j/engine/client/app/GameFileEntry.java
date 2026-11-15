package org.fogofwarlabs.sc4j.engine.client.app;

import org.fogofwarlabs.sc4j.engine.game.PlayerSlot;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Describes one stored game file a mod's registered format reads: a replay or a saved game, with the summary its format read from the file's
 * header and the time the file was last written.
 */
public final class GameFileEntry {
    private final @NotNull Path path;
    private final @NotNull String formatName;
    private final @NotNull Instant modifiedTime;

    private final @NotNull String mapName;
    private final long frameCount;
    private final int playerCount;
    private final @NotNull List<@NotNull PlayerSlot> players;

    /**
     * Creates a {@link GameFileEntry} instance.
     *
     * @param path         the file
     * @param formatName   the name of the format that reads it
     * @param modifiedTime the time the file was last written
     * @param mapName      the name of the game's map
     * @param frameCount   the game loops of a replay, or the game loop a saved game was saved at
     * @param playerCount  the players of the game
     * @param players      the slots of the players who played, copied; empty when the format reads only their number
     */
    public GameFileEntry(@NotNull Path path, @NotNull String formatName, @NotNull Instant modifiedTime,
                         @NotNull String mapName, long frameCount, int playerCount, @NotNull List<@NotNull PlayerSlot> players) {
        this.path         = path;
        this.formatName   = formatName;
        this.modifiedTime = modifiedTime;

        this.mapName     = mapName;
        this.frameCount  = frameCount;
        this.playerCount = playerCount;
        this.players     = List.copyOf(players);
    }

    /**
     * @return the file
     */
    public @NotNull Path getPath() {
        return this.path;
    }

    /**
     * @return the name of the format that reads the file
     */
    public @NotNull String getFormatName() {
        return this.formatName;
    }

    /**
     * @return the time the file was last written
     */
    public @NotNull Instant getModifiedTime() {
        return this.modifiedTime;
    }

    /**
     * @return the name of the game's map
     */
    public @NotNull String getMapName() {
        return this.mapName;
    }

    /**
     * @return the game loops of a replay, or the game loop a saved game was saved at
     */
    public long getFrameCount() {
        return this.frameCount;
    }

    /**
     * @return the players of the game
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
