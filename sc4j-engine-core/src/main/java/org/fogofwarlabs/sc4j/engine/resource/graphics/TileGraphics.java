package org.fogofwarlabs.sc4j.engine.resource.graphics;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the pictures of the terrain tiles of a tileset: every tile has the same size and the same footprint ({@link TileShape}), and
 * tile {@code n} is stored row-major at pixel offset {@code n * tileWidth * tileHeight} of one layer. Which tile a map cell shows is
 * decided by the map content.
 */
public final class TileGraphics {
    private final int tileWidth;
    private final int tileHeight;
    private final @NotNull TileShape shape;
    private final int tileCount;

    private final @NotNull PixelLayer pixels;

    /**
     * Creates a {@link TileGraphics} instance.
     *
     * @param tileWidth  the width of a tile in pixels, at least {@code 1}
     * @param tileHeight the height of a tile in pixels, at least {@code 1}
     * @param shape      the footprint of every tile in its rectangle
     * @param pixels     the pixels of every tile, indexed or RGBA, a whole number of tiles
     * @throws IllegalArgumentException if a tile is empty, the layer is an alpha layer or it does not hold whole tiles
     */
    public TileGraphics(int tileWidth, int tileHeight, @NotNull TileShape shape, @NotNull PixelLayer pixels) {
        if (tileWidth < 1 || tileHeight < 1) {
            throw Exceptions.newIllegalArgumentException("A tile must not be empty: {0}x{1}", tileWidth, tileHeight);
        }

        if (pixels.getFormat() == PixelFormat.ALPHA_8) {
            throw Exceptions.newIllegalArgumentException("The tile layer must be indexed or RGBA.");
        }

        if (pixels.getLength() % (tileWidth * tileHeight) != 0) {
            throw Exceptions.newIllegalArgumentException("The tile layer of {0} pixels holds no whole number of {1}x{2} tiles.",
                                                         pixels.getLength(), tileWidth, tileHeight);
        }

        this.tileWidth  = tileWidth;
        this.tileHeight = tileHeight;
        this.shape      = shape;
        this.tileCount  = pixels.getLength() / (tileWidth * tileHeight);

        this.pixels = pixels;
    }

    /**
     * @return the width of a tile in pixels
     */
    public int getTileWidth() {
        return this.tileWidth;
    }

    /**
     * @return the height of a tile in pixels
     */
    public int getTileHeight() {
        return this.tileHeight;
    }

    /**
     * @return the footprint of every tile in its rectangle
     */
    public @NotNull TileShape getShape() {
        return this.shape;
    }

    /**
     * @return the number of tiles
     */
    public int getTileCount() {
        return this.tileCount;
    }

    /**
     * @return the pixels of every tile
     */
    public @NotNull PixelLayer getPixels() {
        return this.pixels;
    }

    /**
     * Returns where a tile starts in the pixel layer.
     *
     * @param tileIndex the tile index, {@code 0} up to {@link #getTileCount()} minus one
     * @return the pixel offset of the tile's top-left pixel
     * @throws IndexOutOfBoundsException if the index names no tile
     */
    public int getTileOffset(int tileIndex) {
        if (tileIndex < 0 || tileIndex >= this.tileCount) {
            throw Exceptions.newIndexOutOfBoundsException("No tile {0}; the tileset has {1}.", tileIndex, this.tileCount);
        }

        return tileIndex * this.tileWidth * this.tileHeight;
    }
}
