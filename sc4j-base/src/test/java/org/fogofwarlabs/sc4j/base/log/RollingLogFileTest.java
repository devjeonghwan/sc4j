package org.fogofwarlabs.sc4j.base.log;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests that the rolling log file rolls by size and keeps the configured number of files.
 */
final class RollingLogFileTest {
    /**
     * The rolling file of the test, its kept file count and its size limit in bytes.
     */
    private static final @NotNull String ROLLING_FILE_NAME = "test.log";
    private static final int ROLLING_FILE_COUNT = 3;
    private static final int ROLLING_SIZE_LIMIT = 100;

    @Test
    void write_should_roll_files_by_size_and_keep_configured_count(@TempDir @NotNull Path directory) throws IOException {
        RollingLogFile file = new RollingLogFile(directory, ROLLING_FILE_NAME, ROLLING_FILE_COUNT, ROLLING_SIZE_LIMIT);

        for (int line = 0; line < 20; line++) {
            Assertions.assertTrue(file.write("line " + line + " " + "x".repeat(40)));
        }

        file.close();

        Assertions.assertTrue(Files.exists(directory.resolve(ROLLING_FILE_NAME)));
        Assertions.assertTrue(Files.exists(directory.resolve("test.1.log")));
        Assertions.assertTrue(Files.exists(directory.resolve("test.2.log")));
        Assertions.assertFalse(Files.exists(directory.resolve("test.3.log")));
        Assertions.assertTrue(Files.size(directory.resolve(ROLLING_FILE_NAME)) <= ROLLING_SIZE_LIMIT);
    }
}
