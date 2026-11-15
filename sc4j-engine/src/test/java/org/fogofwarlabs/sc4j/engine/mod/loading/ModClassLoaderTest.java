package org.fogofwarlabs.sc4j.engine.mod.loading;

import org.fogofwarlabs.sc4j.engine.mod.Mod;
import org.fogofwarlabs.sc4j.manifest.test.TestJarUtils;
import org.fogofwarlabs.sc4j.testmod.ExampleMod;
import org.fogofwarlabs.sc4j.testmod.NotAMod;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Tests the isolation of the mod class loader: engine-side packages come only from the runtime, mod classes from the jar
 * first.
 */
final class ModClassLoaderTest {
    private static final @NotNull String MANIFEST = "{}";
    private static final @NotNull String INJECTED_CLASS = "org.fogofwarlabs.sc4j.engine.mod.InjectedType";

    @Test
    void protected_classes_come_from_the_parent_even_when_the_jar_ships_them(@TempDir @NotNull Path directory) throws IOException,
                                                                                                                      ClassNotFoundException {
        Map<String, byte[]> entries = TestJarUtils.createEntries(MANIFEST, ExampleMod.class);
        byte[] foreignClass = TestJarUtils.readClass(NotAMod.class);

        entries.put(TestJarUtils.getEntryName(Mod.class), foreignClass);
        entries.put(INJECTED_CLASS.replace('.', '/') + ".class", foreignClass);

        try (ModClassLoader loader = createLoader(directory, entries)) {
            Assertions.assertSame(Mod.class, Class.forName(Mod.class.getName(), false, loader));
            Assertions.assertThrows(ClassNotFoundException.class, () -> Class.forName(INJECTED_CLASS, false, loader));
            Assertions.assertNull(loader.getResource(INJECTED_CLASS.replace('.', '/') + ".class"));
            Assertions.assertTrue(ModClassLoader.isProtected("org.fogofwarlabs.sc4j.runtime.RuntimeMain"));
            Assertions.assertFalse(ModClassLoader.isProtected("org.example.ExampleMod"));
        }
    }

    @Test
    void mod_classes_come_from_the_jar_before_the_parent(@TempDir @NotNull Path directory) throws IOException, ClassNotFoundException {
        try (ModClassLoader loader = createLoader(directory, TestJarUtils.createEntries(MANIFEST, ExampleMod.class))) {
            Class<?> loaded = Class.forName(ExampleMod.class.getName(), false, loader);
            Class<?> parentOnly = Class.forName(Assertions.class.getName(), false, loader);

            Assertions.assertSame(loader, loaded.getClassLoader());
            Assertions.assertNotSame(ExampleMod.class, loaded);
            Assertions.assertTrue(Mod.class.isAssignableFrom(loaded));
            Assertions.assertSame(Assertions.class, parentOnly);
            Assertions.assertNotNull(loader.getResource(TestJarUtils.MANIFEST_ENTRY));
        }
    }

    private static @NotNull ModClassLoader createLoader(@NotNull Path directory, @NotNull Map<String, byte[]> entries) throws IOException {
        Path jar = directory.resolve("example.jar");

        TestJarUtils.write(jar, entries);

        return new ModClassLoader("example", jar.toUri().toURL(), ModClassLoaderTest.class.getClassLoader());
    }
}
