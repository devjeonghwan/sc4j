package org.fogofwarlabs.sc4j.engine.mod.identity;

import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.test.TestJarUtils;
import org.fogofwarlabs.sc4j.manifest.version.EngineVersion;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests that the mod identity is stable for the same jar contents, changes with any entry or identity data, names the first differing part
 * and survives its wire form, also inside a serialized refusal ({@link ModIdentityException}).
 */
final class ModIdentityTest {
    private static final @NotNull String MANIFEST = """
                                                    {"format": 1, "id": "example", "name": {"en": "Example"}, "version": "1.0.0", "engine": "0.1", "entry": "org.example.ExampleMod"}
                                                    """;
    private static final @NotNull String DATA_ENTRY = "data/units.json";
    private static final @NotNull String RULES_ENTRY = "data/rules.json";

    private static @NotNull ModIdentity createIdentity(@NotNull Path jar, @NotNull List<byte[]> identityData) throws IOException, ManifestException {
        ModManifest manifest = ModManifestReader.read(jar).getManifest();

        return ModIdentity.create(EngineVersion.CURRENT, manifest, ModContentDigestUtils.compute(jar), identityData);
    }

    private static @NotNull Path writeJar(@NotNull Path jar, boolean reversed, @NotNull String units) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        byte[] manifest = MANIFEST.getBytes(StandardCharsets.UTF_8);
        byte[] unitData = units.getBytes(StandardCharsets.UTF_8);
        byte[] rules = "rules".getBytes(StandardCharsets.UTF_8);

        if (reversed) {
            entries.put(RULES_ENTRY, rules);
            entries.put(DATA_ENTRY, unitData);
            entries.put(TestJarUtils.MANIFEST_ENTRY, manifest);
        } else {
            entries.put(TestJarUtils.MANIFEST_ENTRY, manifest);
            entries.put(DATA_ENTRY, unitData);
            entries.put(RULES_ENTRY, rules);
        }

        TestJarUtils.write(jar, entries);

        return jar;
    }

    @Test
    void identity_stays_equal_for_the_same_jar_contents_in_any_entry_order(@TempDir @NotNull Path directory) throws IOException, ManifestException {
        Path first = writeJar(directory.resolve("first.jar"), false, "units");
        Path reordered = writeJar(directory.resolve("reordered.jar"), true, "units");

        ModIdentity identity = createIdentity(first, List.of());

        Assertions.assertEquals(identity, createIdentity(first, List.of()));
        Assertions.assertEquals(identity, createIdentity(reordered, List.of()));
        Assertions.assertEquals(identity.hashCode(), createIdentity(reordered, List.of()).hashCode());
        Assertions.assertTrue(identity.describe().startsWith("example 1.0.0 (engine 0.1, content "), identity.describe());
    }

    @Test
    void identity_changes_with_an_entry_or_identity_data(@TempDir @NotNull Path directory) throws IOException, ManifestException {
        Path original = writeJar(directory.resolve("original.jar"), false, "units");
        Path changed = writeJar(directory.resolve("changed.jar"), false, "units!");
        ModIdentity identity = createIdentity(original, List.of());
        ModIdentity withData = createIdentity(original, List.of("data 7".getBytes(StandardCharsets.UTF_8)));

        Assertions.assertNotEquals(identity, createIdentity(changed, List.of()));
        Assertions.assertNotEquals(identity, withData);
        Assertions.assertArrayEquals(identity.getContentDigest(), withData.getContentDigest());
        Assertions.assertFalse(Arrays.equals(identity.getDataDigest(), withData.getDataDigest()));
        Assertions.assertEquals(withData, createIdentity(original, List.of("data 7".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void find_mismatch_should_name_the_first_differing_part(@TempDir @NotNull Path directory) throws IOException, ManifestException {
        Path original = writeJar(directory.resolve("original.jar"), false, "units");
        Path changed = writeJar(directory.resolve("changed.jar"), false, "units!");
        ModIdentity identity = createIdentity(original, List.of());
        ModManifest manifest = ModManifestReader.read(original).getManifest();
        ModIdentity otherEngine = ModIdentity.create(EngineVersion.parse("2.0"), manifest, ModContentDigestUtils.compute(original), List.of());

        Assertions.assertNull(identity.findMismatch(createIdentity(original, List.of())));
        Assertions.assertEquals(ModIdentityMismatch.ENGINE_VERSION, identity.findMismatch(otherEngine));
        Assertions.assertEquals(ModIdentityMismatch.MOD_CONTENT, identity.findMismatch(createIdentity(changed, List.of())));
        Assertions.assertEquals(ModIdentityMismatch.IDENTITY_DATA, identity.findMismatch(createIdentity(original, List.of(new byte[]{1}))));
    }

    @Test
    void read_should_return_the_identity_that_write_wrote(@TempDir @NotNull Path directory) throws IOException, ManifestException {
        ModIdentity identity = createIdentity(writeJar(directory.resolve("original.jar"), false, "units"), List.of(new byte[]{1}));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        identity.write(new DataOutputStream(bytes));

        ModIdentity read = ModIdentity.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        Assertions.assertEquals(identity, read);
        Assertions.assertNull(identity.findMismatch(read));
        Assertions.assertThrows(IOException.class, () -> ModIdentity.read(new DataInputStream(new ByteArrayInputStream(new byte[]{0, 1, 'x'}))));
    }

    @Test
    void refusal_keeps_the_recorded_identity_through_serialization(@TempDir @NotNull Path directory) throws Exception {
        ModIdentity recorded = createIdentity(writeJar(directory.resolve("recorded.jar"), false, "units"), List.of(new byte[]{1}));
        ModIdentity local = createIdentity(writeJar(directory.resolve("local.jar"), false, "units!"), List.of(new byte[]{1}));
        ModIdentityException refusal = ModIdentityException.create("replay", ModIdentityMismatch.MOD_CONTENT, recorded, local);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(refusal);
        }

        ModIdentityException read;

        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            read = (ModIdentityException) input.readObject();
        }

        Assertions.assertEquals(refusal.getMessage(), read.getMessage());
        Assertions.assertEquals(ModIdentityMismatch.MOD_CONTENT, read.getMismatch());
        Assertions.assertEquals(recorded, read.getRecordedIdentity());
    }
}
