package org.fogofwarlabs.sc4j.demo.map;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.fogofwarlabs.sc4j.engine.map.JsonMapLoader;
import org.fogofwarlabs.sc4j.engine.map.MapCatalog;
import org.fogofwarlabs.sc4j.engine.map.MapModel;
import org.fogofwarlabs.sc4j.engine.map.StandardMapData;
import org.fogofwarlabs.sc4j.engine.net.lobby.FolderLobbyMaps;
import org.fogofwarlabs.sc4j.engine.net.lobby.LobbyMaps;
import org.fogofwarlabs.sc4j.engine.net.lobby.MapFolders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Holds the built-in maps of the demo, files of the engine map format in the demo's resources, in an engine {@link MapCatalog}: a game setup
 * names its map by {@link MapReference}, and the systems of each game read the terrain of that map. They are the maps of the demo's network
 * lobby ({@link LobbyMaps}) together with the map files of the demo's map folders, through the engine's {@link FolderLobbyMaps}: the host
 * sends a map's file to a member that misses it, and a member saves a received map in its map folders and keeps it next to the built-in ones.
 * A map file of the folders is loaded into the maps once a lobby names it.
 * <p>
 * Ashen Ford: 64 x 64 tiles, a base with three gold mines in the north-west and the south-east corner, a rocky ridge across the middle with a
 * ford in the west and one in the east, annihilation victory.
 */
public final class DemoMaps implements LobbyMaps {
    /**
     * The resource folder of the maps.
     */
    public static final @NotNull String MAP_ROOT = "demo/maps/";
    /**
     * The name of the two-player map Ashen Ford.
     */
    public static final @NotNull String ASHEN_FORD = "Ashen Ford";
    /**
     * The file extension of a map of the engine map format.
     */
    public static final @NotNull String MAP_FILE_EXTENSION = ".json";

    /**
     * The file names of the built-in maps in the resource folder, in menu order.
     */
    private static final @NotNull List<@NotNull String> MAP_FILES = List.of("ashen-ford.json");

    /**
     * Loads the built-in maps from the resources of a class loader over the types of the demo's content.
     *
     * @param loader  the class loader of the demo's classes and resources
     * @param content the demo's content, whose unit types and resource kinds the maps name
     * @param folders the demo's map folders, which hold the maps received in network lobbies
     * @return the maps
     * @throws IOException if a map is missing or not a valid map of the engine format
     */
    public static @NotNull DemoMaps load(@NotNull ClassLoader loader, @NotNull DemoContent content, @NotNull MapFolders folders) throws IOException {
        JsonMapLoader mapLoader = new JsonMapLoader(content.getUnitTypes(), content.getResourceKinds());
        MapCatalog catalog = new MapCatalog();

        for (String file : MAP_FILES) {
            String resource = MAP_ROOT + file;
            byte[] contents;

            try (InputStream input = loader.getResourceAsStream(resource)) {
                if (input == null) {
                    throw Exceptions.newIOException("The demo map {0} is missing.", resource);
                }

                contents = input.readAllBytes();
            }

            MapModel map = mapLoader.read(contents, resource);

            catalog.add(map.requireModData(StandardMapData.class).getName(), contents, map);
        }

        return new DemoMaps(mapLoader, catalog, new FolderLobbyMaps(folders, MAP_FILE_EXTENSION));
    }


    private final @NotNull JsonMapLoader mapLoader;
    private final @NotNull MapCatalog catalog;
    private final @NotNull FolderLobbyMaps files;

    private DemoMaps(@NotNull JsonMapLoader mapLoader, @NotNull MapCatalog catalog, @NotNull FolderLobbyMaps files) {
        this.mapLoader = mapLoader;
        this.catalog   = catalog;
        this.files     = files;
    }

    /**
     * @return the references of the built-in maps in menu order
     */
    public @NotNull List<@NotNull MapReference> getReferences() {
        return this.catalog.getReferences();
    }


    /**
     * Checks whether this machine has a map.
     * <p>
     * A map file of the map folders with the reference's digest is loaded and added to the demo's maps under the reference's name; a file that
     * is no map of the engine format does not count.
     *
     * @param map the map reference
     * @return {@code true} if this machine has the map of the reference's name and digest, {@code false} otherwise
     */
    @Override
    public boolean hasMap(@NotNull MapReference map) {
        if (this.catalog.find(map) != null) {
            return true;
        }

        try {
            byte[] contents = this.files.readMap(map);

            if (contents == null) {
                return false;
            }

            this.catalog.add(map.getName(), contents, this.mapLoader.read(contents, map.getName()));

            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Reads the file contents of a map for a member that misses it.
     *
     * @param map the map reference
     * @return the contents of the map of the reference's name and digest, or {@code null} if this machine cannot send it
     * @throws IOException if the map cannot be read
     */
    @Override
    public byte @Nullable [] readMap(@NotNull MapReference map) throws IOException {
        byte[] contents = this.catalog.findContents(map);

        return contents != null ? contents : this.files.readMap(map);
    }

    /**
     * Keeps a map a joiner received; the lobby checked that the contents have the reference's digest.
     * <p>
     * Loads the map of the engine format, saves it in the map folders ({@link FolderLobbyMaps#addMap}) and adds it to the demo's maps.
     *
     * @param map      the map reference
     * @param contents the map file contents
     * @throws IOException if the contents are no map of the engine format over the demo's content or cannot be saved
     */
    @Override
    public void addMap(@NotNull MapReference map, byte @NotNull [] contents) throws IOException {
        MapModel loaded = this.mapLoader.read(contents, map.getName());

        this.files.addMap(map, contents);
        this.catalog.add(map.getName(), contents, loaded);
    }

    /**
     * Returns the reference of a built-in map by its name.
     *
     * @param name the map name, such as {@link #ASHEN_FORD}
     * @return the reference
     * @throws IllegalArgumentException if no built-in map has the name
     */
    public @NotNull MapReference requireReference(@NotNull String name) {
        for (MapReference reference : this.catalog.getReferences()) {
            if (reference.getName().equals(name)) {
                return reference;
            }
        }

        throw Exceptions.newIllegalArgumentException("The demo has no map named {0}.", name);
    }

    /**
     * Returns the map of a game setup.
     *
     * @param setup the game setup
     * @return the map of the setup's map reference
     * @throws IllegalArgumentException if no built-in map has the setup's name and digest
     */
    public @NotNull MapModel require(@NotNull GameSetup setup) {
        return this.catalog.require(setup);
    }

    /**
     * Returns the map of a reference.
     *
     * @param reference the map reference
     * @return the map of the same name and digest
     * @throws IllegalArgumentException if no built-in map has the reference's name and digest
     */
    public @NotNull MapModel require(@NotNull MapReference reference) {
        return this.catalog.require(reference);
    }
}
