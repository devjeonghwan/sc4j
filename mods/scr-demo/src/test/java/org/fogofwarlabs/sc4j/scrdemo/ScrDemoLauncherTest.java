package org.fogofwarlabs.sc4j.scrdemo;

import org.fogofwarlabs.sc4j.launcher.catalog.ModCatalog;
import org.fogofwarlabs.sc4j.launcher.catalog.ModEntry;
import org.fogofwarlabs.sc4j.launcher.catalog.ModStatus;
import org.fogofwarlabs.sc4j.launcher.catalog.SettingsIssue;
import org.fogofwarlabs.sc4j.launcher.form.FormField;
import org.fogofwarlabs.sc4j.launcher.form.SettingsForm;
import org.fogofwarlabs.sc4j.launcher.launch.RuntimeCommandLine;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.fogofwarlabs.sc4j.manifest.settings.InstallationRecords;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValueStore;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValuesException;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValuesFile;
import org.fogofwarlabs.sc4j.manifest.test.FakeInstallationRecords;
import org.fogofwarlabs.sc4j.manifest.validation.ProblemCode;
import org.fogofwarlabs.sc4j.scr.ScrMod;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the launcher over an install whose {@code mods/} folder holds the SC:R mod jar and the demo's jar: both are listed with their own
 * ids and names and their own settings files (the text language), and both use the one shared StarCraft: Remastered folder of the install,
 * which the first scan finds where the computer records the game and which the user can change for both at once.
 */
final class ScrDemoLauncherTest {
    private static final @NotNull String SCR_JAR = "scr.jar";
    private static final @NotNull String DEMO_JAR = "scr-demo.jar";
    private static final @NotNull String SCR_ID = "scr";
    private static final @NotNull String DEMO_ID = "scr-demo";
    private static final @NotNull String SHARED_GAME_FOLDER = "starcraft-remastered";
    private static final @NotNull String UNINSTALL_ENTRY = "StarCraft";
    private static final @NotNull String ACCOUNT_ID = "0123456789abcdef";
    private static final @NotNull String LOCALE_KEY = "locale";
    private static final @NotNull String ENGLISH_LOCALE = "enUS";
    private static final @NotNull String KOREAN_LOCALE = "koKR";
    private static final @NotNull String NAME_LANGUAGE = "en";
    private static final @NotNull List<@NotNull String> INSTALLATION_FILES = List.of(".build.info", "x86_64/StarCraft.exe");

    private static @NotNull Path createInstallation(@NotNull Path folder) throws IOException {
        for (String file : INSTALLATION_FILES) {
            Path path = folder.resolve(file);

            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }

        return folder.toAbsolutePath().normalize();
    }

    private static @NotNull Map<@NotNull String, @NotNull Object> readSharedValues(@NotNull InstallLayout install) throws IOException,
                                                                                                                          SettingValuesException {
        return new SettingValuesFile(install.getSharedSettingsFile()).read();
    }

    private static @NotNull ModEntry requireEntry(@NotNull ModCatalog catalog, @NotNull String jarName) {
        ModEntry entry = catalog.findByJarName(jarName);

        Assertions.assertNotNull(entry, () -> "the launcher does not list " + jarName);

        return entry;
    }

    private static @NotNull FormField requireField(@NotNull SettingsForm form, @NotNull String key) {
        FormField field = form.findField(key);

        Assertions.assertNotNull(field, () -> "the form has no field " + key);

        return field;
    }

    private static void assertMissingInstallation(@NotNull ModEntry entry) {
        Assertions.assertEquals(ModStatus.WARNING, entry.getStatus(), entry::getJarName);
        Assertions.assertEquals(1, entry.getIssues().size(), entry::getJarName);

        SettingsIssue issue = (SettingsIssue) entry.getIssues().getFirst();

        Assertions.assertEquals(ProblemCode.VALUE_REQUIRED, issue.getProblem().getCode());
        Assertions.assertEquals(ScrMod.GAME_FOLDER_SETTING, issue.getProblem().getSubject());
    }

    private static void assertCommandLine(@NotNull InstallLayout install, @NotNull String modId, @NotNull Path jar) {
        Path account = install.getBeaconLinkDirectory().resolve("accounts").resolve(ACCOUNT_ID);
        RuntimeCommandLine commandLine = RuntimeCommandLine.create(install, modId, jar, account, RuntimeCommandLine.isWindows());
        List<String> arguments = commandLine.getArguments();

        Assertions.assertEquals(install.getSettingsFile(modId), commandLine.getSettingsFile());
        Assertions.assertEquals(install.getLogDirectory(modId), commandLine.getLogDirectory());
        Assertions.assertTrue(arguments.contains(jar.toAbsolutePath().normalize().toString()), () -> modId + ": " + arguments);
        Assertions.assertTrue(arguments.contains(install.getSettingsFile(modId).toString()), () -> modId + ": " + arguments);
        Assertions.assertTrue(arguments.contains(install.getStorageDirectory(modId).toString()), () -> modId + ": " + arguments);
        Assertions.assertTrue(arguments.contains(install.getLogDirectory(modId).toString()), () -> modId + ": " + arguments);
        Assertions.assertTrue(arguments.contains(account.toString()), () -> modId + ": " + arguments);
    }


    @Test
    void the_launcher_lists_both_mods_with_one_game_folder_and_separate_settings(@TempDir @NotNull Path directory) throws IOException,
                                                                                                                          SettingValuesException {
        InstallLayout install = InstallLayout.of(directory.resolve("install"));
        Path recorded = createInstallation(directory.resolve("recorded"));
        Path moved = createInstallation(directory.resolve("moved"));
        InstallationRecords records = new FakeInstallationRecords(Map.of(UNINSTALL_ENTRY, List.of(recorded)), List.of());

        Files.createDirectories(install.getModsDirectory());
        Files.copy(ScrDemoTestResources.getScrModJar(), install.getModsDirectory().resolve(SCR_JAR));
        Files.copy(ScrDemoTestResources.getModJar(), install.getModsDirectory().resolve(DEMO_JAR));

        // Nothing recorded and nothing saved: both mods ask for the game folder.
        ModCatalog unknown = ModCatalog.scan(install, InstallationRecords.NONE);

        assertMissingInstallation(requireEntry(unknown, SCR_JAR));
        assertMissingInstallation(requireEntry(unknown, DEMO_JAR));
        Assertions.assertFalse(Files.exists(install.getSharedSettingsFile()));

        // The first scan with the game recorded finds and saves its folder once, for both mods.
        ModCatalog catalog = ModCatalog.scan(install, records);
        ModManifest scrManifest = requireEntry(catalog, SCR_JAR).getManifest();
        ModManifest demoManifest = requireEntry(catalog, DEMO_JAR).getManifest();

        Assertions.assertEquals(List.of(DEMO_JAR, SCR_JAR), catalog.getEntries().stream().map(ModEntry::getJarName).toList());
        Assertions.assertEquals(2, catalog.countStatus(ModStatus.READY));
        Assertions.assertNotNull(scrManifest);
        Assertions.assertNotNull(demoManifest);
        Assertions.assertEquals(SCR_ID, scrManifest.getId());
        Assertions.assertEquals(DEMO_ID, demoManifest.getId());
        Assertions.assertNotEquals(scrManifest.getName().get(NAME_LANGUAGE), demoManifest.getName().get(NAME_LANGUAGE));
        Assertions.assertEquals(Map.of(SHARED_GAME_FOLDER, recorded.toString()), readSharedValues(install));

        // The forms share one folder field: a change in one form is the folder of both; each form keeps its own language.
        SettingValueStore scrStore = SettingValueStore.of(install, scrManifest);
        SettingValueStore demoStore = SettingValueStore.of(install, demoManifest);
        Map<String, FormField> sharedFields = new HashMap<>();
        SettingsForm scrForm = SettingsForm.create(scrManifest.getSettings(), scrStore.read(), sharedFields);
        SettingsForm demoForm = SettingsForm.create(demoManifest.getSettings(), demoStore.read(), sharedFields);
        FormField folder = requireField(scrForm, ScrMod.GAME_FOLDER_SETTING);

        Assertions.assertEquals(recorded.toString(), folder.getValue());
        Assertions.assertTrue(folder.setInput(moved.toString()));
        Assertions.assertSame(folder, requireField(demoForm, ScrMod.GAME_FOLDER_SETTING));
        Assertions.assertTrue(requireField(scrForm, LOCALE_KEY).setInput(ENGLISH_LOCALE));
        Assertions.assertTrue(requireField(demoForm, LOCALE_KEY).setInput(KOREAN_LOCALE));

        scrForm.save(scrStore);
        demoForm.save(demoStore);

        Assertions.assertEquals(Map.of(SHARED_GAME_FOLDER, moved.toString()), readSharedValues(install));
        Assertions.assertEquals(Map.of(LOCALE_KEY, ENGLISH_LOCALE), new SettingValuesFile(install.getSettingsFile(SCR_ID)).read());
        Assertions.assertEquals(Map.of(LOCALE_KEY, KOREAN_LOCALE), new SettingValuesFile(install.getSettingsFile(DEMO_ID)).read());

        // A saved folder wins over the records.
        Assertions.assertEquals(2, ModCatalog.scan(install, records).countStatus(ModStatus.READY));
        Assertions.assertEquals(Map.of(SHARED_GAME_FOLDER, moved.toString()), readSharedValues(install));

        // Each mod starts with its own jar, settings file, storage folder and log folder.
        assertCommandLine(install, SCR_ID, install.getModsDirectory().resolve(SCR_JAR));
        assertCommandLine(install, DEMO_ID, install.getModsDirectory().resolve(DEMO_JAR));
    }
}
