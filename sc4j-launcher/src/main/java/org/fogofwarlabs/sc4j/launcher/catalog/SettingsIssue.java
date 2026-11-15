package org.fogofwarlabs.sc4j.launcher.catalog;

import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.manifest.settings.SettingDefinition;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationProblem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reports a problem of a mod's saved pre-run settings ({@code userdata/settings/<modId>.json}): a required value is
 * missing, a value fails its declarative checks, or the file cannot be read. The mod can be started once the settings
 * form has fixed it.
 */
public final class SettingsIssue implements ModIssue {
    private final @NotNull ValidationProblem problem;
    private final @Nullable SettingDefinition definition;

    /**
     * Creates a {@link SettingsIssue} instance.
     *
     * @param problem    the problem the settings schema or the settings value file reported
     * @param definition the setting the problem belongs to, or {@code null} for a problem of the whole file
     */
    public SettingsIssue(@NotNull ValidationProblem problem, @Nullable SettingDefinition definition) {
        this.problem    = problem;
        this.definition = definition;
    }

    /**
     * @return {@link ModStatus#WARNING}: the settings form can fix the settings before the start
     */
    @Override
    public @NotNull ModStatus getStatus() {
        return ModStatus.WARNING;
    }

    /**
     * @return the problem the settings schema or the settings value file reported
     */
    public @NotNull ValidationProblem getProblem() {
        return this.problem;
    }

    /**
     * @return the setting the problem belongs to, or {@code null} for a problem of the whole file
     */
    public @Nullable SettingDefinition getDefinition() {
        return this.definition;
    }

    /**
     * Describes the issue for the user.
     *
     * @param texts the launcher texts of the display language
     * @return the localized reason
     */
    @Override
    public @NotNull String describe(@NotNull LauncherTexts texts) {
        return this.definition == null ? texts.describeProblem(this.problem) : texts.describeSettingProblem(this.problem, this.definition);
    }

    /**
     * Describes the issue on one line in English, for the console and for copied reports.
     *
     * @return the technical description, such as {@code value.required (installDirectory): A value is required.}
     */
    @Override
    public @NotNull String describeDetail() {
        return this.problem.describe();
    }
}
