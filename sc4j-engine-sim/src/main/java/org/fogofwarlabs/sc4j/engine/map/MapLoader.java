package org.fogofwarlabs.sc4j.engine.map;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Reads the maps of a mod's map format into the engine map model.
 */
@FunctionalInterface
public interface MapLoader {
    /**
     * Loads a map file.
     *
     * @param file the map file
     * @return the loaded map
     * @throws IOException if the file cannot be read or is not a map of the format
     */
    @NotNull MapModel load(@NotNull Path file) throws IOException;
}
