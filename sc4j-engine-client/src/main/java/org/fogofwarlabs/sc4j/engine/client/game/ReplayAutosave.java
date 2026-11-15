package org.fogofwarlabs.sc4j.engine.client.game;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Saves the replays of a client's live games without the user asking: the automatic file once at the end of each game ({@link #saveAtEnd}), the
 * same file earlier from the {@link SimulationWatchdog}'s thread while a frame stalls ({@link #saveStalled}), so a game whose window is then closed
 * keeps its replay, and a copy under a name the user chose ({@link #saveCopy}). The mod names the file and writes the replay in its own format
 * ({@link ReplayWriter}); this class owns whether the running game's replay was written and which file the last game wrote.
 */
public final class ReplayAutosave {
    private static final @NotNull Logger LOGGER = Logger.of(ReplayAutosave.class);

    /**
     * Writes the running game's replay to a file in the mod's replay format.
     */
    @FunctionalInterface
    public interface ReplayWriter {
        /**
         * Writes the replay of the commands run so far.
         *
         * @param file the file
         * @throws IOException if the file cannot be written
         */
        void write(@NotNull Path file) throws IOException;
    }

    /**
     * Copies a written replay under another name, replacing a file there (the user confirmed the name); the folders are created.
     *
     * @param written the written replay, or {@code null} when none was written
     * @param target  the chosen file
     * @return {@code true} if the file was written, {@code false} otherwise
     */
    public static boolean saveCopy(@Nullable Path written, @NotNull Path target) {
        if (written == null || !Files.isRegularFile(written)) {
            return false;
        }

        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }

            if (!written.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
                Files.copy(written, target, StandardCopyOption.REPLACE_EXISTING);
            }

            LOGGER.info("LIVE REPLAY SAVED: " + target);

            return true;
        } catch (IOException exception) {
            LOGGER.warning("LIVE REPLAY SAVE FAILED: " + exception.getMessage());

            return false;
        }
    }

    /**
     * Whether the running game's replay was written at its end; the watchdog's thread reads it.
     */
    private volatile boolean saved;
    /**
     * The replay file written at the end of the last game.
     */
    private @Nullable Path lastWritten;

    /**
     * Creates a {@link ReplayAutosave} instance before the first game.
     */
    public ReplayAutosave() {
        this.saved       = false;
        this.lastWritten = null;
    }

    /**
     * @return the replay file written at the end of the last game, or {@code null} if none was written
     */
    public @Nullable Path getLastWritten() {
        return this.lastWritten;
    }

    /**
     * Marks a new game as not yet written.
     */
    public void startGame() {
        this.saved = false;
    }

    /**
     * Writes the automatic replay at the end of the running game, once per game: later calls for the same game do nothing. The file's folders are
     * created; a failure is logged.
     *
     * @param file   the automatic file, or {@code null} if this game writes none
     * @param writer the writer of the running game's replay
     */
    public void saveAtEnd(@Nullable Path file, @NotNull ReplayWriter writer) {
        if (this.saved) {
            return;
        }

        this.saved = true;

        if (file == null) {
            return;
        }

        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }

            writer.write(file);
            this.lastWritten = file;
            LOGGER.info("LIVE REPLAY SAVED: " + file);
        } catch (IOException exception) {
            LOGGER.warning("LIVE REPLAY SAVE FAILED: " + exception.getMessage());
        }
    }

    /**
     * Writes the running game's automatic replay from the watchdog's thread while a frame stalls: the frame may never return, and a window closed
     * then would lose the game. The replay holds the commands run so far; the end of the game writes the file again if the frame returns. Nothing is
     * written after the end of the game; a failure is logged.
     *
     * @param file   the automatic file, or {@code null} if this game writes none
     * @param writer the writer of the running game's replay
     */
    public void saveStalled(@Nullable Path file, @NotNull ReplayWriter writer) {
        if (this.saved || file == null) {
            return;
        }

        try {
            writer.write(file);
            LOGGER.info("LIVE REPLAY SAVED (stalled frame): " + file);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warning("LIVE REPLAY SAVE FAILED (stalled frame): " + exception);
        }
    }
}
