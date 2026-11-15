package org.fogofwarlabs.sc4j.manifest.settings;

import org.fogofwarlabs.sc4j.manifest.text.LocalizedText;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines a {@code boolean} setting; it has no check beyond its type.
 */
public final class BooleanSetting extends SettingDefinition {
    /**
     * Creates a {@link BooleanSetting} instance.
     *
     * @param key          the key of the value in the settings value file
     * @param label        the label of the launcher's form field
     * @param description  the longer description, or {@code null}
     * @param required     whether a value (or a default) must exist before the mod runs
     * @param defaultValue the value used when none is saved, or {@code null}
     * @param message      the localized message the launcher shows when a check fails, or {@code null} for the generic one
     */
    public BooleanSetting(@NotNull String key, @NotNull LocalizedText label, @Nullable LocalizedText description,
                          boolean required, @Nullable Boolean defaultValue, @Nullable LocalizedText message) {
        super(key, label, description, required, defaultValue, message);
    }

    /**
     * @return the type of the setting
     */
    @Override
    public @NotNull SettingType getType() {
        return SettingType.BOOLEAN;
    }

    /**
     * Runs the type's declarative checks on a value. A boolean value has none.
     *
     * @param value    the value, of the type's Java type
     * @param problems receives a problem for each failed check, with this setting's {@link #getMessage() message}
     */
    @Override
    void check(@NotNull Object value, @NotNull ValidationResult.Builder problems) {
        // A boolean of the right type is always valid.
    }
}
