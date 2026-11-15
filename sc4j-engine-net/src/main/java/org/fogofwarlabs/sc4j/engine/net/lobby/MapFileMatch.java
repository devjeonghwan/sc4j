package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Tells whether a file of the map folders holds a wanted map ({@link MapFolders#findFile}), by its contents, not its name: the engine's lobby
 * maps compare the digest of a {@link org.fogofwarlabs.sc4j.engine.game.MapReference}, a mod its own size and checksum.
 */
@FunctionalInterface
public interface MapFileMatch {
    /**
     * Checks whether a file holds the wanted map.
     *
     * @param file the file
     * @return {@code true} if the file holds the wanted map, {@code false} otherwise
     * @throws IOException if the file cannot be read; the search then skips it
     */
    boolean matches(@NotNull Path file) throws IOException;
}
