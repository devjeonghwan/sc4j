package org.fogofwarlabs.sc4j.launcher.update;

import org.fogofwarlabs.sc4j.launcher.release.ReleaseAsset;
import org.fogofwarlabs.sc4j.launcher.release.ReleaseVersion;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a release of the update repository that is newer than the running launcher: its version, its user package and the package's
 * checksum file.
 */
public final class LauncherRelease {
    private final @NotNull ReleaseVersion version;
    private final @NotNull ReleaseAsset packageFile;
    private final @NotNull ReleaseAsset checksumFile;

    /**
     * Creates a {@link LauncherRelease} instance.
     *
     * @param version      the release version
     * @param packageFile  the user package {@code SC4J-<version>.zip}
     * @param checksumFile the package's checksum file {@code SC4J-<version>.zip.sha256}
     */
    public LauncherRelease(@NotNull ReleaseVersion version, @NotNull ReleaseAsset packageFile, @NotNull ReleaseAsset checksumFile) {
        this.version      = version;
        this.packageFile  = packageFile;
        this.checksumFile = checksumFile;
    }

    /**
     * @return the release version
     */
    public @NotNull ReleaseVersion getVersion() {
        return this.version;
    }

    /**
     * @return the user package {@code SC4J-<version>.zip}
     */
    public @NotNull ReleaseAsset getPackageFile() {
        return this.packageFile;
    }

    /**
     * @return the package's checksum file {@code SC4J-<version>.zip.sha256}
     */
    public @NotNull ReleaseAsset getChecksumFile() {
        return this.checksumFile;
    }
}
