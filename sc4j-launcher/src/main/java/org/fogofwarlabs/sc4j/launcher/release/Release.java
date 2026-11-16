package org.fogofwarlabs.sc4j.launcher.release;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Describes a release of a repository: its tag and its files.
 */
public final class Release {
    private final @NotNull ReleaseRepository repository;
    private final @NotNull String tag;
    private final @NotNull List<@NotNull ReleaseAsset> assets;

    /**
     * Creates a {@link Release} instance.
     *
     * @param repository the repository
     * @param tag        the release tag, such as {@code v1.2.0}
     * @param assets     the files, in the order GitHub lists them
     */
    public Release(@NotNull ReleaseRepository repository, @NotNull String tag, @NotNull List<@NotNull ReleaseAsset> assets) {
        this.repository = repository;
        this.tag        = tag;
        this.assets     = List.copyOf(assets);
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
     * @return the files, in the order GitHub lists them
     */
    public @NotNull List<@NotNull ReleaseAsset> getAssets() {
        return this.assets;
    }

    /**
     * Finds a file by its name.
     *
     * @param name the file name
     * @return the file, or {@code null} when the release has no such file
     */
    public @Nullable ReleaseAsset findAsset(@NotNull String name) {
        for (ReleaseAsset asset : this.assets) {
            if (asset.getName().equals(name)) {
                return asset;
            }
        }

        return null;
    }
}
