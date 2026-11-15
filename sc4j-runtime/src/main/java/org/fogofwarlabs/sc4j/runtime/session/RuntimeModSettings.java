package org.fogofwarlabs.sc4j.runtime.session;

import org.fogofwarlabs.sc4j.engine.mod.ModSettings;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValues;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Gives a mod the setting values the runtime read from the settings value file and validated against the mod's schema.
 */
final class RuntimeModSettings implements ModSettings {
    private final @NotNull SettingValues values;

    /**
     * Creates a {@link RuntimeModSettings} instance.
     *
     * @param values the validated values
     */
    RuntimeModSettings(@NotNull SettingValues values) {
        this.values = values;
    }

    /**
     * Checks whether a setting has a value, saved or default.
     *
     * @param key the setting key
     * @return {@code true} if the setting has a value, {@code false} otherwise
     */
    @Override
    public boolean hasValue(@NotNull String key) {
        return this.values.hasValue(key);
    }

    /**
     * Returns the value of a {@code directory} setting.
     *
     * @param key the setting key
     * @return the directory
     * @throws IllegalArgumentException if the schema has no {@code directory} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    @Override
    public @NotNull Path getDirectory(@NotNull String key) {
        return this.values.getDirectory(key);
    }

    /**
     * Returns the value of a {@code file} setting.
     *
     * @param key the setting key
     * @return the file
     * @throws IllegalArgumentException if the schema has no {@code file} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    @Override
    public @NotNull Path getFile(@NotNull String key) {
        return this.values.getFile(key);
    }

    /**
     * Returns the value of a {@code choice} setting.
     *
     * @param key the setting key
     * @return the chosen option
     * @throws IllegalArgumentException if the schema has no {@code choice} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    @Override
    public @NotNull String getChoice(@NotNull String key) {
        return this.values.getChoice(key);
    }

    /**
     * Returns the value of a {@code boolean} setting.
     *
     * @param key the setting key
     * @return the value
     * @throws IllegalArgumentException if the schema has no {@code boolean} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    @Override
    public boolean getBoolean(@NotNull String key) {
        return this.values.getBoolean(key);
    }

    /**
     * Returns the value of an {@code integer} setting.
     *
     * @param key the setting key
     * @return the value
     * @throws IllegalArgumentException if the schema has no {@code integer} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    @Override
    public int getInteger(@NotNull String key) {
        return this.values.getInteger(key);
    }

    /**
     * Returns the value of a {@code string} setting.
     *
     * @param key the setting key
     * @return the value
     * @throws IllegalArgumentException if the schema has no {@code string} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    @Override
    public @NotNull String getString(@NotNull String key) {
        return this.values.getString(key);
    }
}
