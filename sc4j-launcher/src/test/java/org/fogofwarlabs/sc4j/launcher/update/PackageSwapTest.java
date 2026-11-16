package org.fogofwarlabs.sc4j.launcher.update;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Tests the replacement of an install's programs by an unpacked user package: the replaced items, the files that stay, and the old programs
 * restored when a step fails.
 */
final class PackageSwapTest {
    private static final @NotNull String OLD = "old";
    private static final @NotNull String NEW = "new";
    private static final @NotNull String USER_FILE = "userdata/launcher.json";
    private static final @NotNull String MOD_FILE = "mods/demo.jar";
    private static final @NotNull String OTHER_FILE = "notes.txt";
    private static final @NotNull String OLD_ONLY_FILE = "launcher/lib/old-only.jar";
    private static final @NotNull String NEW_ONLY_FILE = "launcher/lib/new-only.jar";
    private static final int ONE_ATTEMPT = 1;
    private static final long NO_DELAY = 0;

    @Test
    void apply_should_replace_the_programs_and_keep_every_other_file(@TempDir @NotNull Path directory) throws IOException {
        Path installRoot = directory.resolve("install");
        Path packageRoot = directory.resolve("package");

        writeInstall(installRoot);
        UserPackageScenario.writeFiles(packageRoot, UserPackageScenario.createProgramFiles(NEW));
        UserPackageScenario.writeFiles(packageRoot, Map.of(NEW_ONLY_FILE, NEW, MOD_FILE, NEW));

        new PackageSwap(installRoot, packageRoot, directory.resolve("backup"), ONE_ATTEMPT, NO_DELAY).apply();

        for (String file : UserPackageScenario.createProgramFiles(NEW).keySet()) {
            Assertions.assertEquals(NEW, Files.readString(installRoot.resolve(file), StandardCharsets.UTF_8), file);
            Assertions.assertEquals(NEW, Files.readString(packageRoot.resolve(file), StandardCharsets.UTF_8), "the package stays: " + file);
        }

        Assertions.assertEquals(NEW, Files.readString(installRoot.resolve(NEW_ONLY_FILE), StandardCharsets.UTF_8));
        Assertions.assertFalse(Files.exists(installRoot.resolve(OLD_ONLY_FILE)), "a program folder is replaced as a whole");
        assertKept(installRoot);
    }

    @Test
    void apply_should_restore_the_old_programs_when_a_step_fails(@TempDir @NotNull Path directory) throws IOException {
        Path installRoot = directory.resolve("install");
        Path packageRoot = directory.resolve("package");
        Path backupRoot = directory.resolve("backup");
        PackageSwap swap = new PackageSwap(installRoot, packageRoot, backupRoot, ONE_ATTEMPT, NO_DELAY);

        writeInstall(installRoot);
        UserPackageScenario.writeFiles(packageRoot, UserPackageScenario.createProgramFiles(NEW));
        // A backup in the way makes the move of NOTICE fail after the items before it were replaced.
        UserPackageScenario.writeFiles(backupRoot, Map.of("NOTICE", OLD));

        Assertions.assertThrows(IOException.class, swap::apply);

        for (String file : UserPackageScenario.createProgramFiles(OLD).keySet()) {
            Assertions.assertEquals(OLD, Files.readString(installRoot.resolve(file), StandardCharsets.UTF_8), file);
        }

        Assertions.assertEquals(OLD, Files.readString(installRoot.resolve(OLD_ONLY_FILE), StandardCharsets.UTF_8));
        assertKept(installRoot);
    }

    @Test
    void apply_should_remove_the_windows_programs_when_the_package_has_none(@TempDir @NotNull Path directory) throws IOException {
        Path installRoot = directory.resolve("install");
        Path packageRoot = directory.resolve("package");
        Map<String, String> files = UserPackageScenario.createProgramFiles(NEW);

        files.keySet().removeIf(PackageSwapTest::isWindowsProgram);
        writeInstall(installRoot);
        UserPackageScenario.writeFiles(packageRoot, files);

        new PackageSwap(installRoot, packageRoot, directory.resolve("backup"), ONE_ATTEMPT, NO_DELAY).apply();

        for (String file : files.keySet()) {
            Assertions.assertEquals(NEW, Files.readString(installRoot.resolve(file), StandardCharsets.UTF_8), file);
        }

        for (String item : PackageSwap.WINDOWS_ITEMS) {
            Assertions.assertFalse(Files.exists(installRoot.resolve(item)), item);
        }

        assertKept(installRoot);
    }

    @Test
    void check_package_should_refuse_a_package_without_every_program(@TempDir @NotNull Path directory) throws IOException {
        Map<String, String> files = UserPackageScenario.createProgramFiles(NEW);

        files.remove("NOTICE");
        UserPackageScenario.writeFiles(directory, files);

        Assertions.assertThrows(IOException.class, () -> PackageSwap.checkPackage(directory));
    }

    private static boolean isWindowsProgram(@NotNull String file) {
        return PackageSwap.WINDOWS_ITEMS.stream().anyMatch(item -> file.equals(item) || file.startsWith(item + "/"));
    }

    private static void writeInstall(@NotNull Path installRoot) throws IOException {
        UserPackageScenario.writeFiles(installRoot, UserPackageScenario.createProgramFiles(OLD));
        UserPackageScenario.writeFiles(installRoot, Map.of(OLD_ONLY_FILE, OLD, USER_FILE, OLD, MOD_FILE, OLD, OTHER_FILE, OLD));
    }

    private static void assertKept(@NotNull Path installRoot) throws IOException {
        for (String file : new String[]{USER_FILE, MOD_FILE, OTHER_FILE}) {
            Assertions.assertEquals(OLD, Files.readString(installRoot.resolve(file), StandardCharsets.UTF_8), "kept: " + file);
        }
    }
}
