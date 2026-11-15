package org.fogofwarlabs.sc4j.engine.client.settings;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Tests the engine settings store: groups and keys read in file order from a file in the layout the store writes, typed reads with their
 * failures, and writes that replace only the given keys, keep every other group and key and leave no temporary file.
 */
final class SettingsStoreTest {
    /**
     * A settings file in the stable layout the store writes (one line per group).
     */
    private static final @NotNull String MOD_SETTINGS = """
                                                        {
                                                          "createGame": {"type": 3, "speed": 6, "subtype": 1},
                                                          "graphics": {"shadows": true, "quality": "high", "vsync": "off", "fpsLimitEnabled": true, "fpsLimit": 300, "windowMode": 1},
                                                          "game": {"gameSpeed": 6, "gameTimer": true, "hintNumber": 101, "language": "en-US"},
                                                          "input": {"mouseConfine": false, "hardwareCursor": true},
                                                          "audio": {"set": "classic", "musicVolume": 75, "sfxVolume": 35, "musicEnabled": false}
                                                        }
                                                        """;
    private static final int FRAME_RATE_LIMIT = 300;
    private static final int MAXIMUM_FRAME_RATE_LIMIT = 1000;
    private static final int MUSIC_VOLUME = 75;
    private static final int MAXIMUM_VOLUME = 100;
    private static final int CHANGED_VOLUME = 40;

    private static @NotNull Path writeFile(@NotNull Path directory, @NotNull String contents) throws IOException {
        Path file = directory.resolve(SettingsStore.FILE_NAME);

        Files.writeString(file, contents, StandardCharsets.UTF_8);

        return file;
    }

    @Test
    void read_should_keep_groups_and_keys_in_file_order(@TempDir @NotNull Path directory) throws IOException {
        SettingsStore store = SettingsStore.read(writeFile(directory, MOD_SETTINGS));

        Assertions.assertEquals(List.of("createGame", "graphics", "game", "input", "audio"), store.getGroupNames());

        SettingsGroup graphics = store.getGroup("graphics");

        Assertions.assertNotNull(graphics);
        Assertions.assertEquals("graphics", graphics.getName());
        Assertions.assertEquals(List.of("shadows", "quality", "vsync", "fpsLimitEnabled", "fpsLimit", "windowMode"), graphics.getKeys());
        Assertions.assertTrue(graphics.getBoolean("shadows"));
        Assertions.assertEquals("high", graphics.getString("quality"));
        Assertions.assertEquals(FRAME_RATE_LIMIT, graphics.getInteger("fpsLimit", 0, MAXIMUM_FRAME_RATE_LIMIT));
        Assertions.assertEquals("graphics.vsync", graphics.getQualifiedName("vsync"));
        Assertions.assertTrue(graphics.hasValue("vsync"));
        Assertions.assertFalse(graphics.hasValue("gamma"));
        Assertions.assertNull(store.getGroup("paths"));
    }

    @Test
    void typed_reads_reject_wrong_types_and_ranges(@TempDir @NotNull Path directory) throws IOException {
        SettingsStore store = SettingsStore.read(writeFile(directory, MOD_SETTINGS));
        SettingsGroup audio = store.getGroup("audio");

        Assertions.assertNotNull(audio);
        Assertions.assertEquals(MUSIC_VOLUME, audio.getInteger("musicVolume", 0, MAXIMUM_VOLUME));

        IllegalArgumentException range = Assertions.assertThrows(IllegalArgumentException.class,
                                                                 () -> audio.getInteger("musicVolume", 0, MUSIC_VOLUME - 1));

        Assertions.assertTrue(range.getMessage().startsWith("audio.musicVolume must be a number from 0 to "), range.getMessage());
        Assertions.assertThrows(IllegalArgumentException.class, () -> audio.getBoolean("set"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> audio.getString("musicEnabled"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> audio.getInteger("set", 0, MAXIMUM_VOLUME));
        Assertions.assertThrows(IllegalArgumentException.class, () -> audio.getBoolean("absent"));
    }

    @Test
    void read_should_name_entries_that_are_not_groups(@TempDir @NotNull Path directory) throws IOException {
        SettingsStore store = SettingsStore.read(writeFile(directory, "{\"graphics\": {\"vsync\": \"on\"}, \"version\": 2}"));

        Assertions.assertEquals(List.of("graphics", "version"), store.getGroupNames());
        Assertions.assertNull(store.getGroup("version"));
    }

    @Test
    void read_should_refuse_a_document_that_is_no_object(@TempDir @NotNull Path directory) throws IOException {
        Path file = writeFile(directory, "[1, 2]");

        Assertions.assertThrows(IOException.class, () -> SettingsStore.read(file));
        Assertions.assertThrows(IOException.class, () -> SettingsStore.read(directory.resolve("missing.json")));
    }

    @Test
    void write_should_replace_only_the_given_keys_with_stable_layout(@TempDir @NotNull Path directory) throws IOException {
        Path file = writeFile(directory, MOD_SETTINGS);

        SettingsStore.write(file, "audio", Map.of("musicVolume", CHANGED_VOLUME));

        String text = Files.readString(file, StandardCharsets.UTF_8);

        Assertions.assertEquals(MOD_SETTINGS.replace("\"musicVolume\": 75", "\"musicVolume\": 40"), text);

        SettingsStore.write(file, "audio", Map.of("musicVolume", CHANGED_VOLUME));

        Assertions.assertEquals(text, Files.readString(file, StandardCharsets.UTF_8), "the same values leave the same bytes");
    }

    @Test
    void write_should_keep_unknown_groups_and_values(@TempDir @NotNull Path directory) throws IOException {
        Path file = writeFile(directory, """
                                         {
                                           "future": {"anything": [1, 2, {"deep": true}]},
                                           "createGame": {"name": "한글 게임", "type": 2},
                                           "version": 2
                                         }
                                         """);

        SettingsStore.write(file, "createGame", Map.of("type", 10));
        SettingsStore.write(file, "graphics", Map.of("vsync", "off"));

        SettingsStore store = SettingsStore.read(file);
        SettingsGroup createGame = store.getGroup("createGame");
        SettingsGroup future = store.getGroup("future");

        Assertions.assertEquals(List.of("future", "createGame", "version", "graphics"), store.getGroupNames());
        Assertions.assertNotNull(createGame);
        Assertions.assertNotNull(future);
        Assertions.assertEquals("한글 게임", createGame.getString("name"));
        Assertions.assertEquals(10L, createGame.getValue("type"));
        Assertions.assertEquals(List.of(1L, 2L, Map.of("deep", true)), future.getValue("anything"));
    }

    @Test
    void write_should_create_a_missing_file_and_refuse_a_document_that_is_no_object(@TempDir @NotNull Path directory) throws IOException {
        Path file = directory.resolve("mods").resolve("demo").resolve(SettingsStore.FILE_NAME);

        SettingsStore.write(file, "createGame", Map.of("type", 3));

        SettingsGroup createGame = SettingsStore.read(file).getGroup("createGame");

        Assertions.assertNotNull(createGame);
        Assertions.assertEquals(3, createGame.getInteger("type", 0, Integer.MAX_VALUE));

        Files.writeString(file, "[1, 2]", StandardCharsets.UTF_8);

        Assertions.assertThrows(IOException.class, () -> SettingsStore.write(file, "createGame", Map.of("type", 2)));
        Assertions.assertEquals("[1, 2]", Files.readString(file, StandardCharsets.UTF_8));

        try (Stream<Path> left = Files.list(file.getParent())) {
            Assertions.assertEquals(1, left.count(), "no temporary file is left behind");
        }
    }
}
