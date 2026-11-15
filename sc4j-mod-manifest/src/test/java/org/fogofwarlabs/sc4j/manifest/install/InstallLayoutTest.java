package org.fogofwarlabs.sc4j.manifest.install;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * Tests the install folders and finding the install from a program's class location.
 */
final class InstallLayoutTest {
    @Test
    void install_layout_places_every_folder_below_the_install(@TempDir @NotNull Path directory) {
        InstallLayout install = InstallLayout.of(directory);
        Path userData = directory.resolve("userdata");

        Assertions.assertEquals(directory.resolve("launcher").resolve("seeds.json"), install.getSeedsFile());
        Assertions.assertEquals(directory.resolve("runtime"), install.getRuntimeDirectory());
        Assertions.assertEquals(directory.resolve("mods"), install.getModsDirectory());
        Assertions.assertEquals(userData.resolve("launcher.json"), install.getLauncherSettingsFile());
        Assertions.assertEquals(userData.resolve("settings").resolve("my-mod.json"), install.getSettingsFile("my-mod"));
        Assertions.assertEquals(userData.resolve("mods").resolve("my-mod"), install.getStorageDirectory("my-mod"));
        Assertions.assertEquals(userData.resolve("logs").resolve("my-mod"), install.getLogDirectory("my-mod"));
        Assertions.assertEquals(userData.resolve("beaconlink"), install.getBeaconLinkDirectory());
        Assertions.assertEquals(userData.resolve("beaconlink").resolve("nodes.json"), install.getNodesFile());
        Assertions.assertEquals(directory.resolve("runtime").resolve("bin").resolve("sc4j-runtime.bat"), install.getRuntimeScript("sc4j-runtime.bat"));
    }

    @Test
    void from_code_location_should_find_installs_and_the_development_install(@TempDir @NotNull Path directory) {
        Path installedJar = directory.resolve("runtime").resolve("lib").resolve("sc4j-runtime-0.1.0.jar");
        Path classFolder = directory.resolve("sc4j-runtime").resolve("build").resolve("classes").resolve("java").resolve("main");
        InstallLayout installed = InstallLayout.fromCodeLocation(installedJar, InstallLayout.RUNTIME_FOLDER);
        InstallLayout development = InstallLayout.fromCodeLocation(classFolder, InstallLayout.RUNTIME_FOLDER);

        Assertions.assertEquals(directory, installed == null ? null : installed.getRoot());
        Assertions.assertEquals(directory.resolve("build").resolve("dev-install"), development == null ? null : development.getRoot());
        Assertions.assertNull(InstallLayout.fromCodeLocation(directory.resolve("lib").resolve("sc4j-runtime.jar"), InstallLayout.RUNTIME_FOLDER));
        Assertions.assertNull(InstallLayout.fromCodeLocation(installedJar, InstallLayout.LAUNCHER_FOLDER));
        Assertions.assertNull(InstallLayout.fromCodeLocation(directory.resolve("other"), InstallLayout.RUNTIME_FOLDER));
    }
}
