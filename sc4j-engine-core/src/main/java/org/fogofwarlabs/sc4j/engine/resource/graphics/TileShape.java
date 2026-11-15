package org.fogofwarlabs.sc4j.engine.resource.graphics;

/**
 * Names the footprint of the pictures of a {@link TileGraphics}: how much of its rectangle one tile covers. The shape describes the
 * pictures only; where a tile is drawn on the screen is the world projection's matter.
 */
public enum TileShape {
    /**
     * The tile covers its whole rectangle.
     */
    SQUARE,
    /**
     * The tile covers the diamond whose corners are the middles of the rectangle's edges; the corners outside it are transparent.
     */
    DIAMOND
}
