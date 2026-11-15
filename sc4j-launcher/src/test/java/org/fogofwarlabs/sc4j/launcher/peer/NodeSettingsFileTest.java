package org.fogofwarlabs.sc4j.launcher.peer;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests the node settings file: the automatic role on the default port when missing, written and read back, and refused when broken.
 */
final class NodeSettingsFileTest {
    private static final int CHOSEN_PORT = 17455;

    @Test
    void read_should_give_the_automatic_role_on_the_default_port_for_a_missing_file(@TempDir @NotNull Path directory) throws IOException {
        NodeSettings settings = new NodeSettingsFile(directory.resolve(NodeSettingsFile.FILE_NAME)).read();

        Assertions.assertEquals(NodeRole.AUTOMATIC, settings.getRole());
        Assertions.assertEquals(NodeSettings.DEFAULT_PORT, settings.getPort());
    }

    @Test
    void write_and_read_should_keep_the_setting(@TempDir @NotNull Path directory) throws IOException {
        Path path = directory.resolve("beaconlink").resolve(NodeSettingsFile.FILE_NAME);
        NodeSettingsFile file = new NodeSettingsFile(path);

        file.write(new NodeSettings(NodeRole.OFF, CHOSEN_PORT));

        NodeSettings settings = file.read();

        Assertions.assertEquals(NodeRole.OFF, settings.getRole());
        Assertions.assertEquals(CHOSEN_PORT, settings.getPort());
        Assertions.assertTrue(Files.readString(path, StandardCharsets.UTF_8).contains("\"off\""), "the role is written by its name");
    }

    @Test
    void read_should_throw_for_a_broken_file(@TempDir @NotNull Path directory) throws IOException {
        Path path = directory.resolve(NodeSettingsFile.FILE_NAME);
        NodeSettingsFile file = new NodeSettingsFile(path);

        Files.writeString(path, "{\"format\": 1, \"role\": \"automatic\", \"port\": 70000}", StandardCharsets.UTF_8);

        Assertions.assertThrows(IOException.class, file::read);

        Files.writeString(path, "{\"format\": 1, \"role\": \"public\", \"port\": 7455}", StandardCharsets.UTF_8);

        Assertions.assertThrows(IOException.class, file::read);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NodeSettings(NodeRole.AUTOMATIC, 0));
    }
}
