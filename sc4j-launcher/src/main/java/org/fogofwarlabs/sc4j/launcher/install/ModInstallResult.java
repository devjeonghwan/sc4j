package org.fogofwarlabs.sc4j.launcher.install;

import org.fogofwarlabs.sc4j.launcher.catalog.ModIssue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tells how a mod install ended: the jar that joined {@code mods/}, or the reasons the jar was refused.
 */
public final class ModInstallResult {
    private final @Nullable String jarName;
    private final @NotNull List<@NotNull ModIssue> issues;

    /**
     * Creates a {@link ModInstallResult} instance.
     *
     * @param jarName the file name of the installed jar, or {@code null} when the jar was refused
     * @param issues  the reasons the jar was refused, empty when it was installed
     */
    public ModInstallResult(@Nullable String jarName, @NotNull List<@NotNull ModIssue> issues) {
        this.jarName = jarName;
        this.issues  = List.copyOf(issues);
    }

    /**
     * @return the file name of the installed jar in {@code mods/}, or {@code null} when the jar was refused
     */
    public @Nullable String getJarName() {
        return this.jarName;
    }

    /**
     * @return the reasons the jar was refused, empty when it was installed
     */
    public @NotNull List<@NotNull ModIssue> getIssues() {
        return this.issues;
    }
}
