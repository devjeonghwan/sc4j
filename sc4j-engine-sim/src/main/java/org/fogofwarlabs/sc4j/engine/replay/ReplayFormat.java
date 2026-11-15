package org.fogofwarlabs.sc4j.engine.replay;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Describes the format of a mod's replay files, the one a client lists, opens and saves: the engine replay format
 * ({@link EngineReplayFormat}) for a mod without its own, or the mod's own format. The mod chooses its format; what it records and how it
 * plays a replay back belongs to the format's implementation.
 */
public interface ReplayFormat {
    /**
     * Finds the format of a file among a mod's formats: the first whose {@link #isReplay} accepts the contents.
     *
     * @param formats  the formats, in the mod's order
     * @param contents the file contents
     * @return the first format the contents start like, or {@code null} if none
     */
    static @Nullable ReplayFormat find(@NotNull List<? extends @NotNull ReplayFormat> formats, byte @NotNull [] contents) {
        for (ReplayFormat format : formats) {
            if (format.isReplay(contents)) {
                return format;
            }
        }

        return null;
    }

    /**
     * @return the name of the format for people, such as {@code SC4J engine replay}
     */
    @NotNull String getName();

    /**
     * @return the file name extension of the format's replay files, without the dot, such as {@code sc4r}
     */
    @NotNull String getFileExtension();

    /**
     * Tests whether file contents start like a replay of this format; the contents may still be damaged.
     *
     * @param contents the file contents
     * @return {@code true} if the contents start like a replay of this format, {@code false} otherwise
     */
    boolean isReplay(byte @NotNull [] contents);

    /**
     * Reads what a replay list shows of a replay: its map, its length and its players.
     *
     * @param contents the file contents
     * @return the created {@link ReplaySummary} instance
     * @throws IOException if the contents are no readable replay of this format for the running mod
     */
    @NotNull ReplaySummary readSummary(byte @NotNull [] contents) throws IOException;
}
