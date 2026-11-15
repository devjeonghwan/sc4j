package org.fogofwarlabs.sc4j.manifest.settings;

import org.fogofwarlabs.sc4j.manifest.text.LocalizedText;
import org.fogofwarlabs.sc4j.manifest.validation.ProblemCode;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Defines a {@code file} setting: the value must name an existing regular file whose extension, when
 * {@code extensions} lists any, is one of them (compared without case).
 */
public final class FileSetting extends SettingDefinition {
    private static final char EXTENSION_SEPARATOR = '.';

    /**
     * Normalizes a manifest extension: without a leading dot, in lower case.
     *
     * @param extension the extension as written, such as {@code .sc4r} or {@code SC4R}
     * @return the normalized extension, such as {@code sc4r}
     */
    public static @NotNull String normalizeExtension(@NotNull String extension) {
        String name = !extension.isEmpty() && extension.charAt(0) == EXTENSION_SEPARATOR ? extension.substring(1) : extension;

        return name.toLowerCase(Locale.ROOT);
    }


    private final @NotNull List<@NotNull String> extensions;

    /**
     * Creates a {@link FileSetting} instance.
     *
     * @param key          the key of the value in the settings value file
     * @param label        the label of the launcher's form field
     * @param description  the longer description, or {@code null}
     * @param required     whether a value (or a default) must exist before the mod runs
     * @param defaultValue the file used when none is saved, or {@code null}
     * @param message      the localized message the launcher shows when a check fails, or {@code null} for the generic one
     * @param extensions   the allowed extensions, with or without the leading dot; empty allows any file
     */
    public FileSetting(@NotNull String key, @NotNull LocalizedText label, @Nullable LocalizedText description,
                       boolean required, @Nullable Path defaultValue, @Nullable LocalizedText message,
                       @NotNull List<@NotNull String> extensions) {
        super(key, label, description, required, defaultValue, message);

        List<String> normalized = new ArrayList<>(extensions.size());

        for (String extension : extensions) {
            normalized.add(normalizeExtension(extension));
        }

        this.extensions = List.copyOf(normalized);
    }

    /**
     * @return the type of the setting
     */
    @Override
    public @NotNull SettingType getType() {
        return SettingType.FILE;
    }

    /**
     * @return the allowed extensions without the leading dot, in lower case; empty when any file is allowed
     */
    public @NotNull List<@NotNull String> getExtensions() {
        return this.extensions;
    }

    /**
     * Runs the type's declarative checks on a value.
     *
     * @param value    the value, of the type's Java type
     * @param problems receives a problem for each failed check, with this setting's {@link #getMessage() message}
     */
    @Override
    void check(@NotNull Object value, @NotNull ValidationResult.Builder problems) {
        Path file = (Path) value;

        if (!Files.isRegularFile(file)) {
            problems.addCheckFailure(ProblemCode.VALUE_FILE, this.getKey(), this.getMessage(), "Not a file: {0}", file);

            return;
        }

        Path fileName = file.getFileName();
        String name = fileName == null ? "" : fileName.toString();
        int separator = name.lastIndexOf(EXTENSION_SEPARATOR);
        String extension = separator < 0 ? "" : name.substring(separator + 1).toLowerCase(Locale.ROOT);

        if (!this.extensions.isEmpty() && !this.extensions.contains(extension)) {
            problems.addCheckFailure(ProblemCode.VALUE_EXTENSION, this.getKey(), this.getMessage(),
                                     "The file {0} does not have one of the extensions {1}.", file, this.extensions);
        }
    }
}
