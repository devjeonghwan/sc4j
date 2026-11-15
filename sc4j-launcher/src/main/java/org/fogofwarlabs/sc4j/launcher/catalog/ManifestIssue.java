package org.fogofwarlabs.sc4j.launcher.catalog;

import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationProblem;
import org.jetbrains.annotations.NotNull;

/**
 * Reports a problem of a mod jar or its manifest that the manifest module found: the jar cannot be read, the manifest is
 * missing, broken or invalid, an image is missing, or the engine version is not supported. The mod cannot be started.
 */
public final class ManifestIssue implements ModIssue {
    private final @NotNull ValidationProblem problem;

    /**
     * Creates a {@link ManifestIssue} instance.
     *
     * @param problem the problem the manifest module reported
     */
    public ManifestIssue(@NotNull ValidationProblem problem) {
        this.problem = problem;
    }

    /**
     * @return {@link ModStatus#ERROR}: a mod with a broken jar or manifest cannot be started
     */
    @Override
    public @NotNull ModStatus getStatus() {
        return ModStatus.ERROR;
    }

    /**
     * @return the problem the manifest module reported
     */
    public @NotNull ValidationProblem getProblem() {
        return this.problem;
    }

    /**
     * Describes the issue for the user.
     *
     * @param texts the launcher texts of the display language
     * @return the localized reason
     */
    @Override
    public @NotNull String describe(@NotNull LauncherTexts texts) {
        return texts.describeProblem(this.problem);
    }

    /**
     * Describes the issue on one line in English, for the console and for copied reports.
     *
     * @return the technical description, such as {@code field.missing (entry): The field is required.}
     */
    @Override
    public @NotNull String describeDetail() {
        return this.problem.describe();
    }
}
