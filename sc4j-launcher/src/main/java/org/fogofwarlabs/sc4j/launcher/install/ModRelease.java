package org.fogofwarlabs.sc4j.launcher.install;

import org.fogofwarlabs.sc4j.launcher.release.ReleaseAsset;
import org.fogofwarlabs.sc4j.launcher.release.ReleaseRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Describes the release a mod installs from: the repository, the release tag, the mod jar and the jar's checksum file.
 */
public final class ModRelease {
    private final @NotNull ReleaseRepository repository;
    private final @NotNull String tag;
    private final @NotNull ReleaseAsset jarFile;
    private final @NotNull ReleaseAsset checksumFile;

    /**
     * Creates a {@link ModRelease} instance.
     *
     * @param repository   the repository
     * @param tag          the release tag, such as {@code v1.2.0}
     * @param jarFile      the mod jar {@code <id>.jar}
     * @param checksumFile the jar's checksum file {@code <id>.jar.sha256}
     */
    public ModRelease(@NotNull ReleaseRepository repository, @NotNull String tag, @NotNull ReleaseAsset jarFile, @NotNull ReleaseAsset checksumFile) {
        this.repository   = repository;
        this.tag          = tag;
        this.jarFile      = jarFile;
        this.checksumFile = checksumFile;
    }

    /**
     * @return the repository
     */
    public @NotNull ReleaseRepository getRepository() {
        return this.repository;
    }

    /**
     * @return the release tag, such as {@code v1.2.0}
     */
    public @NotNull String getTag() {
        return this.tag;
    }

    /**
     * @return the mod jar {@code <id>.jar}
     */
    public @NotNull ReleaseAsset getJarFile() {
        return this.jarFile;
    }

    /**
     * @return the jar's checksum file {@code <id>.jar.sha256}
     */
    public @NotNull ReleaseAsset getChecksumFile() {
        return this.checksumFile;
    }
}
