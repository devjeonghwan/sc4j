package org.fogofwarlabs.sc4j.base.log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Defines logger severity levels, from the most to the least severe.
 */
public enum LogLevel {
    /**
     * A failure the program cannot recover from for the current operation.
     */
    ERROR(5, "ERROR"),
    /**
     * A problem the program works around, such as a missing optional asset.
     */
    WARNING(4, "WARN"),
    /**
     * A notable event, such as a loaded replay or a saved screenshot.
     */
    INFO(3, "INFO"),
    /**
     * Diagnostic detail for developers.
     */
    DEBUG(2, "DEBUG"),
    /**
     * Fine-grained diagnostic detail, such as per-frame statistics.
     */
    TRACE(1, "TRACE"),
    /**
     * Disables logging; only a threshold, never a message level.
     */
    OFF(999, "OFF");

    /**
     * Parses a level name, case-insensitively; {@code warn} and {@code warning} both name {@link #WARNING}.
     *
     * @param name the level name
     * @return the level, or {@code null} if the name is unknown
     */
    public static @Nullable LogLevel parse(@Nullable String name) {
        if (name == null) {
            return null;
        }

        String normalized = name.trim().toUpperCase(Locale.ROOT);

        for (LogLevel level : values()) {
            if (level.name().equals(normalized) || level.title.equals(normalized)) {
                return level;
            }
        }

        return null;
    }


    private final int priority;
    private final @NotNull String title;

    LogLevel(int priority, @NotNull String title) {
        this.priority = priority;
        this.title    = title;
    }

    /**
     * @return the priority: a message is written when its priority is at least the threshold's
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * @return the short title written in log lines
     */
    public @NotNull String getTitle() {
        return this.title;
    }
}
