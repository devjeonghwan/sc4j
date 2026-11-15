package org.fogofwarlabs.sc4j.manifest.settings.value;

import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.fogofwarlabs.sc4j.manifest.settings.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads and saves the values of one mod's settings where the install keeps them: the value of a setting without {@code shared} in the
 * mod's settings value file ({@code userdata/settings/<modId>.json}) under its key, the value of a setting with {@code shared} in the
 * shared settings value file ({@code userdata/shared-settings.json}) under its shared name, so every mod that declares the same shared name
 * reads and saves one value. Both files have the settings value file format ({@link SettingValuesFile}); the shared one is read and written
 * only for a mod with shared settings.
 */
public final class SettingValueStore {
    /**
     * Creates a {@link SettingValueStore} instance for a mod of an install, with its settings value file
     * ({@link InstallLayout#getSettingsFile}) and the install's shared settings value file ({@link InstallLayout#getSharedSettingsFile()}).
     *
     * @param install  the install
     * @param manifest the mod's manifest
     * @return the created {@link SettingValueStore} instance
     */
    public static @NotNull SettingValueStore of(@NotNull InstallLayout install, @NotNull ModManifest manifest) {
        return new SettingValueStore(manifest.getSettings(), new SettingValuesFile(install.getSettingsFile(manifest.getId())),
                                     new SettingValuesFile(install.getSharedSettingsFile()));
    }


    private final @NotNull SettingsSchema schema;
    private final @NotNull SettingValuesFile modFile;
    private final @NotNull SettingValuesFile sharedFile;
    private final boolean shared;

    /**
     * Creates a {@link SettingValueStore} instance.
     *
     * @param schema     the mod's settings schema
     * @param modFile    the mod's settings value file
     * @param sharedFile the install's shared settings value file
     */
    public SettingValueStore(@NotNull SettingsSchema schema, @NotNull SettingValuesFile modFile, @NotNull SettingValuesFile sharedFile) {
        boolean anyShared = false;

        for (SettingDefinition definition : schema.getDefinitions()) {
            anyShared |= definition.getSharedName() != null;
        }

        this.schema     = schema;
        this.modFile    = modFile;
        this.sharedFile = sharedFile;
        this.shared     = anyShared;
    }

    /**
     * @return the mod's settings value file
     */
    public @NotNull SettingValuesFile getModFile() {
        return this.modFile;
    }

    /**
     * @return the install's shared settings value file
     */
    public @NotNull SettingValuesFile getSharedFile() {
        return this.sharedFile;
    }

    /**
     * Reads the saved values of the mod's settings.
     *
     * @return the parsed values by setting key, in schema order; a setting without a saved value has no entry
     * @throws IOException            if a file exists but cannot be read
     * @throws SettingValuesException if a file is not valid JSON or not in the settings value format
     */
    public @NotNull Map<@NotNull String, @NotNull Object> read() throws IOException, SettingValuesException {
        return this.resolve(InstallationRecords.NONE);
    }

    /**
     * Reads the saved values of the mod's settings and fills each {@code directory} setting without a saved value that declares
     * {@code detect} with the directory the computer's records give ({@link DirectorySetting#detect}). A detected directory is saved, so a
     * first start finds it and the user can still choose another.
     *
     * @param records the computer's installation records
     * @return the parsed values by setting key, in schema order; a setting without a saved or detected value has no entry
     * @throws IOException            if a file exists but cannot be read, or a detected directory cannot be saved
     * @throws SettingValuesException if a file is not valid JSON or not in the settings value format
     */
    public @NotNull Map<@NotNull String, @NotNull Object> resolve(@NotNull InstallationRecords records) throws IOException, SettingValuesException {
        Map<String, Object> modValues = new LinkedHashMap<>(this.modFile.read());
        Map<String, Object> sharedValues = this.shared ? new LinkedHashMap<>(this.sharedFile.read()) : new LinkedHashMap<>();
        Map<String, Object> values = new LinkedHashMap<>();
        boolean modDetected = false;
        boolean sharedDetected = false;

        for (SettingDefinition definition : this.schema.getDefinitions()) {
            String sharedName = definition.getSharedName();
            Map<String, Object> source = sharedName == null ? modValues : sharedValues;
            String name = sharedName == null ? definition.getKey() : sharedName;
            Object value = source.get(name);
            Path detected = value == null && definition instanceof DirectorySetting directory ? directory.detect(records) : null;

            if (detected != null) {
                value = SettingType.DIRECTORY.toJson(detected);
                source.put(name, value);
                modDetected |= sharedName == null;
                sharedDetected |= sharedName != null;
            }

            if (value != null) {
                values.put(definition.getKey(), value);
            }
        }

        if (modDetected) {
            this.modFile.write(modValues);
        }

        if (sharedDetected) {
            this.sharedFile.write(sharedValues);
        }

        return values;
    }

    /**
     * Saves values: the values of the mod's own settings replace the mod's settings value file, and the value of each shared setting
     * replaces its value in the shared settings value file, whose values of other names stay. A shared setting without a value removes its
     * saved value. Values without a setting are not saved.
     *
     * @param savedValues the JSON values by setting key, as the settings value file stores them
     * @throws IOException            if a file cannot be read or written
     * @throws SettingValuesException if the existing shared settings value file is not valid JSON or not in the settings value format
     */
    public void write(@NotNull Map<@NotNull String, ?> savedValues) throws IOException, SettingValuesException {
        Map<String, Object> modValues = new LinkedHashMap<>();
        Map<String, Object> sharedValues = this.shared ? new LinkedHashMap<>(this.sharedFile.read()) : new LinkedHashMap<>();

        for (SettingDefinition definition : this.schema.getDefinitions()) {
            String sharedName = definition.getSharedName();
            Object value = savedValues.get(definition.getKey());

            if (sharedName != null && value == null) {
                sharedValues.remove(sharedName);
            } else if (sharedName != null) {
                sharedValues.put(sharedName, value);
            } else if (value != null) {
                modValues.put(definition.getKey(), value);
            }
        }

        this.modFile.write(modValues);

        if (this.shared) {
            this.sharedFile.write(sharedValues);
        }
    }
}
