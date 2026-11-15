package org.fogofwarlabs.sc4j.runtime.session;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * Tests that a mod's storage resolves paths inside its folder, rejects every path that leaves it and holds the mod's one settings file.
 */
final class DirectoryModStorageTest {
    @Test
    void resolve_should_stay_inside_the_storage_folder(@TempDir @NotNull Path directory) {
        DirectoryModStorage storage = new DirectoryModStorage(directory.resolve("mods").resolve("my-mod"));
        Path root = storage.getRoot();

        Assertions.assertEquals(root.resolve("replays").resolve("last-game.sc4r"), storage.resolve("replays/last-game.sc4r"));
        Assertions.assertEquals(root.resolve("maps"), storage.resolve("save/../maps"));
        Assertions.assertEquals(root, storage.resolve(""));
    }

    @Test
    void resolve_should_reject_paths_that_leave_the_storage_folder(@TempDir @NotNull Path directory) {
        DirectoryModStorage storage = new DirectoryModStorage(directory.resolve("mods").resolve("my-mod"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> storage.resolve("../demo/save"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> storage.resolve("replays/../../my-mod-other"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> storage.resolve(directory.toAbsolutePath().toString()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> storage.resolve("/maps"));
    }

    @Test
    void get_settings_file_should_name_the_settings_file_of_the_storage_folder(@TempDir @NotNull Path directory) {
        DirectoryModStorage myMod = new DirectoryModStorage(directory.resolve("mods").resolve("my-mod"));
        DirectoryModStorage demo = new DirectoryModStorage(directory.resolve("mods").resolve("demo"));

        Assertions.assertEquals(myMod.getRoot().resolve("sc4j-settings.json"), myMod.getSettingsFile());
        Assertions.assertNotEquals(myMod.getSettingsFile(), demo.getSettingsFile(), "every mod has its own settings");
    }
}
