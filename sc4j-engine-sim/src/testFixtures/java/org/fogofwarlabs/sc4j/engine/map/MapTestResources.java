package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Names the maps of the engine map format in the test resources of the engine and reads them.
 */
public final class MapTestResources {
    /**
     * The skirmish map: 48 x 48 tiles, two rock blocks in the middle, a start location and two gold mines in two opposite corners, a scout of
     * slot 3, a center location, annihilation victory and two triggers of its own.
     */
    public static final @NotNull String TWIN_ROCKS = "/maps/twin-rocks.json";

    /**
     * Reads a map resource.
     *
     * @param resource the resource path
     * @return the file contents
     * @throws IOException if the resource is missing or cannot be read
     */
    public static byte @NotNull [] read(@NotNull String resource) throws IOException {
        try (InputStream input = MapTestResources.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw Exceptions.newIOException("Missing test resource {0}", resource);
            }

            return input.readAllBytes();
        }
    }

    private MapTestResources() {
    }
}
