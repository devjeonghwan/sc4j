package org.fogofwarlabs.sc4j.launcher;

import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.fogofwarlabs.sc4j.manifest.test.TestJarUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Tests the launcher's console mod list ({@code --list}), which reads an install without opening a window.
 */
final class LauncherMainTest {
    private static final @NotNull String MANIFEST = """
                                                    {"format": 1, "id": "example", "name": {"en": "Example"}, "version": "1.0.0", "engine": "0.1", "entry": "org.example.ExampleMod"}
                                                    """;

    @Test
    void print_catalog_should_list_each_jar_with_its_status_and_reasons(@TempDir @NotNull Path directory) throws IOException {
        InstallLayout install = InstallLayout.of(directory);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        Files.createDirectories(install.getModsDirectory());
        TestJarUtils.write(install.getModsDirectory().resolve("example.jar"),
                           Map.of(TestJarUtils.MANIFEST_ENTRY, MANIFEST.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertEquals(0, LauncherMain.printCatalog(install, new PrintStream(bytes, true, StandardCharsets.UTF_8)));

        TestJarUtils.write(install.getModsDirectory().resolve("broken-test.jar"), Map.of("readme.txt", new byte[0]));

        Assertions.assertEquals(1, LauncherMain.printCatalog(install, new PrintStream(bytes, true, StandardCharsets.UTF_8)));

        String text = bytes.toString(StandardCharsets.UTF_8);

        Assertions.assertTrue(text.contains("READY   example              example.jar"), text);
        Assertions.assertTrue(text.contains("ERROR   -                    broken-test.jar"), text);
        Assertions.assertTrue(text.contains("        manifest.missing (sc4j-mod.json): The jar has no sc4j-mod.json at its root."), text);
    }
}
