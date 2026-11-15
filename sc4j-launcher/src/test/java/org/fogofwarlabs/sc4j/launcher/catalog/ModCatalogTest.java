package org.fogofwarlabs.sc4j.launcher.catalog;

import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.fogofwarlabs.sc4j.manifest.settings.InstallationRecords;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValuesFile;
import org.fogofwarlabs.sc4j.manifest.test.TestJarUtils;
import org.fogofwarlabs.sc4j.manifest.validation.ProblemCode;
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
 * Tests the mod list of an install: the status and reasons of valid, broken, unsupported, incomplete and conflicting jars.
 */
final class ModCatalogTest {
    private static final @NotNull String MANIFEST = """
                                                    {
                                                      "format": 1,
                                                      "id": "%s",
                                                      "name": {"en": "Example", "ko": "예제"},
                                                      "version": "1.0.0",
                                                      "engine": "%s",
                                                      "entry": "org.example.ExampleMod",
                                                      "images": {"icon": "meta/icon.png"},
                                                      "settings": [
                                                        {"key": "folder", "type": "directory", "required": true, "label": {"en": "Folder"}}
                                                      ]
                                                    }
                                                    """;

    @Test
    void scan_should_report_a_valid_jar_with_valid_settings_as_ready(@TempDir @NotNull Path directory) throws IOException {
        InstallLayout install = InstallLayout.of(directory);

        writeMod(install, "example.jar", MANIFEST.formatted("example", "0.1"), true);
        new SettingValuesFile(install.getSettingsFile("example")).write(Map.of("folder", directory.toString()));

        ModEntry entry = ModCatalog.scan(install, InstallationRecords.NONE).getEntries().getFirst();

        Assertions.assertEquals(ModStatus.READY, entry.getStatus());
        Assertions.assertEquals(List.of(), entry.getIssues());
        Assertions.assertNotNull(entry.getArchive());
        Assertions.assertEquals("example.jar", entry.getJarName());
    }

    @Test
    void scan_should_report_missing_settings_as_a_warning(@TempDir @NotNull Path directory) throws IOException {
        InstallLayout install = InstallLayout.of(directory);

        writeMod(install, "example.jar", MANIFEST.formatted("example", "0.1"), true);

        ModEntry entry = ModCatalog.scan(install, InstallationRecords.NONE).getEntries().getFirst();

        Assertions.assertEquals(ModStatus.WARNING, entry.getStatus());
        Assertions.assertTrue(entry.getStatus().isStartable());
        Assertions.assertEquals(ProblemCode.VALUE_REQUIRED, ((SettingsIssue) entry.getIssues().getFirst()).getProblem().getCode());
    }

    @Test
    void scan_should_report_a_broken_manifest_as_an_error(@TempDir @NotNull Path directory) throws IOException {
        InstallLayout install = InstallLayout.of(directory);

        writeMod(install, "broken.jar", "{\"format\": 1, \"id\": ", true);

        ModEntry entry = ModCatalog.scan(install, InstallationRecords.NONE).getEntries().getFirst();

        Assertions.assertEquals(ModStatus.ERROR, entry.getStatus());
        Assertions.assertFalse(entry.getStatus().isStartable());
        Assertions.assertNull(entry.getArchive());
        Assertions.assertEquals(ProblemCode.MANIFEST_SYNTAX, ((ManifestIssue) entry.getIssues().getFirst()).getProblem().getCode());
    }

    @Test
    void scan_should_report_an_unsupported_engine_version_as_an_error(@TempDir @NotNull Path directory) throws IOException {
        InstallLayout install = InstallLayout.of(directory);

        writeMod(install, "future.jar", MANIFEST.formatted("future", "1.0"), true);

        ModEntry entry = ModCatalog.scan(install, InstallationRecords.NONE).getEntries().getFirst();

        Assertions.assertEquals(ModStatus.ERROR, entry.getStatus());
        Assertions.assertNotNull(entry.getManifest());
        Assertions.assertEquals(ProblemCode.ENGINE_UNSUPPORTED, ((ManifestIssue) entry.getIssues().getFirst()).getProblem().getCode());
    }

    @Test
    void scan_should_report_a_missing_image_as_an_error(@TempDir @NotNull Path directory) throws IOException {
        InstallLayout install = InstallLayout.of(directory);

        writeMod(install, "no-icon.jar", MANIFEST.formatted("no-icon", "0.1"), false);

        ModEntry entry = ModCatalog.scan(install, InstallationRecords.NONE).getEntries().getFirst();

        Assertions.assertEquals(ModStatus.ERROR, entry.getStatus());
        Assertions.assertEquals(ProblemCode.IMAGE_MISSING, ((ManifestIssue) entry.getIssues().getFirst()).getProblem().getCode());
    }

    @Test
    void scan_should_list_every_jar_of_a_duplicate_id_as_an_error_naming_the_other_jars(@TempDir @NotNull Path directory) throws IOException {
        InstallLayout install = InstallLayout.of(directory);

        writeMod(install, "first.jar", MANIFEST.formatted("same", "0.1"), true);
        writeMod(install, "second.jar", MANIFEST.formatted("same", "0.1"), true);
        writeMod(install, "other.jar", MANIFEST.formatted("other", "0.1"), true);

        ModCatalog catalog = ModCatalog.scan(install, InstallationRecords.NONE);
        ModEntry first = catalog.findByJarName("first.jar");
        ModEntry second = catalog.findByJarName("second.jar");
        ModEntry other = catalog.findByJarName("other.jar");

        Assertions.assertNotNull(first);
        Assertions.assertNotNull(second);
        Assertions.assertNotNull(other);
        Assertions.assertEquals(3, catalog.getEntries().size());
        Assertions.assertEquals(2, catalog.countStatus(ModStatus.ERROR));
        Assertions.assertEquals(List.of("second.jar"), ((IdConflictIssue) first.getIssues().getFirst()).getOtherJarNames());
        Assertions.assertEquals(List.of("first.jar"), ((IdConflictIssue) second.getIssues().getFirst()).getOtherJarNames());
        Assertions.assertEquals("id.conflict (same): first.jar and second.jar declare the same id", first.getIssues().getFirst().describeDetail());
        Assertions.assertEquals(ModStatus.WARNING, other.getStatus());
    }

    @Test
    void scan_should_return_an_empty_list_without_a_mods_folder(@TempDir @NotNull Path directory) throws IOException {
        Assertions.assertEquals(List.of(), ModCatalog.scan(InstallLayout.of(directory), InstallationRecords.NONE).getEntries());
    }

    private static void writeMod(@NotNull InstallLayout install, @NotNull String jarName,
                                 @NotNull String manifest, boolean withIcon) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();

        entries.put(TestJarUtils.MANIFEST_ENTRY, manifest.getBytes(StandardCharsets.UTF_8));

        if (withIcon) {
            entries.put("meta/icon.png", TestJarUtils.PNG_CONTENTS);
        }

        Files.createDirectories(install.getModsDirectory());
        TestJarUtils.write(install.getModsDirectory().resolve(jarName), entries);
    }
}
