package org.fogofwarlabs.sc4j.engine.mod;

import org.fogofwarlabs.sc4j.engine.client.settings.SettingsStore;
import org.fogofwarlabs.sc4j.engine.net.lobby.FolderLobbyMaps;
import org.fogofwarlabs.sc4j.engine.net.lobby.MapFolders;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/**
 * Gives a mod its own folder for whatever it keeps between runs: in-game options, replays, saved games, maps, caches.
 * Every mod has a separate folder, {@code <install>/userdata/mods/<modId>/}, so mods never share files.
 */
public interface ModStorage {
    /**
     * @return the absolute, normalized storage folder
     */
    @NotNull Path getRoot();

    /**
     * Resolves a path inside the storage folder.
     *
     * @param relativePath the relative path with {@code /} separators, such as {@code replays/last-game.sc4r}
     * @return the absolute, normalized path inside the storage folder
     * @throws IllegalArgumentException if the path is absolute, invalid or leaves the storage folder
     */
    @NotNull Path resolve(@NotNull String relativePath);

    /**
     * Returns the mod's settings file, the one {@link SettingsStore} of the mod: {@code <storage folder>/sc4j-settings.json}. Settings are never
     * shared between mods.
     *
     * @return the absolute, normalized path of the settings file
     */
    default @NotNull Path getSettingsFile() {
        return this.resolve(SettingsStore.FILE_NAME);
    }

    /**
     * Returns the mod's map folders, the folders of the engine's default lobby maps ({@link FolderLobbyMaps}): the map folder
     * {@code <storage folder>/maps}, searched with its subfolders, and its subfolder {@code downloads} that received maps are saved to.
     *
     * @return the map folders
     */
    default @NotNull MapFolders getMapFolders() {
        return MapFolders.of(List.of(this.resolve(MapFolders.STORAGE_FOLDER)), MapFolders.DOWNLOAD_FOLDER);
    }
}
