package org.fogofwarlabs.sc4j.manifest.test;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes mod jars for tests: a manifest, images and the class files of test classes, in a given entry order.
 */
public final class TestJarUtils {
    /**
     * The manifest's jar entry.
     */
    public static final @NotNull String MANIFEST_ENTRY = "sc4j-mod.json";

    /**
     * The smallest PNG signature that the manifest reader accepts as an image.
     */
    public static final byte @NotNull [] PNG_CONTENTS = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 0};

    private static final @NotNull String CLASS_EXTENSION = ".class";

    /**
     * Writes a jar with entries in the given order.
     *
     * @param jar     the jar to write
     * @param entries the contents by entry name
     * @throws IOException if the jar cannot be written
     */
    public static void write(@NotNull Path jar, @NotNull Map<@NotNull String, byte @NotNull []> entries) throws IOException {
        try (OutputStream output = Files.newOutputStream(jar);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }

    /**
     * Creates the entries of a mod jar: the manifest followed by the class files of test classes.
     *
     * @param manifest the manifest text
     * @param classes  the classes whose class files the jar holds, read from the test class path
     * @return the entries by name, in that order
     * @throws IOException if a class file cannot be read
     */
    public static @NotNull Map<@NotNull String, byte @NotNull []> createEntries(@NotNull String manifest, @NotNull Class<?> @NotNull ... classes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();

        entries.put(MANIFEST_ENTRY, manifest.getBytes(StandardCharsets.UTF_8));

        for (Class<?> type : classes) {
            entries.put(getEntryName(type), readClass(type));
        }

        return entries;
    }

    /**
     * Returns the jar entry of a class file.
     *
     * @param type the class
     * @return the entry, such as {@code org/example/ExampleMod.class}
     */
    public static @NotNull String getEntryName(@NotNull Class<?> type) {
        return type.getName().replace('.', '/') + CLASS_EXTENSION;
    }

    /**
     * Reads the class file of a class from the class path.
     *
     * @param type the class
     * @return the class file bytes
     * @throws IOException if the class file cannot be read
     */
    public static byte @NotNull [] readClass(@NotNull Class<?> type) throws IOException {
        ClassLoader classLoader = type.getClassLoader();

        try (InputStream input = classLoader == null ? null : classLoader.getResourceAsStream(getEntryName(type))) {
            if (input == null) {
                throw Exceptions.newIOException("No class file for {0}", type.getName());
            }

            return input.readAllBytes();
        }
    }


    private TestJarUtils() {
    }
}
