package org.fogofwarlabs.sc4j.engine.client.render;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * Draws the world of a frame as an ordered list of {@link WorldLayer}s, bottom first: for example terrain, sprites in draw
 * order, overlays and fog. A layer that cannot read a resource stops the frame's world drawing; layers that can draw around a
 * missing resource handle it themselves.
 */
public final class WorldView {
    private final @NotNull List<@NotNull WorldLayer> layers;

    /**
     * Creates a {@link WorldView} instance.
     *
     * @param layers the layers, bottom first, copied
     */
    public WorldView(@NotNull List<@NotNull WorldLayer> layers) {
        this.layers = List.copyOf(layers);
    }

    /**
     * @return the layers, bottom first
     */
    public @NotNull List<@NotNull WorldLayer> getLayers() {
        return this.layers;
    }

    /**
     * Draws every layer in order.
     *
     * @param viewport the frame's viewport
     * @throws IOException if a layer cannot read a resource it draws
     */
    public void draw(@NotNull WorldViewport viewport) throws IOException {
        for (int index = 0; index < this.layers.size(); index++) {
            this.layers.get(index).draw(viewport);
        }
    }
}
