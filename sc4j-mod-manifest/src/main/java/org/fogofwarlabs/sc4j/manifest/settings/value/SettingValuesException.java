package org.fogofwarlabs.sc4j.manifest.settings.value;

import org.fogofwarlabs.sc4j.manifest.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

/**
 * Signals that saved setting values cannot be used: the settings value file is broken, or a value is missing or fails
 * its setting's checks. The problems say which keys and why.
 */
public final class SettingValuesException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient @NotNull ValidationResult result;

    /**
     * Creates a {@link SettingValuesException} instance.
     *
     * @param result the problems, at least one
     */
    public SettingValuesException(@NotNull ValidationResult result) {
        super(result.describe());

        this.result = result;
    }

    /**
     * @return the problems
     */
    public @NotNull ValidationResult getResult() {
        return this.result;
    }
}
