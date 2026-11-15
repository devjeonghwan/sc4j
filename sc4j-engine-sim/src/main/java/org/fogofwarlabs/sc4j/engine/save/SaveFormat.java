package org.fogofwarlabs.sc4j.engine.save;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Describes the format of a mod's saved games, the one a client lists, loads and saves: the engine save format ({@link EngineSaveFormat})
 * for a mod without its own, or the mod's own format. The mod chooses its format; what a saved game holds and how a game is resumed from it
 * belongs to the format's implementation.
 */
public interface SaveFormat {
    /**
     * Finds the format of a file among a mod's formats: the first whose {@link #isSave} accepts the contents.
     *
     * @param formats  the formats, in the mod's order
     * @param contents the file contents
     * @return the first format the contents start like, or {@code null} if none
     */
    static @Nullable SaveFormat find(@NotNull List<? extends @NotNull SaveFormat> formats, byte @NotNull [] contents) {
        for (SaveFormat format : formats) {
            if (format.isSave(contents)) {
                return format;
            }
        }

        return null;
    }

    /**
     * @return the name of the format for people, such as {@code SC4J engine save}
     */
    @NotNull String getName();

    /**
     * @return the file name extension of the format's saved games, without the dot, such as {@code sc4s}
     */
    @NotNull String getFileExtension();

    /**
     * Tests whether file contents start like a saved game of this format; the contents may still be damaged.
     *
     * @param contents the file contents
     * @return {@code true} if the contents start like a saved game of this format, {@code false} otherwise
     */
    boolean isSave(byte @NotNull [] contents);

    /**
     * Reads what a load list shows of a saved game: its map, the frame it was saved at and its players.
     *
     * @param contents the file contents
     * @return the created {@link SaveSummary} instance
     * @throws IOException if the contents are no readable saved game of this format for the running mod
     */
    @NotNull SaveSummary readSummary(byte @NotNull [] contents) throws IOException;
}
