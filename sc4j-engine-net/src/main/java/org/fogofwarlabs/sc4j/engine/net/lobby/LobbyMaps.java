package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Gives a {@link NetworkLobby} the maps of this machine by {@link MapReference} (name and digest): whether this machine has a map, the
 * contents the host sends to a member that misses it, and where a joiner keeps a received map. The engine's default is {@link FolderLobbyMaps}
 * over the map folders of the mod's storage; a mod with maps of its own, for example an engine
 * {@link org.fogofwarlabs.sc4j.engine.map.MapCatalog} of built-in maps, adds them around it.
 * <p>
 * The lobby calls it on the thread that polls the lobby.
 */
public interface LobbyMaps {
    /**
     * Checks whether this machine has a map.
     *
     * @param map the map reference
     * @return {@code true} if this machine has the map of the reference's name and digest, {@code false} otherwise
     */
    boolean hasMap(@NotNull MapReference map);

    /**
     * Reads the file contents of a map for a member that misses it.
     *
     * @param map the map reference
     * @return the contents of the map of the reference's name and digest, or {@code null} if this machine cannot send it
     * @throws IOException if the map cannot be read
     */
    byte @Nullable [] readMap(@NotNull MapReference map) throws IOException;

    /**
     * Keeps a map a joiner received; the lobby checked that the contents have the reference's digest.
     *
     * @param map      the map reference
     * @param contents the map file contents
     * @throws IOException if the contents are no map of the mod's format or cannot be kept
     */
    void addMap(@NotNull MapReference map, byte @NotNull [] contents) throws IOException;
}
