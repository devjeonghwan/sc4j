package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Gives a {@link NetworkLobby} the maps of the {@link MapFolders} of a mod, the engine's default {@link LobbyMaps}: a map is any file in the
 * read folders whose contents have the reference's digest, whatever the file's name, and a received map is saved in the save folder under the
 * reference's name and the mod's map file extension ({@link MapFolders#saveFile}). A mod's storage gives its default folders
 * ({@code ModStorage.getMapFolders()}).
 * <p>
 * The digest of a file is kept while the file keeps its size and modification time, so a check reads only new or changed files. The lobby
 * calls it on the thread that polls the lobby; it is not thread-safe.
 */
public final class FolderLobbyMaps implements LobbyMaps {
    /**
     * The characters a map name cannot keep in its file name; each becomes {@link #REPLACEMENT}.
     */
    private static final @NotNull String FORBIDDEN_CHARACTERS = "\\/:*?\"<>|";
    private static final char REPLACEMENT = '_';
    /**
     * The first character a file name can hold; the control characters below it become {@link #REPLACEMENT}.
     */
    private static final char FIRST_NAME_CHARACTER = ' ';

    /**
     * Holds the digest of a file's contents with the size and modification time it was read at.
     */
    private static final class FileDigest {
        private final long size;
        private final @NotNull FileTime modified;
        private final byte @NotNull [] digest;

        private FileDigest(long size, @NotNull FileTime modified, byte @NotNull [] digest) {
            this.size     = size;
            this.modified = modified;
            this.digest   = digest;
        }
    }

    /**
     * Creates the file name of a map: its name, every character a file name cannot hold replaced by {@code _}, and the extension.
     */
    private static @NotNull String createFileName(@NotNull String mapName, @NotNull String fileExtension) {
        StringBuilder name = new StringBuilder(mapName.length() + fileExtension.length());

        for (int index = 0; index < mapName.length(); index++) {
            char character = mapName.charAt(index);

            name.append(character < FIRST_NAME_CHARACTER || FORBIDDEN_CHARACTERS.indexOf(character) >= 0 ? REPLACEMENT : character);
        }

        return name.append(fileExtension).toString();
    }


    private final @NotNull MapFolders folders;
    private final @NotNull String fileExtension;

    private final @NotNull Map<@NotNull Path, @NotNull FileDigest> digests;

    /**
     * Creates a {@link FolderLobbyMaps} instance.
     *
     * @param folders       the map folders
     * @param fileExtension the extension of a saved map's file name with its dot, such as {@code .json}
     */
    public FolderLobbyMaps(@NotNull MapFolders folders, @NotNull String fileExtension) {
        this.folders       = folders;
        this.fileExtension = fileExtension;

        this.digests = new HashMap<>();
    }

    /**
     * Checks whether this machine has a map.
     * <p>
     * A file of the read folders has the map when its contents have the reference's digest; the name is not compared.
     *
     * @param map the map reference
     * @return {@code true} if this machine has the map of the reference's digest, {@code false} otherwise
     */
    @Override
    public boolean hasMap(@NotNull MapReference map) {
        return this.findFile(map) != null;
    }

    /**
     * Reads the file contents of a map for a member that misses it.
     *
     * @param map the map reference
     * @return the contents of the first file of the read folders with the reference's digest, or {@code null} if no folder holds one
     * @throws IOException if the map cannot be read
     */
    @Override
    public byte @Nullable [] readMap(@NotNull MapReference map) throws IOException {
        Path file = this.findFile(map);

        if (file == null) {
            return null;
        }

        byte[] contents = Files.readAllBytes(file);

        return MapReference.create(map.getName(), contents).isSameMap(map) ? contents : null;
    }

    /**
     * Keeps a map a joiner received; the lobby checked that the contents have the reference's digest.
     * <p>
     * Saves the contents in the save folder under the reference's name and the file extension ({@link MapFolders#saveFile}); the contents are
     * not checked against a map format.
     *
     * @param map      the map reference
     * @param contents the map file contents
     * @throws IOException if the contents cannot be saved
     */
    @Override
    public void addMap(@NotNull MapReference map, byte @NotNull [] contents) throws IOException {
        this.folders.saveFile(createFileName(map.getName(), this.fileExtension), contents);
    }

    private @Nullable Path findFile(@NotNull MapReference map) {
        byte[] wanted = map.getDigest();

        return this.folders.findFile(file -> Arrays.equals(wanted, this.readDigest(file)));
    }

    /**
     * Returns the digest of a file's contents, read again only when the file's size or modification time changed.
     */
    private byte @NotNull [] readDigest(@NotNull Path file) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
        FileDigest known = this.digests.get(file);

        if (known != null && known.size == attributes.size() && known.modified.equals(attributes.lastModifiedTime())) {
            return known.digest;
        }

        byte[] digest = MapReference.create(file.toString(), Files.readAllBytes(file)).getDigest();

        this.digests.put(file, new FileDigest(attributes.size(), attributes.lastModifiedTime(), digest));

        return digest;
    }
}
