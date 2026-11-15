package org.fogofwarlabs.sc4j.manifest.test;

import org.fogofwarlabs.sc4j.manifest.settings.InstallationRecords;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Stands in for the computer's installation records in tests: the uninstall entries and program folders a test lays out, so a test
 * never reads the real registry.
 */
public final class FakeInstallationRecords implements InstallationRecords {
    private final @NotNull Map<@NotNull String, @NotNull List<@NotNull Path>> uninstallLocations;
    private final @NotNull List<@NotNull Path> programDirectories;

    /**
     * Creates a {@link FakeInstallationRecords} instance.
     *
     * @param uninstallLocations the install locations by uninstall entry name
     * @param programDirectories the program folders
     */
    public FakeInstallationRecords(@NotNull Map<@NotNull String, @NotNull List<@NotNull Path>> uninstallLocations,
                                   @NotNull List<@NotNull Path> programDirectories) {
        this.uninstallLocations = Map.copyOf(uninstallLocations);
        this.programDirectories = List.copyOf(programDirectories);
    }

    /**
     * Returns the install locations of the uninstall entries with a name.
     *
     * @param entryName the entry name, such as {@code Example Game}
     * @return the recorded install folders, in the order they are looked up; empty when no entry has the name
     */
    @Override
    public @NotNull List<@NotNull Path> findUninstallLocations(@NotNull String entryName) {
        return this.uninstallLocations.getOrDefault(entryName, List.of());
    }

    /**
     * @return the folders programs are installed into by default, in the order they are looked at
     */
    @Override
    public @NotNull List<@NotNull Path> getProgramDirectories() {
        return this.programDirectories;
    }
}
