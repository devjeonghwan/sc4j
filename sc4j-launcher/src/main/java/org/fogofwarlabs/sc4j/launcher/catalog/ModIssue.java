package org.fogofwarlabs.sc4j.launcher.catalog;

import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.jetbrains.annotations.NotNull;

/**
 * Describes one reason why a mod is not {@link ModStatus#READY}: a problem of its jar or manifest, an id shared with
 * another jar, or a problem of its saved settings.
 */
public sealed interface ModIssue permits ManifestIssue, IdConflictIssue, SettingsIssue {
    /**
     * @return the status the issue gives the mod, {@link ModStatus#WARNING} or {@link ModStatus#ERROR}
     */
    @NotNull ModStatus getStatus();

    /**
     * Describes the issue for the user.
     *
     * @param texts the launcher texts of the display language
     * @return the localized reason
     */
    @NotNull String describe(@NotNull LauncherTexts texts);

    /**
     * Describes the issue on one line in English, for the console and for copied reports.
     *
     * @return the technical description, such as {@code field.missing (entry): The field is required.}
     */
    @NotNull String describeDetail();
}
