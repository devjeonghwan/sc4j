package org.fogofwarlabs.sc4j.launcher.form;

import org.fogofwarlabs.sc4j.manifest.settings.SettingDefinition;
import org.fogofwarlabs.sc4j.manifest.settings.SettingType;
import org.fogofwarlabs.sc4j.manifest.settings.SettingsSchema;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValueStore;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValuesException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the pre-run settings form of one mod, generated from its manifest's settings schema: one {@link FormField} per
 * setting, in schema order, filled from the saved values ({@link SettingValueStore#read()}) or the defaults. The field of a shared setting
 * ({@code shared}) is one field for the forms of every mod that declares the same shared name, so a change in one form is the value of all.
 * <p>
 * Validation is the manifest module's declarative validation only. A required setting without a value or a value that
 * fails its checks blocks the start ({@link #isValid()}). Starting saves the values first ({@link #save}).
 */
public final class SettingsForm {
    /**
     * Creates a {@link SettingsForm} instance for a schema and the saved values, with fields of its own for the shared settings.
     *
     * @param schema      the mod's settings schema
     * @param savedValues the saved values by key ({@link SettingValueStore#read()})
     * @return the created {@link SettingsForm} instance, every field validated
     */
    public static @NotNull SettingsForm create(@NotNull SettingsSchema schema, @NotNull Map<@NotNull String, ?> savedValues) {
        return create(schema, savedValues, new HashMap<>());
    }

    /**
     * Creates a {@link SettingsForm} instance for a schema and the saved values. A shared setting uses the field of its shared name when
     * another form created one, otherwise its new field becomes the field of that name.
     *
     * @param schema       the mod's settings schema
     * @param savedValues  the saved values by key ({@link SettingValueStore#read()})
     * @param sharedFields the fields of the shared settings by shared name, which the forms of every mod share; receives the new ones
     * @return the created {@link SettingsForm} instance, every field validated
     */
    public static @NotNull SettingsForm create(@NotNull SettingsSchema schema, @NotNull Map<@NotNull String, ?> savedValues,
                                               @NotNull Map<@NotNull String, @NotNull FormField> sharedFields) {
        Map<String, FormField> fields = new LinkedHashMap<>();

        for (SettingDefinition definition : schema.getDefinitions()) {
            String sharedName = definition.getSharedName();
            FormField field = sharedName == null ? null : sharedFields.get(sharedName);

            if (field == null) {
                Object saved = savedValues.get(definition.getKey());

                field = new FormField(definition, saved != null ? saved : getDefaultValue(definition));
            }

            if (sharedName != null) {
                sharedFields.putIfAbsent(sharedName, field);
            }

            fields.put(definition.getKey(), field);
        }

        return new SettingsForm(fields);
    }

    /**
     * Returns the value a field starts with when nothing is saved: the setting's default, and {@code false} for a
     * {@code boolean} setting without a default (an unchecked box).
     *
     * @param definition the setting
     * @return the value in the settings value file's form, or {@code null} for an empty field
     */
    private static @Nullable Object getDefaultValue(@NotNull SettingDefinition definition) {
        Object defaultValue = definition.getDefaultValue();

        if (defaultValue == null) {
            return definition.getType() == SettingType.BOOLEAN ? Boolean.FALSE : null;
        }

        return definition.getType().toJson(defaultValue);
    }


    private final @NotNull Map<@NotNull String, @NotNull FormField> fieldsByKey;
    private final @NotNull List<@NotNull FormField> fields;

    /**
     * Creates a {@link SettingsForm} instance.
     *
     * @param fieldsByKey the fields by setting key, in schema order
     */
    private SettingsForm(@NotNull Map<@NotNull String, @NotNull FormField> fieldsByKey) {
        this.fieldsByKey = fieldsByKey;
        this.fields      = List.copyOf(fieldsByKey.values());
    }

    /**
     * @return the fields in schema order
     */
    public @NotNull List<@NotNull FormField> getFields() {
        return this.fields;
    }

    /**
     * Finds the field of a setting.
     *
     * @param key the setting key
     * @return the field, or {@code null} when the schema has no such setting
     */
    public @Nullable FormField findField(@NotNull String key) {
        return this.fieldsByKey.get(key);
    }

    /**
     * @return the number of fields whose last validation found a problem
     */
    public int getInvalidCount() {
        int count = 0;

        for (FormField field : this.fields) {
            if (!field.isValid()) {
                count++;
            }
        }

        return count;
    }

    /**
     * @return {@code true} if every field is valid, so the mod may start, {@code false} otherwise
     */
    public boolean isValid() {
        return this.getInvalidCount() == 0;
    }

    /**
     * Validates every field again, for example after files changed on disk; {@link #isValid()} tells the result.
     */
    public void validate() {
        for (FormField field : this.fields) {
            field.validate();
        }
    }

    /**
     * Sets every field of the mod's own settings back to its setting's default value; the shared settings, which other mods use too, stay.
     */
    public void resetToDefaults() {
        for (FormField field : this.fields) {
            if (field.getDefinition().getSharedName() == null) {
                field.setInput(getDefaultValue(field.getDefinition()));
            }
        }
    }

    /**
     * Returns the values as the settings are saved: every field that has a value, by setting key, in schema order.
     *
     * @return the JSON values by key
     */
    public @NotNull Map<@NotNull String, @NotNull Object> toSavedValues() {
        Map<String, Object> savedValues = new LinkedHashMap<>();

        for (Map.Entry<String, FormField> entry : this.fieldsByKey.entrySet()) {
            Object value = entry.getValue().getValue();

            if (value != null) {
                savedValues.put(entry.getKey(), value);
            }
        }

        return savedValues;
    }

    /**
     * Saves the values: the mod's own settings to its settings value file and the shared settings to the shared settings value file.
     *
     * @param store the mod's setting value store
     * @throws IOException            if a file cannot be read or written
     * @throws SettingValuesException if the existing shared settings value file is not valid
     */
    public void save(@NotNull SettingValueStore store) throws IOException, SettingValuesException {
        store.write(this.toSavedValues());
    }
}
