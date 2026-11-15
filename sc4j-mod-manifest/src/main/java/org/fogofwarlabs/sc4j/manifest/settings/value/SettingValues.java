package org.fogofwarlabs.sc4j.manifest.settings.value;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.manifest.settings.SettingDefinition;
import org.fogofwarlabs.sc4j.manifest.settings.SettingType;
import org.fogofwarlabs.sc4j.manifest.settings.SettingsSchema;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the validated, typed values of a mod's pre-run settings: the saved values with the defaults of the settings
 * that have none. Each getter returns the value of one setting of its type.
 */
public final class SettingValues {
    /**
     * Creates a {@link SettingValues} instance from saved values that pass the schema.
     *
     * @param schema      the mod's settings schema
     * @param savedValues the parsed values by key, as the settings value file holds them
     * @return the created {@link SettingValues} instance
     * @throws SettingValuesException if a required value is missing, or a value has the wrong type or fails its checks
     */
    public static @NotNull SettingValues resolve(@NotNull SettingsSchema schema, @NotNull Map<@NotNull String, ?> savedValues) throws SettingValuesException {
        ValidationResult result = schema.validate(savedValues);

        if (!result.isValid()) {
            throw new SettingValuesException(result);
        }

        Map<String, Object> values = new LinkedHashMap<>();

        for (SettingDefinition definition : schema.getDefinitions()) {
            Object value = definition.resolve(savedValues.get(definition.getKey()));

            if (value != null) {
                values.put(definition.getKey(), value);
            }
        }

        return new SettingValues(schema, values);
    }


    private final @NotNull SettingsSchema schema;
    private final @NotNull Map<@NotNull String, @NotNull Object> values;

    /**
     * Creates a {@link SettingValues} instance.
     *
     * @param schema the settings schema
     * @param values the typed values by key
     */
    private SettingValues(@NotNull SettingsSchema schema, @NotNull Map<@NotNull String, @NotNull Object> values) {
        this.schema = schema;
        this.values = values;
    }

    /**
     * @return the settings schema the values belong to
     */
    public @NotNull SettingsSchema getSchema() {
        return this.schema;
    }

    /**
     * Checks whether a setting has a value, saved or default.
     *
     * @param key the setting key
     * @return {@code true} if the setting has a value, {@code false} otherwise
     */
    public boolean hasValue(@NotNull String key) {
        return this.values.containsKey(key);
    }

    /**
     * Returns the value of a {@code directory} setting.
     *
     * @param key the setting key
     * @return the directory
     * @throws IllegalArgumentException if the schema has no {@code directory} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    public @NotNull Path getDirectory(@NotNull String key) {
        return (Path) this.getValue(key, SettingType.DIRECTORY);
    }

    /**
     * Returns the value of a {@code file} setting.
     *
     * @param key the setting key
     * @return the file
     * @throws IllegalArgumentException if the schema has no {@code file} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    public @NotNull Path getFile(@NotNull String key) {
        return (Path) this.getValue(key, SettingType.FILE);
    }

    /**
     * Returns the value of a {@code choice} setting.
     *
     * @param key the setting key
     * @return the chosen option
     * @throws IllegalArgumentException if the schema has no {@code choice} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    public @NotNull String getChoice(@NotNull String key) {
        return (String) this.getValue(key, SettingType.CHOICE);
    }

    /**
     * Returns the value of a {@code boolean} setting.
     *
     * @param key the setting key
     * @return the value
     * @throws IllegalArgumentException if the schema has no {@code boolean} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    public boolean getBoolean(@NotNull String key) {
        return (Boolean) this.getValue(key, SettingType.BOOLEAN);
    }

    /**
     * Returns the value of an {@code integer} setting.
     *
     * @param key the setting key
     * @return the value
     * @throws IllegalArgumentException if the schema has no {@code integer} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    public int getInteger(@NotNull String key) {
        return (Integer) this.getValue(key, SettingType.INTEGER);
    }

    /**
     * Returns the value of a {@code string} setting.
     *
     * @param key the setting key
     * @return the value
     * @throws IllegalArgumentException if the schema has no {@code string} setting with the key
     * @throws IllegalStateException    if the setting has no value
     */
    public @NotNull String getString(@NotNull String key) {
        return (String) this.getValue(key, SettingType.STRING);
    }

    /**
     * Returns the values as the settings value file stores them ({@link SettingValuesFile#write}).
     *
     * @return the JSON values by key, in schema order
     */
    public @NotNull Map<@NotNull String, @NotNull Object> toSavedValues() {
        Map<String, Object> savedValues = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : this.values.entrySet()) {
            SettingDefinition definition = this.schema.findDefinition(entry.getKey());

            if (definition != null) {
                savedValues.put(entry.getKey(), definition.getType().toJson(entry.getValue()));
            }
        }

        return savedValues;
    }

    private @NotNull Object getValue(@NotNull String key, @NotNull SettingType type) {
        SettingDefinition definition = this.schema.findDefinition(key);

        if (definition == null || definition.getType() != type) {
            throw Exceptions.newIllegalArgumentException("The mod has no {0} setting {1}.", type.getName(), key);
        }

        Object value = this.values.get(key);

        if (value == null) {
            throw Exceptions.newIllegalStateException("The setting {0} has no value.", key);
        }

        return value;
    }
}
