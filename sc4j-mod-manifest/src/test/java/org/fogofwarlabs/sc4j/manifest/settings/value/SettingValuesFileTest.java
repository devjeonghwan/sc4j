package org.fogofwarlabs.sc4j.manifest.settings.value;

import org.fogofwarlabs.sc4j.base.json.JsonReader;
import org.fogofwarlabs.sc4j.manifest.settings.SettingsSchema;
import org.fogofwarlabs.sc4j.manifest.validation.ProblemCode;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the settings value file round trip and the typed values resolved from it.
 */
final class SettingValuesFileTest {
    private static final @NotNull String SCHEMA = """
                                                  [
                                                    {"key": "folder", "type": "directory", "required": true, "label": {"en": "Folder"}},
                                                    {"key": "locale", "type": "choice", "options": ["en-US", "ko-KR"], "default": "ko-KR", "label": {"en": "Language"}},
                                                    {"key": "music", "type": "boolean", "label": {"en": "Music"}},
                                                    {"key": "speed", "type": "integer", "min": 0, "max": 6, "label": {"en": "Speed"}},
                                                    {"key": "name", "type": "string", "label": {"en": "Name"}}
                                                  ]
                                                  """;

    private static @NotNull SettingsSchema readSchema() throws IOException {
        ValidationResult.Builder problems = new ValidationResult.Builder();
        SettingsSchema schema = SettingsSchema.read((List<?>) JsonReader.parse(SCHEMA.getBytes(StandardCharsets.UTF_8)), "settings", problems);

        Assertions.assertTrue(problems.isValid(), problems.build().describe());

        return schema;
    }

    @Test
    void write_and_read_should_round_trip_typed_values(@TempDir @NotNull Path directory) throws IOException, SettingValuesException {
        SettingsSchema schema = readSchema();
        SettingValuesFile file = new SettingValuesFile(directory.resolve("settings").resolve("example.json"));
        Map<String, Object> saved = new LinkedHashMap<>();

        saved.put("folder", directory.toString());
        saved.put("music", false);
        saved.put("speed", 5L);
        saved.put("name", "플레이어");

        file.write(saved);

        SettingValues values = SettingValues.resolve(schema, file.read());

        Assertions.assertEquals(directory, values.getDirectory("folder"));
        Assertions.assertEquals("ko-KR", values.getChoice("locale"));
        Assertions.assertFalse(values.getBoolean("music"));
        Assertions.assertEquals(5, values.getInteger("speed"));
        Assertions.assertEquals("플레이어", values.getString("name"));

        file.write(values.toSavedValues());

        Assertions.assertEquals(values.toSavedValues(), SettingValues.resolve(schema, file.read()).toSavedValues());
        Assertions.assertTrue(Files.readString(file.getPath(), StandardCharsets.UTF_8).startsWith("{\n  \"format\": 1,"));
    }

    @Test
    void resolve_should_reject_missing_required_values_and_typed_access_errors(@TempDir @NotNull Path directory) throws IOException, SettingValuesException {
        SettingsSchema schema = readSchema();
        SettingValuesFile missing = new SettingValuesFile(directory.resolve("missing.json"));

        Assertions.assertEquals(Map.of(), missing.read());

        SettingValuesException exception = Assertions.assertThrows(SettingValuesException.class, () -> SettingValues.resolve(schema, missing.read()));

        Assertions.assertTrue(exception.getResult().hasProblem(ProblemCode.VALUE_REQUIRED));

        SettingValues values = SettingValues.resolve(schema, Map.of("folder", directory.toString()));

        Assertions.assertFalse(values.hasValue("music"));
        Assertions.assertThrows(IllegalStateException.class, () -> values.getBoolean("music"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> values.getString("locale"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> values.getString("unknown"));
    }

    @Test
    void read_should_reject_a_broken_file(@TempDir @NotNull Path directory) throws IOException {
        Path broken = Files.writeString(directory.resolve("broken.json"), "{\"format\": 1, \"values\": ");
        Path flat = Files.writeString(directory.resolve("flat.json"), "{\"folder\": \"C:/Games\"}");
        Path future = Files.writeString(directory.resolve("future.json"), "{\"format\": 2, \"values\": {}}");

        for (Path path : List.of(broken, flat, future)) {
            ValidationResult result = Assertions.assertThrows(SettingValuesException.class, () -> new SettingValuesFile(path).read()).getResult();

            Assertions.assertTrue(result.hasProblem(ProblemCode.VALUES_SYNTAX), path.toString());
        }
    }
}
