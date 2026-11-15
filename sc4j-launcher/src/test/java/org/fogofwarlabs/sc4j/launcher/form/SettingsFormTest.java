package org.fogofwarlabs.sc4j.launcher.form;

import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.i18n.UiLanguage;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.settings.SettingType;
import org.fogofwarlabs.sc4j.manifest.settings.SettingsSchema;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValueStore;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValues;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValuesException;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValuesFile;
import org.fogofwarlabs.sc4j.manifest.validation.ProblemCode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the settings form generated from a settings schema: its fields, their validation and messages, saving, and the field of a shared
 * setting that the forms of two mods share.
 */
final class SettingsFormTest {
    private static final @NotNull String MANIFEST = """
                                                    {
                                                      "format": 1,
                                                      "id": "example",
                                                      "name": {"en": "Example"},
                                                      "version": "1.0.0",
                                                      "engine": "0.1",
                                                      "entry": "org.example.ExampleMod",
                                                      "settings": [
                                                        {"key": "folder", "type": "directory", "required": true, "label": {"en": "Game folder", "ko": "게임 폴더"},
                                                         "check": {"contains": ["marker.txt"]},
                                                         "message": {"en": "Choose the folder that holds marker.txt.", "ko": "marker.txt가 있는 폴더를 선택하세요."}},
                                                        {"key": "replay", "type": "file", "extensions": ["sc4r"], "label": {"en": "Replay"}},
                                                        {"key": "locale", "type": "choice", "options": ["en-US", "ko-KR"], "default": "ko-KR", "label": {"en": "Text language"}},
                                                        {"key": "windowed", "type": "boolean", "label": {"en": "Start in a window"}},
                                                        {"key": "frames", "type": "integer", "min": 30, "max": 240, "default": 60, "label": {"en": "Frame rate", "ko": "프레임"}},
                                                        {"key": "player", "type": "string", "required": true, "pattern": "[A-Za-z]+", "label": {"en": "Player name"}}
                                                      ]
                                                    }
                                                    """;

    /**
     * A mod whose game folder, under a key of its own, is shared under the name {@code example-game}.
     */
    private static final @NotNull String GAME_MANIFEST = """
                                                         {"format": 1, "id": "%s", "name": {"en": "Game mod"}, "version": "1.0.0", "engine": "0.1", "entry": "org.example.GameMod",
                                                          "settings": [{"key": "%s", "type": "directory", "required": true, "label": {"en": "Game folder"}, "check": {"contains": ["marker.txt"]},
                                                                        "shared": "example-game"},
                                                                       {"key": "locale", "type": "choice", "options": ["en-US", "ko-KR"], "default": "en-US", "label": {"en": "Text language"}}]}
                                                         """;

    @Test
    void create_should_generate_one_field_per_setting_type_with_the_defaults() throws ManifestException {
        SettingsForm form = SettingsForm.create(readSchema(), Map.of());
        List<SettingType> types = new ArrayList<>();

        for (FormField field : form.getFields()) {
            types.add(field.getDefinition().getType());
        }

        Assertions.assertEquals(List.of(SettingType.values()), types);
        Assertions.assertNull(requireField(form, "folder").getValue());
        Assertions.assertEquals("ko-KR", requireField(form, "locale").getValue());
        Assertions.assertEquals(Boolean.FALSE, requireField(form, "windowed").getValue());
        Assertions.assertEquals(60L, requireField(form, "frames").getValue());
    }

    @Test
    void required_settings_without_a_value_block_the_start() throws ManifestException {
        SettingsForm form = SettingsForm.create(readSchema(), Map.of());
        LauncherTexts english = LauncherTexts.of(UiLanguage.ENGLISH);
        LauncherTexts korean = LauncherTexts.of(UiLanguage.KOREAN);

        Assertions.assertFalse(form.isValid());
        Assertions.assertEquals(2, form.getInvalidCount());
        Assertions.assertEquals(ProblemCode.VALUE_REQUIRED, requireField(form, "folder").getResult().getProblems().getFirst().getCode());
        Assertions.assertEquals("Game folder: required.", requireField(form, "folder").describeProblem(english));
        Assertions.assertEquals("게임 폴더: 필수.", requireField(form, "folder").describeProblem(korean));
        Assertions.assertEquals("Player name: required.", requireField(form, "player").describeProblem(english));
        Assertions.assertFalse(requireField(form, "player").setInput("   "));
    }

    @Test
    void failed_checks_show_the_setting_message_or_the_localized_generic_message(@TempDir @NotNull Path directory) throws ManifestException {
        SettingsForm form = SettingsForm.create(readSchema(), Map.of());
        LauncherTexts english = LauncherTexts.of(UiLanguage.ENGLISH);
        LauncherTexts korean = LauncherTexts.of(UiLanguage.KOREAN);
        FormField folder = requireField(form, "folder");
        FormField frames = requireField(form, "frames");

        Assertions.assertFalse(folder.setInput(directory.toString()));
        Assertions.assertEquals("Choose the folder that holds marker.txt.", folder.describeProblem(english));
        Assertions.assertEquals("marker.txt가 있는 폴더를 선택하세요.", folder.describeProblem(korean));

        Assertions.assertFalse(frames.setInput("fast"));
        Assertions.assertEquals("Frame rate: wrong type.", frames.describeProblem(english));
        Assertions.assertFalse(frames.setInput("500"));
        Assertions.assertEquals("Frame rate: integer 30 .. 240.", frames.describeProblem(english));
        Assertions.assertEquals("프레임: 정수 30 .. 240.", frames.describeProblem(korean));

        Assertions.assertFalse(requireField(form, "player").setInput("Player1"));
        Assertions.assertEquals("Player name: does not match [A-Za-z]+.", requireField(form, "player").describeProblem(english));
        Assertions.assertFalse(requireField(form, "replay").setInput(directory.resolve("missing.sc4r").toString()));
        Assertions.assertEquals("Replay: file not found.", requireField(form, "replay").describeProblem(english));
    }

    @Test
    void save_should_write_values_that_the_runtime_resolves(@TempDir @NotNull Path directory) throws ManifestException,
                                                                                                     IOException,
                                                                                                     SettingValuesException {
        SettingsSchema schema = readSchema();
        SettingsForm form = SettingsForm.create(schema, Map.of());
        SettingValuesFile file = new SettingValuesFile(directory.resolve("userdata").resolve("settings").resolve("example.json"));

        Files.writeString(directory.resolve("marker.txt"), "", StandardCharsets.UTF_8);
        Assertions.assertTrue(requireField(form, "folder").setInput(directory.toString()));
        Assertions.assertTrue(requireField(form, "frames").setInput(" 144 "));
        Assertions.assertTrue(requireField(form, "player").setInput("Commander"));
        Assertions.assertTrue(form.isValid());

        form.save(new SettingValueStore(schema, file, new SettingValuesFile(directory.resolve("userdata").resolve("shared-settings.json"))));

        Map<String, Object> saved = file.read();
        SettingValues values = SettingValues.resolve(schema, saved);

        Assertions.assertEquals(directory, values.getDirectory("folder"));
        Assertions.assertEquals(144, values.getInteger("frames"));
        Assertions.assertEquals("Commander", values.getString("player"));
        Assertions.assertFalse(values.getBoolean("windowed"));
        Assertions.assertFalse(saved.containsKey("replay"));
        Assertions.assertTrue(SettingsForm.create(schema, saved).isValid());
        Assertions.assertFalse(Files.exists(directory.resolve("userdata").resolve("shared-settings.json")));
    }

    @Test
    void a_shared_field_is_one_field_of_both_forms_and_saved_once(@TempDir @NotNull Path directory) throws ManifestException,
                                                                                                           IOException,
                                                                                                           SettingValuesException {
        ModManifest first = ModManifestReader.parse(GAME_MANIFEST.formatted("first", "gameFolder").getBytes(StandardCharsets.UTF_8));
        ModManifest second = ModManifestReader.parse(GAME_MANIFEST.formatted("second", "folder").getBytes(StandardCharsets.UTF_8));
        SettingValuesFile sharedFile = new SettingValuesFile(directory.resolve("shared-settings.json"));
        SettingValuesFile firstFile = new SettingValuesFile(directory.resolve("first.json"));
        SettingValueStore firstStore = new SettingValueStore(first.getSettings(), firstFile, sharedFile);
        SettingValueStore secondStore = new SettingValueStore(second.getSettings(),
                                                              new SettingValuesFile(directory.resolve("second.json")),
                                                              sharedFile);
        Map<String, FormField> sharedFields = new HashMap<>();
        SettingsForm firstForm = SettingsForm.create(first.getSettings(), firstStore.read(), sharedFields);
        SettingsForm secondForm = SettingsForm.create(second.getSettings(), secondStore.read(), sharedFields);
        FormField folder = requireField(firstForm, "gameFolder");

        Assertions.assertSame(folder, firstForm.getFields().getFirst());
        Assertions.assertSame(folder, requireField(secondForm, "folder"));
        Assertions.assertEquals(Map.of("example-game", folder), sharedFields);
        Assertions.assertFalse(firstForm.isValid());
        Assertions.assertFalse(secondForm.isValid());

        Files.writeString(directory.resolve("marker.txt"), "", StandardCharsets.UTF_8);
        Assertions.assertTrue(folder.setInput(directory.toString()));
        Assertions.assertTrue(firstForm.isValid());
        Assertions.assertTrue(secondForm.isValid());

        secondForm.resetToDefaults();
        firstForm.save(firstStore);

        Assertions.assertEquals(directory.toString(), folder.getValue());
        Assertions.assertEquals(Map.of("locale", "en-US"), firstFile.read());
        Assertions.assertEquals(Map.of("example-game", directory.toString()), sharedFile.read());
        Assertions.assertTrue(SettingsForm.create(second.getSettings(), secondStore.read()).isValid());
    }

    @Test
    void reset_to_defaults_should_restore_the_default_values() throws ManifestException {
        SettingsForm form = SettingsForm.create(readSchema(), Map.of("locale", "en-US", "frames", 200L));

        form.resetToDefaults();

        Assertions.assertEquals("ko-KR", requireField(form, "locale").getValue());
        Assertions.assertEquals(60L, requireField(form, "frames").getValue());
    }

    private static @NotNull SettingsSchema readSchema() throws ManifestException {
        return ModManifestReader.parse(MANIFEST.getBytes(StandardCharsets.UTF_8)).getSettings();
    }

    private static @NotNull FormField requireField(@NotNull SettingsForm form, @NotNull String key) {
        FormField field = form.findField(key);

        Assertions.assertNotNull(field, key);

        return field;
    }
}
