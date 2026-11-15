package org.fogofwarlabs.sc4j.manifest.settings;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.manifest.text.LocalizedText;
import org.fogofwarlabs.sc4j.manifest.validation.ProblemCode;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines an {@code integer} setting: the value must lie within the optional {@code min} and {@code max}, both inclusive.
 */
public final class IntegerSetting extends SettingDefinition {
    private final @Nullable Integer minimum;
    private final @Nullable Integer maximum;

    /**
     * Creates a {@link IntegerSetting} instance.
     *
     * @param key          the key of the value in the settings value file
     * @param label        the label of the launcher's form field
     * @param description  the longer description, or {@code null}
     * @param required     whether a value (or a default) must exist before the mod runs
     * @param defaultValue the value used when none is saved, or {@code null}
     * @param message      the localized message the launcher shows when a check fails, or {@code null} for the generic one
     * @param minimum      the smallest allowed value, or {@code null} for no lower bound
     * @param maximum      the largest allowed value, or {@code null} for no upper bound
     * @throws IllegalArgumentException if the minimum is above the maximum
     */
    public IntegerSetting(@NotNull String key, @NotNull LocalizedText label, @Nullable LocalizedText description,
                          boolean required, @Nullable Integer defaultValue, @Nullable LocalizedText message,
                          @Nullable Integer minimum, @Nullable Integer maximum) {
        super(key, label, description, required, defaultValue, message);

        if (minimum != null && maximum != null && minimum > maximum) {
            throw Exceptions.newIllegalArgumentException("The integer setting {0} has min {1} above max {2}.", key, minimum, maximum);
        }

        this.minimum = minimum;
        this.maximum = maximum;
    }

    /**
     * @return the type of the setting
     */
    @Override
    public @NotNull SettingType getType() {
        return SettingType.INTEGER;
    }

    /**
     * @return the smallest allowed value ({@code min}), or {@code null} for no lower bound
     */
    public @Nullable Integer getMinimum() {
        return this.minimum;
    }

    /**
     * @return the largest allowed value ({@code max}), or {@code null} for no upper bound
     */
    public @Nullable Integer getMaximum() {
        return this.maximum;
    }

    /**
     * Runs the type's declarative checks on a value.
     *
     * @param value    the value, of the type's Java type
     * @param problems receives a problem for each failed check, with this setting's {@link #getMessage() message}
     */
    @Override
    void check(@NotNull Object value, @NotNull ValidationResult.Builder problems) {
        int number = (Integer) value;

        if (this.minimum != null && number < this.minimum) {
            problems.addCheckFailure(ProblemCode.VALUE_RANGE, this.getKey(), this.getMessage(), "{0} is below the minimum {1}.", number, this.minimum);
        } else if (this.maximum != null && number > this.maximum) {
            problems.addCheckFailure(ProblemCode.VALUE_RANGE, this.getKey(), this.getMessage(), "{0} is above the maximum {1}.", number, this.maximum);
        }
    }
}
