package org.fogofwarlabs.sc4j.manifest.settings;

import org.fogofwarlabs.sc4j.manifest.text.LocalizedText;
import org.fogofwarlabs.sc4j.manifest.validation.ProblemCode;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Defines a {@code string} setting: the whole value must match the optional regular expression {@code pattern}.
 */
public final class StringSetting extends SettingDefinition {
    private final @Nullable Pattern pattern;

    /**
     * Creates a {@link StringSetting} instance.
     *
     * @param key          the key of the value in the settings value file
     * @param label        the label of the launcher's form field
     * @param description  the longer description, or {@code null}
     * @param required     whether a value (or a default) must exist before the mod runs
     * @param defaultValue the value used when none is saved, or {@code null}
     * @param message      the localized message the launcher shows when a check fails, or {@code null} for the generic one
     * @param pattern      the regular expression the whole value must match, or {@code null} for any text
     */
    public StringSetting(@NotNull String key, @NotNull LocalizedText label, @Nullable LocalizedText description,
                         boolean required, @Nullable String defaultValue, @Nullable LocalizedText message,
                         @Nullable Pattern pattern) {
        super(key, label, description, required, defaultValue, message);

        this.pattern = pattern;
    }

    /**
     * @return the type of the setting
     */
    @Override
    public @NotNull SettingType getType() {
        return SettingType.STRING;
    }

    /**
     * @return the regular expression the whole value must match ({@code pattern}), or {@code null} for any text
     */
    public @Nullable Pattern getPattern() {
        return this.pattern;
    }

    /**
     * Runs the type's declarative checks on a value.
     *
     * @param value    the value, of the type's Java type
     * @param problems receives a problem for each failed check, with this setting's {@link #getMessage() message}
     */
    @Override
    void check(@NotNull Object value, @NotNull ValidationResult.Builder problems) {
        if (this.pattern != null && !this.pattern.matcher((String) value).matches()) {
            problems.addCheckFailure(ProblemCode.VALUE_PATTERN, this.getKey(), this.getMessage(),
                                     "{0} does not match the pattern {1}.", value, this.pattern.pattern());
        }
    }
}
