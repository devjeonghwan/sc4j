package org.fogofwarlabs.sc4j.launcher.install;

import org.fogofwarlabs.sc4j.launcher.release.ReleaseRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes one mod of the catalog: the name it is listed with, the repository it installs from and an optional description.
 */
public final class ModIndexEntry {
    private final @NotNull String name;
    private final @NotNull ReleaseRepository repository;
    private final @Nullable String description;

    /**
     * Creates a {@link ModIndexEntry} instance.
     *
     * @param name        the name the mod is listed with
     * @param repository  the repository whose releases carry the mod
     * @param description the description, or {@code null}
     */
    public ModIndexEntry(@NotNull String name, @NotNull ReleaseRepository repository, @Nullable String description) {
        this.name        = name;
        this.repository  = repository;
        this.description = description;
    }

    /**
     * @return the name the mod is listed with
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the repository whose releases carry the mod
     */
    public @NotNull ReleaseRepository getRepository() {
        return this.repository;
    }

    /**
     * @return the description, or {@code null}
     */
    public @Nullable String getDescription() {
        return this.description;
    }
}
