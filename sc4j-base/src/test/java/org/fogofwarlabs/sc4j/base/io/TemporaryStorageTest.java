package org.fogofwarlabs.sc4j.base.io;

import com.sun.management.HotSpotDiagnosticMXBean;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verifies the temporary storage of the module's test process: default temporary files and JUnit temporary directories stay in
 * the module build, and the virtual machine runs without its performance-data file.
 */
final class TemporaryStorageTest {
    private static final @NotNull String TEMPORARY_PREFIX = "sc4j-process-";
    private static final @NotNull String TEMPORARY_SUFFIX = ".tmp";

    @Test
    void temporary_files_and_junit_directory_stay_in_the_module_build(@TempDir @NotNull Path directory) throws IOException {
        Path expected = Path.of("build", "tmp", "test-io").toRealPath();
        Path temporary = Files.createTempFile(TEMPORARY_PREFIX, TEMPORARY_SUFFIX);

        try {
            File legacy = File.createTempFile(TEMPORARY_PREFIX, TEMPORARY_SUFFIX);

            try {
                Assertions.assertEquals(expected, temporary.toRealPath().getParent());
                Assertions.assertEquals(expected, legacy.toPath().toRealPath().getParent());
                Assertions.assertEquals(expected, directory.toRealPath().getParent());
                Assertions.assertEquals("false", ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
                                                                  .getVMOption("UsePerfData").getValue());
            } finally {
                Files.deleteIfExists(legacy.toPath());
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
