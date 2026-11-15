package org.fogofwarlabs.sc4j.launcher.settings;

import org.fogofwarlabs.sc4j.launcher.i18n.UiLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the launcher's own settings, {@code userdata/launcher.json}: the UI language chosen on the Settings screen, the
 * mod selected last and the window state. Instances are immutable; each {@code with} method creates a changed copy.
 */
public final class LauncherSettings {
    /**
     * The settings before anything is saved: the system language, no last mod and the default window.
     */
    public static final @NotNull LauncherSettings DEFAULT = new LauncherSettings(null, null, null);

    private final @Nullable UiLanguage language;
    private final @Nullable String lastMod;
    private final @Nullable WindowState window;

    /**
     * Creates a {@link LauncherSettings} instance.
     *
     * @param language the UI language chosen on the Settings screen, or {@code null} to follow the operating system
     * @param lastMod  the file name of the mod jar selected last, such as {@code my-mod.jar}, or {@code null}
     * @param window   the window state, or {@code null} for the default window
     */
    public LauncherSettings(@Nullable UiLanguage language, @Nullable String lastMod, @Nullable WindowState window) {
        this.language = language;
        this.lastMod  = lastMod;
        this.window   = window;
    }

    /**
     * @return the UI language chosen on the Settings screen, or {@code null} to follow the operating system
     */
    public @Nullable UiLanguage getLanguage() {
        return this.language;
    }

    /**
     * @return the file name of the mod jar selected last, or {@code null}
     */
    public @Nullable String getLastMod() {
        return this.lastMod;
    }

    /**
     * @return the window state, or {@code null} for the default window
     */
    public @Nullable WindowState getWindow() {
        return this.window;
    }

    /**
     * Creates a copy with another UI language.
     *
     * @param language the UI language, or {@code null} to follow the operating system
     * @return the created {@link LauncherSettings} instance
     */
    public @NotNull LauncherSettings withLanguage(@Nullable UiLanguage language) {
        return new LauncherSettings(language, this.lastMod, this.window);
    }

    /**
     * Creates a copy with another last mod.
     *
     * @param lastMod the file name of the mod jar selected last, or {@code null}
     * @return the created {@link LauncherSettings} instance
     */
    public @NotNull LauncherSettings withLastMod(@Nullable String lastMod) {
        return new LauncherSettings(this.language, lastMod, this.window);
    }

    /**
     * Creates a copy with another window state.
     *
     * @param window the window state, or {@code null} for the default window
     * @return the created {@link LauncherSettings} instance
     */
    public @NotNull LauncherSettings withWindow(@Nullable WindowState window) {
        return new LauncherSettings(this.language, this.lastMod, window);
    }
}
