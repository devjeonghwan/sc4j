package org.fogofwarlabs.sc4j.launcher.form;

import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.manifest.settings.SettingDefinition;
import org.fogofwarlabs.sc4j.manifest.settings.SettingType;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationProblem;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holds the value of one generated form field and its validation result. The value is kept in the form the settings value
 * file stores (a path or other text, {@code true}/{@code false}, a whole number), so the manifest module's declarative
 * checks ({@link SettingDefinition#validate}) validate exactly what will be saved.
 * <p>
 * A field without a value uses the setting's default, as the runtime does.
 */
public final class FormField {
    private final @NotNull SettingDefinition definition;

    private @Nullable Object value;
    private @NotNull ValidationResult result;

    /**
     * Creates a {@link FormField} instance and validates its first value.
     *
     * @param definition the setting the field edits
     * @param value      the first value, in the settings value file's form, or {@code null} for none
     */
    FormField(@NotNull SettingDefinition definition, @Nullable Object value) {
        this.definition = definition;

        this.value  = value;
        this.result = this.check();
    }

    /**
     * @return the setting the field edits
     */
    public @NotNull SettingDefinition getDefinition() {
        return this.definition;
    }

    /**
     * @return the current value in the settings value file's form, or {@code null} when the field is empty
     */
    public @Nullable Object getValue() {
        return this.value;
    }

    /**
     * Returns the text a text control shows for the current value.
     *
     * @return the value as text, empty when the field has no value
     */
    public @NotNull String getText() {
        return this.value == null ? "" : String.valueOf(this.value);
    }

    /**
     * @return the result of the last validation
     */
    public @NotNull ValidationResult getResult() {
        return this.result;
    }

    /**
     * @return {@code true} if the last validation found no problem, {@code false} otherwise
     */
    public boolean isValid() {
        return this.result.isValid();
    }

    /**
     * Sets the value a control entered and validates it. Blank text means no value; the text of an {@code integer} setting
     * becomes a whole number when it is one (otherwise the text fails the type check).
     *
     * @param input the entered text, a {@link Boolean}, a {@link Long}, or {@code null} for no value
     * @return {@code true} if the new value is valid, {@code false} otherwise
     */
    public boolean setInput(@Nullable Object input) {
        this.value  = this.normalize(input);
        this.result = this.check();

        return this.result.isValid();
    }

    /**
     * Validates the current value again, for example after files changed on disk.
     */
    public void validate() {
        this.result = this.check();
    }

    /**
     * Describes the first problem of the last validation for the user: the setting's own localized message when the manifest
     * has one, the launcher's generic message otherwise.
     *
     * @param texts the launcher texts of the display language
     * @return the localized message, or {@code null} when the value is valid
     */
    public @Nullable String describeProblem(@NotNull LauncherTexts texts) {
        List<ValidationProblem> problems = this.result.getProblems();

        return problems.isEmpty() ? null : texts.describeSettingProblem(problems.getFirst(), this.definition);
    }

    private @Nullable Object normalize(@Nullable Object input) {
        if (!(input instanceof String text)) {
            return input;
        }

        String stripped = text.strip();

        if (stripped.isEmpty()) {
            return null;
        }

        if (this.definition.getType() == SettingType.INTEGER) {
            try {
                return Long.parseLong(stripped);
            } catch (NumberFormatException exception) {
                return text;
            }
        }

        return this.definition.getType() == SettingType.STRING ? text : stripped;
    }

    private @NotNull ValidationResult check() {
        ValidationResult.Builder problems = new ValidationResult.Builder();

        this.definition.validate(this.value, problems);

        return problems.build();
    }
}
