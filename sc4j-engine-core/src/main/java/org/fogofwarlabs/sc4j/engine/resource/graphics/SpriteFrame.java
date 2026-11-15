package org.fogofwarlabs.sc4j.engine.resource.graphics;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;

/**
 * Describes one frame of a {@link SpriteSheet}: the rectangle of stored pixels inside the sheet's frame box and where
 * those pixels start in the sheet's layers. Pixels outside the rectangle are transparent; the stored pixels are row-major,
 * {@code width} per row.
 */
public final class SpriteFrame {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final int pixelOffset;

    /**
     * Creates a {@link SpriteFrame} instance.
     *
     * @param x           the left of the stored rectangle in the frame box, in pixels
     * @param y           the top of the stored rectangle in the frame box, in pixels
     * @param width       the width of the stored rectangle, at least {@code 0}
     * @param height      the height of the stored rectangle, at least {@code 0}
     * @param pixelOffset the offset of the first stored pixel in each layer of the sheet
     * @throws IllegalArgumentException if a value is negative
     */
    public SpriteFrame(int x, int y, int width, int height, int pixelOffset) {
        if (x < 0 || y < 0 || width < 0 || height < 0 || pixelOffset < 0) {
            throw Exceptions.newIllegalArgumentException("Negative sprite frame value: {0}, {1}, {2}x{3} at {4}", x, y, width, height, pixelOffset);
        }

        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;

        this.pixelOffset = pixelOffset;
    }

    /**
     * @return the left of the stored rectangle in the frame box, in pixels
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return the top of the stored rectangle in the frame box, in pixels
     */
    public int getY() {
        return this.y;
    }

    /**
     * @return the width of the stored rectangle in pixels
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the height of the stored rectangle in pixels
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the offset of the first stored pixel in each layer of the sheet
     */
    public int getPixelOffset() {
        return this.pixelOffset;
    }
}
