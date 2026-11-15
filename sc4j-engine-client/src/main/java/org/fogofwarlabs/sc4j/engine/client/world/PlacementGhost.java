package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the picture of a building that is not placed yet where the {@link PlacementLayer} snapped its site, for example the
 * {@link UnitSpriteLayer}'s standing frame of the type.
 */
@FunctionalInterface
public interface PlacementGhost {
    /**
     * Draws the ghost of a building type at a world position.
     *
     * @param viewport the frame's viewport
     * @param type     the building type
     * @param x        the world x of the building's position, the centre of its footprint
     * @param y        the world y of the building's position, the centre of its footprint
     * @param color    the packed {@code 0xRRGGBBAA} colour the picture is multiplied by, its alpha the ghost's opacity
     */
    void drawGhost(@NotNull WorldViewport viewport, @NotNull UnitType type, int x, int y, int color);
}
