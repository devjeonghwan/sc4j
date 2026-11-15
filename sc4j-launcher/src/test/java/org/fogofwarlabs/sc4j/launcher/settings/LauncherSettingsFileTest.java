package org.fogofwarlabs.sc4j.launcher.settings;

import org.fogofwarlabs.sc4j.launcher.i18n.UiLanguage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests reading and writing the launcher settings file {@code userdata/launcher.json}.
 */
final class LauncherSettingsFileTest {
    @Test
    void write_and_read_should_keep_every_setting(@TempDir @NotNull Path directory) throws IOException {
        LauncherSettingsFile file = new LauncherSettingsFile(directory.resolve("userdata").resolve("launcher.json"));
        LauncherSettings settings = new LauncherSettings(UiLanguage.KOREAN, "my-mod.jar", new WindowState(-8, 40, 1280, 800, true));

        file.write(settings);

        LauncherSettings read = file.read();
        WindowState window = read.getWindow();

        Assertions.assertEquals(UiLanguage.KOREAN, read.getLanguage());
        Assertions.assertEquals("my-mod.jar", read.getLastMod());
        Assertions.assertNotNull(window);
        Assertions.assertEquals(-8, window.getX());
        Assertions.assertEquals(40, window.getY());
        Assertions.assertEquals(1280, window.getWidth());
        Assertions.assertEquals(800, window.getHeight());
        Assertions.assertTrue(window.isMaximized());
    }

    @Test
    void write_should_leave_out_the_language_that_follows_the_system(@TempDir @NotNull Path directory) throws IOException {
        LauncherSettingsFile file = new LauncherSettingsFile(directory.resolve("launcher.json"));

        file.write(LauncherSettings.DEFAULT.withLastMod("demo.jar"));

        Assertions.assertEquals("{\"format\": 1, \"lastMod\": \"demo.jar\"}\n", Files.readString(file.getPath(), StandardCharsets.UTF_8));
        Assertions.assertNull(file.read().getLanguage());
        Assertions.assertNull(file.read().getWindow());
    }

    @Test
    void read_should_return_the_defaults_without_a_file(@TempDir @NotNull Path directory) throws IOException {
        Assertions.assertSame(LauncherSettings.DEFAULT, new LauncherSettingsFile(directory.resolve("launcher.json")).read());
    }

    @Test
    void read_should_reject_an_invalid_file(@TempDir @NotNull Path directory) throws IOException {
        Path path = directory.resolve("launcher.json");

        Files.writeString(path, "{\"format\": 1, \"language\": \"xx\"}", StandardCharsets.UTF_8);

        Assertions.assertThrows(IOException.class, () -> new LauncherSettingsFile(path).read());
    }
}
