package org.fogofwarlabs.sc4j.engine.client.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the replay autosave: one automatic file per game at its end, the same file from a stalled frame before the end only, no file for a game
 * that writes none, and copies under a chosen name.
 */
final class ReplayAutosaveTest {
    private static final @NotNull String CONTENTS = "replay";
    private static final @NotNull String AUTOMATIC_FILE = "replays/last-game.sc4r";

    /**
     * A writer that records the files it wrote and writes a fixed text.
     */
    private static final class RecordingWriter implements ReplayAutosave.ReplayWriter {
        private final @NotNull List<Path> written;

        private RecordingWriter() {
            this.written = new ArrayList<>();
        }

        @Override
        public void write(@NotNull Path file) throws IOException {
            Files.writeString(file, CONTENTS, StandardCharsets.UTF_8);
            this.written.add(file);
        }
    }

    @Test
    void save_at_end_should_write_once_per_game(@TempDir @NotNull Path directory) {
        Path file = directory.resolve(AUTOMATIC_FILE);
        RecordingWriter writer = new RecordingWriter();
        ReplayAutosave autosave = new ReplayAutosave();

        autosave.startGame();
        autosave.saveAtEnd(file, writer);
        autosave.saveAtEnd(file, writer);

        Assertions.assertEquals(List.of(file), writer.written, "the folders are created and the second call writes nothing");
        Assertions.assertEquals(file, autosave.getLastWritten());

        autosave.startGame();
        autosave.saveAtEnd(file, writer);

        Assertions.assertEquals(List.of(file, file), writer.written, "the next game writes again");
    }

    @Test
    void save_stalled_should_write_only_before_the_end(@TempDir @NotNull Path directory) {
        Path file = directory.resolve("last-game.sc4r");
        RecordingWriter writer = new RecordingWriter();
        ReplayAutosave autosave = new ReplayAutosave();

        autosave.startGame();
        autosave.saveStalled(file, writer);
        autosave.saveStalled(file, writer);

        Assertions.assertEquals(List.of(file, file), writer.written, "every stall report writes the replay so far");
        Assertions.assertNull(autosave.getLastWritten(), "a stall save is not the game's written replay");

        autosave.saveAtEnd(file, writer);
        autosave.saveStalled(file, writer);

        Assertions.assertEquals(3, writer.written.size(), "no stall save after the end");
    }

    @Test
    void games_without_an_automatic_file_write_nothing() {
        RecordingWriter writer = new RecordingWriter();
        ReplayAutosave autosave = new ReplayAutosave();

        autosave.startGame();
        autosave.saveStalled(null, writer);
        autosave.saveAtEnd(null, writer);

        Assertions.assertTrue(writer.written.isEmpty());
        Assertions.assertNull(autosave.getLastWritten());
    }

    @Test
    void failed_writes_are_logged_not_thrown(@TempDir @NotNull Path directory) {
        Path file = directory.resolve("last-game.sc4r");
        ReplayAutosave autosave = new ReplayAutosave();

        autosave.startGame();
        autosave.saveStalled(file, target -> {
            throw Exceptions.newIOException("Disk full.");
        });
        autosave.saveAtEnd(file, target -> {
            throw Exceptions.newIOException("Disk full.");
        });

        Assertions.assertNull(autosave.getLastWritten());
    }

    @Test
    void save_copy_should_copy_a_written_replay_under_the_chosen_name(@TempDir @NotNull Path directory) throws IOException {
        Path written = directory.resolve("last-game.sc4r");
        Path target = directory.resolve("named").resolve("Game.sc4r");

        Assertions.assertFalse(ReplayAutosave.saveCopy(null, target));
        Assertions.assertFalse(ReplayAutosave.saveCopy(written, target), "no replay was written");

        Files.writeString(written, CONTENTS, StandardCharsets.UTF_8);

        Assertions.assertTrue(ReplayAutosave.saveCopy(written, target));
        Assertions.assertEquals(CONTENTS, Files.readString(target, StandardCharsets.UTF_8));
        Assertions.assertTrue(ReplayAutosave.saveCopy(written, written), "the same file is kept");
    }
}
