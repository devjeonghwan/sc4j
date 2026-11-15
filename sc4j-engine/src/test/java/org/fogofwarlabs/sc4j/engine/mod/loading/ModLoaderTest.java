package org.fogofwarlabs.sc4j.engine.mod.loading;

import org.fogofwarlabs.sc4j.engine.mod.ModException;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModArchive;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.test.TestJarUtils;
import org.fogofwarlabs.sc4j.testmod.ArgumentMod;
import org.fogofwarlabs.sc4j.testmod.ExampleMod;
import org.fogofwarlabs.sc4j.testmod.NotAMod;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Tests creating a mod's entry class from its jar and the failures for unusable entries.
 */
final class ModLoaderTest {
    private static final @NotNull String MANIFEST = """
                                                    {"format": 1, "id": "example", "name": {"en": "Example"}, "version": "1.0.0", "engine": "0.1", "entry": "%s"}
                                                    """;

    @Test
    void load_should_create_the_entry_from_the_mod_jar(@TempDir @NotNull Path directory) throws IOException, ManifestException, ModException {
        try (LoadedMod loaded = load(directory, ExampleMod.class.getName())) {
            Assertions.assertEquals(ExampleMod.class.getName(), loaded.getMod().getClass().getName());
            Assertions.assertSame(loaded.getClassLoader(), loaded.getMod().getClass().getClassLoader());
        }
    }

    @Test
    void load_should_reject_entries_that_are_not_usable_mods(@TempDir @NotNull Path directory) {
        assertRejected(directory, NotAMod.class.getName(), "does not implement");
        assertRejected(directory, ArgumentMod.class.getName(), "no public constructor without arguments");
        assertRejected(directory, "org.fogofwarlabs.sc4j.testmod.MissingMod", "cannot be loaded");
        assertRejected(directory, ModLoader.class.getName(), "is not in the mod jar");
    }

    private static void assertRejected(@NotNull Path directory, @NotNull String entry, @NotNull String reason) {
        ModException exception = Assertions.assertThrows(ModException.class, () -> load(directory, entry).close());

        Assertions.assertTrue(exception.getMessage().contains(reason), exception.getMessage());
    }

    private static @NotNull LoadedMod load(@NotNull Path directory, @NotNull String entry) throws IOException, ManifestException, ModException {
        Path jar = directory.resolve(entry + ".jar");

        TestJarUtils.write(jar, TestJarUtils.createEntries(MANIFEST.formatted(entry), ExampleMod.class, NotAMod.class, ArgumentMod.class));

        ModArchive archive = ModManifestReader.read(jar);

        return new ModLoader(ModLoaderTest.class.getClassLoader()).load(archive);
    }
}
