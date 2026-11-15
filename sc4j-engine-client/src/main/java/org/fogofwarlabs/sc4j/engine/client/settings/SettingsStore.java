package org.fogofwarlabs.sc4j.engine.client.settings;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.io.AtomicFileUtils;
import org.fogofwarlabs.sc4j.base.json.JsonReader;
import org.fogofwarlabs.sc4j.base.json.JsonWriterUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Reads and writes a mod's settings file: a JSON object of groups, each group a JSON object of keys and values, for example
 * {@code {"graphics": {"vsync": "off"}, "audio": {"musicVolume": 75}}}. Each mod has its own file in its storage folder
 * ({@code ModStorage.getSettingsFile()}); mods never share settings.
 * <p>
 * A read keeps the groups and keys in file order and leaves validation to the mod ({@link SettingsGroup} reads typed values). A write
 * ({@link #write}) replaces only the given keys of one group: every other group and key, known to the mod or not, stays as it was, and the
 * file is replaced atomically where the file system allows.
 */
public final class SettingsStore {
    /**
     * The file name of a mod's settings file in its storage folder.
     */
    public static final @NotNull String FILE_NAME = "sc4j-settings.json";

    /**
     * Reads a settings file.
     *
     * @param path the settings file
     * @return the created {@link SettingsStore} instance
     * @throws IOException if the file cannot be read or is not a JSON object
     */
    public static @NotNull SettingsStore read(@NotNull Path path) throws IOException {
        return new SettingsStore(readRoot(path));
    }

    /**
     * Writes values into a settings file: the file is read (when it exists), the values replace their keys in the group and every other group and
     * key, known or not, stays as it was; the result is written in the stable layout of {@link JsonWriterUtils} as UTF-8 to a temporary file that
     * then replaces the file (atomically where the file system allows; {@link AtomicFileUtils#write}). A missing file and its folders are created.
     *
     * @param path   the settings file
     * @param group  the group, for example {@code graphics}
     * @param values the keys and values: {@link String}, {@link Integer}, {@link Long}, {@link Boolean}
     * @throws IOException if the path does not name a file, the file cannot be read, is not a JSON object, or cannot be written
     */
    public static void write(@NotNull Path path, @NotNull String group, @NotNull Map<String, ?> values) throws IOException {
        Map<String, Object> root = Files.isRegularFile(path) ? readRoot(path) : new LinkedHashMap<>();
        Map<String, Object> groupValues = new LinkedHashMap<>();

        if (root.get(group) instanceof Map<?, ?> existingGroup) {
            for (Map.Entry<?, ?> entry : existingGroup.entrySet()) {
                groupValues.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        groupValues.putAll(values);
        root.put(group, groupValues);

        AtomicFileUtils.write(path, JsonWriterUtils.write(root).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Reads the top-level object of a settings file with its keys as strings, in file order.
     *
     * @param path the settings file
     * @return the groups by name
     * @throws IOException if the file cannot be read or is not a JSON object
     */
    private static @NotNull Map<String, Object> readRoot(@NotNull Path path) throws IOException {
        if (!(JsonReader.parse(Files.readAllBytes(path)) instanceof Map<?, ?> existing)) {
            throw Exceptions.newIOException("The settings file is not a JSON object: {0}", path);
        }

        Map<String, Object> root = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : existing.entrySet()) {
            root.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        return root;
    }

    private final @NotNull Map<String, Object> root;

    /**
     * Creates a {@link SettingsStore} instance over the top-level object of a file.
     *
     * @param root the groups by name, in file order
     */
    private SettingsStore(@NotNull Map<String, Object> root) {
        this.root = root;
    }

    /**
     * @return the names of the file's top-level entries in file order, groups and entries that are not groups alike
     */
    public @NotNull List<String> getGroupNames() {
        return Collections.unmodifiableList(new ArrayList<>(this.root.keySet()));
    }

    /**
     * Returns a group of the file.
     *
     * @param name the group name
     * @return the group, or {@code null} if the file has no entry of the name or the entry is not a JSON object
     */
    public @Nullable SettingsGroup getGroup(@NotNull String name) {
        return this.root.get(name) instanceof Map<?, ?> values ? new SettingsGroup(name, values) : null;
    }
}
