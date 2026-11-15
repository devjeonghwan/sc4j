package org.fogofwarlabs.sc4j.manifest.settings.value;

import org.fogofwarlabs.sc4j.base.json.JsonReader;
import org.fogofwarlabs.sc4j.manifest.settings.InstallationRecords;
import org.fogofwarlabs.sc4j.manifest.settings.SettingsSchema;
import org.fogofwarlabs.sc4j.manifest.test.FakeInstallationRecords;
import org.fogofwarlabs.sc4j.manifest.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Tests where the values of a mod's settings are kept: a shared setting once for every mod that declares its shared name, every other
 * setting in the mod's own settings value file, and a detected directory saved where its value belongs.
 */
final class SettingValueStoreTest {
    /**
     * The settings of a mod: a shared game folder under the mod's own key, a data folder of the mod only, both detectable, and a language.
     */
    private static final @NotNull String SETTINGS = """
                                                    [{"key": "%s", "type": "directory", "required": true, "label": {"en": "Game folder"}, "check": {"contains": ["game.info"]},
                                                      "detect": {"uninstallEntries": ["Example Game"]}, "shared": "example-game"},
                                                     {"key": "dataFolder", "type": "directory", "label": {"en": "Data folder"}, "check": {"contains": ["data.info"]},
                                                      "detect": {"uninstallEntries": ["Example Data"]}},
                                                     {"key": "locale", "type": "choice", "options": ["en-US", "ko-KR"], "default": "en-US", "label": {"en": "Text language"}}]
                                                    """;
    private static final @NotNull String FIRST_FOLDER_KEY = "gameFolder";
    private static final @NotNull String SECOND_FOLDER_KEY = "folder";
    private static final @NotNull String SHARED_NAME = "example-game";

    private static @NotNull SettingsSchema readSchema(@NotNull String folderKey) throws IOException {
        ValidationResult.Builder problems = new ValidationResult.Builder();
        SettingsSchema schema = SettingsSchema.read((List<?>) JsonReader.parse(SETTINGS.formatted(folderKey).getBytes(StandardCharsets.UTF_8)),
                                                    "settings", problems);

        Assertions.assertTrue(problems.isValid(), problems.build().describe());

        return schema;
    }

    private static @NotNull Path createFolder(@NotNull Path directory, @NotNull String marker) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(marker), "info");

        return directory.toAbsolutePath().normalize();
    }

    @Test
    void two_mods_with_the_same_shared_name_read_and_write_one_value(@TempDir @NotNull Path directory) throws IOException, SettingValuesException {
        SettingValuesFile sharedFile = new SettingValuesFile(directory.resolve("shared-settings.json"));
        SettingValuesFile firstFile = new SettingValuesFile(directory.resolve("settings").resolve("first.json"));
        SettingValuesFile secondFile = new SettingValuesFile(directory.resolve("settings").resolve("second.json"));
        SettingValueStore first = new SettingValueStore(readSchema(FIRST_FOLDER_KEY), firstFile, sharedFile);
        SettingValueStore second = new SettingValueStore(readSchema(SECOND_FOLDER_KEY), secondFile, sharedFile);
        Path game = createFolder(directory.resolve("game"), "game.info");
        Path moved = createFolder(directory.resolve("moved"), "game.info");
        Path firstData = createFolder(directory.resolve("first-data"), "data.info");

        first.write(Map.of(FIRST_FOLDER_KEY, game.toString(), "dataFolder", firstData.toString(), "locale", "ko-KR"));

        Assertions.assertEquals(Map.of(SHARED_NAME, game.toString()), sharedFile.read());
        Assertions.assertEquals(Map.of("dataFolder", firstData.toString(), "locale", "ko-KR"), firstFile.read());
        Assertions.assertEquals(Map.of(SECOND_FOLDER_KEY, game.toString()), second.read());

        second.write(Map.of(SECOND_FOLDER_KEY, moved.toString(), "locale", "en-US"));

        Assertions.assertEquals(Map.of(FIRST_FOLDER_KEY, moved.toString(), "dataFolder", firstData.toString(), "locale", "ko-KR"), first.read());
        Assertions.assertEquals(Map.of(SECOND_FOLDER_KEY, moved.toString(), "locale", "en-US"), second.read());
        Assertions.assertEquals(Map.of("locale", "en-US"), secondFile.read());

        first.write(Map.of("locale", "ko-KR"));

        Assertions.assertEquals(Map.of(), sharedFile.read());
        Assertions.assertEquals(Map.of("locale", "en-US"), second.read());
    }

    @Test
    void a_setting_without_shared_keeps_its_value_per_mod(@TempDir @NotNull Path directory) throws IOException, SettingValuesException {
        SettingValuesFile sharedFile = new SettingValuesFile(directory.resolve("shared-settings.json"));
        SettingValueStore first = new SettingValueStore(readSchema(FIRST_FOLDER_KEY), new SettingValuesFile(directory.resolve("first.json")),
                                                        sharedFile);
        SettingValueStore second = new SettingValueStore(readSchema(SECOND_FOLDER_KEY), new SettingValuesFile(directory.resolve("second.json")),
                                                         sharedFile);
        Path firstData = createFolder(directory.resolve("first-data"), "data.info");
        Path secondData = createFolder(directory.resolve("second-data"), "data.info");

        first.write(Map.of("dataFolder", firstData.toString()));
        second.write(Map.of("dataFolder", secondData.toString()));

        Assertions.assertEquals(firstData.toString(), first.read().get("dataFolder"));
        Assertions.assertEquals(secondData.toString(), second.read().get("dataFolder"));
        Assertions.assertEquals(Map.of(), sharedFile.read());
    }

    @Test
    void resolve_should_fill_empty_detectable_directories_from_the_records_and_save_them(@TempDir @NotNull Path directory) throws IOException,
                                                                                                                                  SettingValuesException {
        SettingValuesFile sharedFile = new SettingValuesFile(directory.resolve("userdata").resolve("shared-settings.json"));
        SettingValuesFile firstFile = new SettingValuesFile(directory.resolve("userdata").resolve("settings").resolve("first.json"));
        SettingValueStore first = new SettingValueStore(readSchema(FIRST_FOLDER_KEY), firstFile, sharedFile);
        SettingValueStore second = new SettingValueStore(readSchema(SECOND_FOLDER_KEY), new SettingValuesFile(directory.resolve("second.json")),
                                                         sharedFile);
        Path game = createFolder(directory.resolve("recorded-game"), "game.info");
        Path data = createFolder(directory.resolve("recorded-data"), "data.info");
        Path chosen = createFolder(directory.resolve("chosen"), "game.info");
        InstallationRecords records = new FakeInstallationRecords(Map.of("Example Game", List.of(game), "Example Data", List.of(data)), List.of());

        Assertions.assertEquals(Map.of(), first.resolve(InstallationRecords.NONE));
        Assertions.assertFalse(Files.exists(sharedFile.getPath()), "nothing detected, nothing saved");
        Assertions.assertEquals(Map.of(FIRST_FOLDER_KEY, game.toString(), "dataFolder", data.toString()), first.resolve(records));
        Assertions.assertEquals(Map.of(SHARED_NAME, game.toString()), sharedFile.read());
        Assertions.assertEquals(Map.of("dataFolder", data.toString()), firstFile.read());
        Assertions.assertEquals(Map.of(SECOND_FOLDER_KEY, game.toString()), second.read());

        second.write(Map.of(SECOND_FOLDER_KEY, chosen.toString()));

        Assertions.assertEquals(chosen.toString(), first.resolve(records).get(FIRST_FOLDER_KEY), "a saved value wins over the records");
    }
}
