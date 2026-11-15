package org.fogofwarlabs.sc4j.launcher.catalog;

import org.fogofwarlabs.sc4j.manifest.ModArchive;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Describes one jar of {@code mods/} as the launcher lists it: the jar, what reading it gave (the manifest and images, when
 * the manifest is valid), the issues found and the resulting status.
 */
public final class ModEntry {
    private final @NotNull Path jar;
    private final @Nullable ModArchive archive;

    private final @NotNull List<@NotNull ModIssue> issues;
    private final @NotNull ModStatus status;

    /**
     * Creates a {@link ModEntry} instance; the status is the most severe status of the issues, {@link ModStatus#READY}
     * without issues.
     *
     * @param jar     the mod jar
     * @param archive the manifest and images read from the jar, or {@code null} when the jar or its manifest is broken
     * @param issues  the issues found, in the order they were found
     */
    public ModEntry(@NotNull Path jar, @Nullable ModArchive archive, @NotNull List<@NotNull ModIssue> issues) {
        ModStatus combined = ModStatus.READY;

        for (ModIssue issue : issues) {
            combined = combined.combine(issue.getStatus());
        }

        this.jar     = jar;
        this.archive = archive;

        this.issues = List.copyOf(issues);
        this.status = combined;
    }

    /**
     * @return the mod jar
     */
    public @NotNull Path getJar() {
        return this.jar;
    }

    /**
     * @return the file name of the mod jar, such as {@code my-mod.jar}
     */
    public @NotNull String getJarName() {
        Path fileName = this.jar.getFileName();

        return fileName == null ? this.jar.toString() : fileName.toString();
    }

    /**
     * @return the manifest and images read from the jar, or {@code null} when the jar or its manifest is broken
     */
    public @Nullable ModArchive getArchive() {
        return this.archive;
    }

    /**
     * @return the validated manifest, or {@code null} when the jar or its manifest is broken
     */
    public @Nullable ModManifest getManifest() {
        return this.archive == null ? null : this.archive.getManifest();
    }

    /**
     * @return the issues found, in the order they were found; empty for a ready mod
     */
    public @NotNull List<@NotNull ModIssue> getIssues() {
        return this.issues;
    }

    /**
     * @return the status: the most severe status of the issues, {@link ModStatus#READY} without issues
     */
    public @NotNull ModStatus getStatus() {
        return this.status;
    }
}
