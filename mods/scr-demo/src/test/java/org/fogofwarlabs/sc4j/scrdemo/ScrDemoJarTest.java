package org.fogofwarlabs.sc4j.scrdemo;

import org.fogofwarlabs.sc4j.engine.mod.identity.ModContentDigestUtils;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentityMismatch;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.settings.SettingDefinition;
import org.fogofwarlabs.sc4j.manifest.version.EngineVersion;
import org.fogofwarlabs.sc4j.scr.ScrMod;
import org.fogofwarlabs.sc4j.scrdemo.clone.CloneOverlay;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Tests the built jar of demo 2 against the built SC:R mod jar: it holds every entry of the SC:R mod jar plus only the demo's own classes
 * (package {@code org.fogofwarlabs.sc4j.scrdemo}), the service file that names its content extension, the art sets' folders
 * ({@code third-party/<set>/}) and the files the overlay adds, and of the shared entries exactly the files the overlay replaces differ
 * (a replaced source as its class and nested classes); its manifest has its own mod id with the SC:R mod's entry, engine version and
 * settings; and its mod identity differs from the SC:R mod's first in the mod id.
 */
final class ScrDemoJarTest {
    private static final @NotNull String MOD_ID = "scr-demo";
    private static final @NotNull String RESOURCES_FOLDER = "/src/main/resources/";
    private static final @NotNull String SOURCES_FOLDER = "/src/main/java/";
    private static final @NotNull String SOURCE_SUFFIX = ".java";
    private static final @NotNull String CLASS_SUFFIX = ".class";
    private static final @NotNull String NESTED_CLASS_SEPARATOR = "$";
    private static final @NotNull String DEMO_PACKAGE_FOLDER = "org/fogofwarlabs/sc4j/scrdemo/";
    private static final @NotNull String ART_FOLDER = "third-party/";
    private static final @NotNull String EXTENSION_SERVICE = "META-INF/services/org.fogofwarlabs.sc4j.scr.game.extension.ScrContentExtension";
    private static final @NotNull String EXTENSION_CLASS = "org.fogofwarlabs.sc4j.scrdemo.ScrDemoContentExtension";

    /**
     * Creates the identity the runtime computes for a jar of the SC:R mod or its clone with the supported installation, whose build is the
     * mod's identity data.
     */
    static @NotNull ModIdentity createIdentity(@NotNull Path jar) throws IOException, ManifestException {
        return ModIdentity.create(EngineVersion.CURRENT, ModManifestReader.read(jar).getManifest(), ModContentDigestUtils.compute(jar),
                                  List.of(ScrMod.SUPPORTED_BUILD.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Lists the jar entries of the overlay files: a resource as its entry, a source as the entry of its class.
     */
    private static @NotNull List<@NotNull String> getOverlayEntries() throws IOException {
        List<String> overlaid = new ArrayList<>();

        for (String path : CloneOverlay.listPaths(ScrDemoTestResources.getOverlay())) {
            int resources = path.indexOf(RESOURCES_FOLDER);
            int sources = path.indexOf(SOURCES_FOLDER);

            if (resources > 0) {
                overlaid.add(path.substring(resources + RESOURCES_FOLDER.length()));
            } else {
                Assertions.assertTrue(sources > 0 && path.endsWith(SOURCE_SUFFIX), () -> "the overlay holds a main source or resource: " + path);
                overlaid.add(path.substring(sources + SOURCES_FOLDER.length(), path.length() - SOURCE_SUFFIX.length()) + CLASS_SUFFIX);
            }
        }

        return overlaid;
    }

    /**
     * @return {@code true} if the entry is an overlay entry or a nested class of an overlay source, {@code false} otherwise
     */
    private static boolean isOverlaid(@NotNull List<@NotNull String> overlaid, @NotNull String name) {
        for (String entry : overlaid) {
            if (name.equals(entry) ||
                entry.endsWith(CLASS_SUFFIX) &&
                name.startsWith(entry.substring(0, entry.length() - CLASS_SUFFIX.length()) + NESTED_CLASS_SEPARATOR)) {
                return true;
            }
        }

        return false;
    }

    private static @NotNull Map<@NotNull String, byte @NotNull []> readEntries(@NotNull Path jar) throws IOException {
        Map<String, byte[]> entries = new TreeMap<>();

        try (JarFile file = new JarFile(jar.toFile())) {
            for (JarEntry entry : file.stream().toList()) {
                if (entry.isDirectory()) {
                    continue;
                }

                try (InputStream input = file.getInputStream(entry)) {
                    entries.put(entry.getName(), input.readAllBytes());
                }
            }
        }

        return entries;
    }


    @Test
    void the_demo_jar_holds_the_sc_r_mod_entries_and_differs_only_in_the_overlay() throws IOException {
        Map<String, byte[]> demo = readEntries(ScrDemoTestResources.getModJar());
        Map<String, byte[]> scr = readEntries(ScrDemoTestResources.getScrModJar());
        List<String> overlaid = getOverlayEntries();
        List<String> changed = new ArrayList<>();

        Assertions.assertTrue(demo.keySet().containsAll(scr.keySet()), "the demo jar holds every entry of the SC:R mod jar");
        Assertions.assertEquals(EXTENSION_CLASS, new String(demo.get(EXTENSION_SERVICE), StandardCharsets.UTF_8).strip());

        for (String name : demo.keySet()) {
            Assertions.assertTrue(scr.containsKey(name) || name.startsWith(DEMO_PACKAGE_FOLDER) || name.startsWith(ART_FOLDER) ||
                                  name.equals(EXTENSION_SERVICE) || isOverlaid(overlaid, name),
                                  () -> "an entry beyond the SC:R mod: " + name);
        }

        for (Map.Entry<String, byte[]> entry : scr.entrySet()) {
            if (!Arrays.equals(entry.getValue(), demo.get(entry.getKey()))) {
                changed.add(entry.getKey());
            }
        }

        for (String name : changed) {
            Assertions.assertTrue(isOverlaid(overlaid, name), () -> "an entry the overlay does not replace differs: " + name);
        }

        for (String name : overlaid) {
            Assertions.assertTrue(changed.contains(name) || !scr.containsKey(name) && demo.containsKey(name),
                                  () -> "an entry of the overlay is unchanged or missing: " + name);
        }
    }

    @Test
    void the_demo_manifest_has_its_own_id_and_the_sc_r_mod_entry_and_settings() throws ManifestException {
        ModManifest demo = ModManifestReader.read(ScrDemoTestResources.getModJar()).getManifest();
        ModManifest scr = ModManifestReader.read(ScrDemoTestResources.getScrModJar()).getManifest();
        SettingDefinition demoFolder = demo.getSettings().findDefinition(ScrMod.GAME_FOLDER_SETTING);
        SettingDefinition scrFolder = scr.getSettings().findDefinition(ScrMod.GAME_FOLDER_SETTING);

        Assertions.assertEquals(MOD_ID, demo.getId());
        Assertions.assertNotEquals(scr.getId(), demo.getId());
        Assertions.assertNotEquals(scr.getName().get("en"), demo.getName().get("en"));
        Assertions.assertEquals(ScrMod.class.getName(), demo.getEntry());
        Assertions.assertEquals(scr.getEngine(), demo.getEngine());
        Assertions.assertEquals(scr.getVersion(), demo.getVersion());
        Assertions.assertNotNull(demoFolder);
        Assertions.assertNotNull(scrFolder);
        Assertions.assertNotNull(scrFolder.getSharedName());
        Assertions.assertEquals(scrFolder.getSharedName(), demoFolder.getSharedName());
        Assertions.assertNotNull(demo.getSettings().findDefinition(ScrMod.LOCALE_SETTING));
    }

    @Test
    void the_demo_identity_differs_from_the_sc_r_mod_identity_in_the_mod_id() throws IOException, ManifestException {
        ModIdentity demo = createIdentity(ScrDemoTestResources.getModJar());
        ModIdentity scr = createIdentity(ScrDemoTestResources.getScrModJar());

        Assertions.assertEquals(ModIdentityMismatch.MOD_ID, scr.findMismatch(demo));
        Assertions.assertNotEquals(scr, demo);
    }
}
