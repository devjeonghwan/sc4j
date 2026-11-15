package org.fogofwarlabs.sc4j.launcher.catalog;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Reports that other jars in {@code mods/} declare the same mod id. Every jar of such a group is listed with this issue,
 * and none can be started until only one is left: the id names the mod's settings, storage and log folders.
 */
public final class IdConflictIssue implements ModIssue {
    private static final @NotNull String CONFLICT_KEY = "issue.id.conflict";
    private static final @NotNull String DETAIL_FORMAT = "id.conflict ({0}): {1} and {2} declare the same id";

    private final @NotNull String id;
    private final @NotNull String jarName;
    private final @NotNull List<@NotNull String> otherJarNames;

    /**
     * Creates a {@link IdConflictIssue} instance.
     *
     * @param id            the shared mod id
     * @param jarName       the file name of the jar this issue belongs to
     * @param otherJarNames the file names of the other jars with the id, in list order
     */
    public IdConflictIssue(@NotNull String id, @NotNull String jarName, @NotNull List<@NotNull String> otherJarNames) {
        this.id            = id;
        this.jarName       = jarName;
        this.otherJarNames = List.copyOf(otherJarNames);
    }

    /**
     * @return {@link ModStatus#ERROR}: no jar of the group can be started
     */
    @Override
    public @NotNull ModStatus getStatus() {
        return ModStatus.ERROR;
    }

    /**
     * @return the shared mod id
     */
    public @NotNull String getId() {
        return this.id;
    }

    /**
     * @return the file names of the other jars with the id, in list order
     */
    public @NotNull List<@NotNull String> getOtherJarNames() {
        return this.otherJarNames;
    }

    /**
     * Describes the issue for the user.
     *
     * @param texts the launcher texts of the display language
     * @return the localized reason, naming the id and the jars
     */
    @Override
    public @NotNull String describe(@NotNull LauncherTexts texts) {
        return texts.format(CONFLICT_KEY, this.id, this.jarName, texts.joinList(this.otherJarNames));
    }

    /**
     * Describes the issue on one line in English, for the console and for copied reports.
     *
     * @return the technical description, such as {@code id.conflict (my-mod): my-mod.jar and my-mod-copy.jar declare the same id}
     */
    @Override
    public @NotNull String describeDetail() {
        return Exceptions.format(DETAIL_FORMAT, this.id, this.jarName, String.join(", ", this.otherJarNames));
    }
}
