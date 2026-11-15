package org.fogofwarlabs.sc4j.manifest.settings.value;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.io.AtomicFileUtils;
import org.fogofwarlabs.sc4j.base.json.JsonReader;
import org.fogofwarlabs.sc4j.base.json.JsonWriterUtils;
import org.fogofwarlabs.sc4j.manifest.validation.ProblemCode;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a settings value file, a mod's {@code userdata/settings/<modId>.json} or the shared
 * {@code userdata/shared-settings.json}: the launcher writes the values of the form, the runtime reads them before it validates
 * them against the mod's schema.
 * <p>
 * The file is a JSON object {@code {"format": 1, "values": {"<key>": <value>, ...}}}; directories and files are path
 * strings, choices and strings are strings, booleans are {@code true} or {@code false}, integers are numbers.
 */
public final class SettingValuesFile {
    /**
     * The settings value file format this release reads and writes.
     */
    public static final int FORMAT = 1;

    private static final @NotNull String FORMAT_FIELD = "format";
    private static final @NotNull String VALUES_FIELD = "values";

    private static @NotNull SettingValuesException createSyntaxException(@NotNull Path file, @NotNull String reason) {
        return new SettingValuesException(new ValidationResult.Builder().add(ProblemCode.VALUES_SYNTAX, file.toString(),
                                                                             "Invalid settings value file: {0}", reason)
                                                                        .build());
    }


    private final @NotNull Path path;

    /**
     * Creates a {@link SettingValuesFile} instance.
     *
     * @param path the file path, such as {@code <install>/userdata/settings/my-mod.json}
     */
    public SettingValuesFile(@NotNull Path path) {
        this.path = path;
    }

    /**
     * @return the file path
     */
    public @NotNull Path getPath() {
        return this.path;
    }

    /**
     * Reads the saved values. A missing file holds no values.
     *
     * @return the parsed values by key, empty when the file does not exist
     * @throws IOException            if the file exists but cannot be read
     * @throws SettingValuesException if the file is not valid JSON or not in the settings value format
     */
    public @NotNull Map<@NotNull String, @NotNull Object> read() throws IOException, SettingValuesException {
        byte[] contents;

        try {
            contents = Files.readAllBytes(this.path);
        } catch (NoSuchFileException exception) {
            return Map.of();
        }

        Object parsed;

        try {
            parsed = JsonReader.parse(contents);
        } catch (IOException exception) {
            throw createSyntaxException(this.path, exception.getMessage());
        }

        if (!(parsed instanceof Map<?, ?> object) || !(object.get(FORMAT_FIELD) instanceof Long format) ||
            !(object.get(VALUES_FIELD) instanceof Map<?, ?> values)) {
            throw createSyntaxException(this.path, Exceptions.format("expected {\"format\": {0}, \"values\": {...}}", FORMAT));
        }

        if (format != FORMAT) {
            throw createSyntaxException(this.path, Exceptions.format("unsupported format {0}", format));
        }

        Map<String, Object> savedValues = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                savedValues.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        return savedValues;
    }

    /**
     * Writes saved values, replacing the file atomically where the file system allows; the folder is created when missing.
     *
     * @param savedValues the JSON values by key ({@link SettingValues#toSavedValues()})
     * @throws IOException if the file cannot be written
     */
    public void write(@NotNull Map<@NotNull String, @NotNull Object> savedValues) throws IOException {
        Map<String, Object> object = new LinkedHashMap<>();

        object.put(FORMAT_FIELD, (long) FORMAT);
        object.put(VALUES_FIELD, new LinkedHashMap<>(savedValues));

        AtomicFileUtils.write(this.path, JsonWriterUtils.write(object).getBytes(StandardCharsets.UTF_8));
    }
}
