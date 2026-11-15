package org.fogofwarlabs.sc4j.launcher.catalog;

import org.jetbrains.annotations.NotNull;

/**
 * Lists the statuses of a mod in the launcher's list. Every view shows a mod's status the same way, and the detail view
 * always shows the reasons ({@link ModIssue}).
 */
public enum ModStatus {
    /**
     * The mod can be started.
     */
    READY,
    /**
     * The mod can be started after something is fixed, for example settings that are missing or fail validation; the
     * settings form shows what to fix.
     */
    WARNING,
    /**
     * The mod cannot be started: its jar or manifest is broken, it needs another engine version, an image is missing or
     * another jar has the same id.
     */
    ERROR;

    /**
     * @return {@code true} if a mod with this status can be started, {@code false} otherwise
     */
    public boolean isStartable() {
        return this != ERROR;
    }

    /**
     * Returns the more severe of two statuses.
     *
     * @param other the other status
     * @return this status or the other one, whichever is more severe
     */
    public @NotNull ModStatus combine(@NotNull ModStatus other) {
        return other.ordinal() > this.ordinal() ? other : this;
    }
}
