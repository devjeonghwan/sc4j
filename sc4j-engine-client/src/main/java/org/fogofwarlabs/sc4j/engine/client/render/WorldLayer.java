package org.fogofwarlabs.sc4j.engine.client.render;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Draws one layer of the world view (terrain, sprites, overlays, fog), in the order its {@link WorldView} holds the layers.
 */
@FunctionalInterface
public interface WorldLayer {
    /**
     * Draws the layer for a frame.
     *
     * @param viewport the frame's viewport
     * @throws IOException if a resource the layer draws cannot be read
     */
    void draw(@NotNull WorldViewport viewport) throws IOException;
}
