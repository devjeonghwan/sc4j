package org.fogofwarlabs.sc4j.engine.mod;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Gives read-only access to the validated pre-run settings of the mod: the values the launcher saved for the manifest's
 * {@code settings} schema, with the defaults of the settings that have none. Each getter reads a setting of its type.
 */
public interface ModSettings {
    /**
     * Checks whether a setting has a value, saved or default.
     *
     * @param key the setting key
     * @return {@code true} if the setting has a value, {@code false} otherwise
     */
    boolean hasValue(@NotNull String key);

    /**
     * Returns the value of a {@code directory} setting.
     *
     * @param key the setting key
     * @return the directory
     * @throws IllegalArgumentException if the schema has no {@code directory} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    @NotNull Path getDirectory(@NotNull String key);

    /**
     * Returns the value of a {@code file} setting.
     *
     * @param key the setting key
     * @return the file
     * @throws IllegalArgumentException if the schema has no {@code file} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    @NotNull Path getFile(@NotNull String key);

    /**
     * Returns the value of a {@code choice} setting.
     *
     * @param key the setting key
     * @return the chosen option
     * @throws IllegalArgumentException if the schema has no {@code choice} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    @NotNull String getChoice(@NotNull String key);

    /**
     * Returns the value of a {@code boolean} setting.
     *
     * @param key the setting key
     * @return the value
     * @throws IllegalArgumentException if the schema has no {@code boolean} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    boolean getBoolean(@NotNull String key);

    /**
     * Returns the value of an {@code integer} setting.
     *
     * @param key the setting key
     * @return the value
     * @throws IllegalArgumentException if the schema has no {@code integer} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    int getInteger(@NotNull String key);

    /**
     * Returns the value of a {@code string} setting.
     *
     * @param key the setting key
     * @return the value
     * @throws IllegalArgumentException if the schema has no {@code string} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    @NotNull String getString(@NotNull String key);
}
