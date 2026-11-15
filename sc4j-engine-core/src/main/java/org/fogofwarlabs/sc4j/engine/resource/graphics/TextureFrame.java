package org.fogofwarlabs.sc4j.engine.resource.graphics;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;

/**
 * Describes one frame of a {@link TextureSheet}: the rectangle it covers in the sheet's frame box, where that rectangle
 * is in the sheet's textures, and flags the content defines for the frame. Positions and sizes are in sheet units.
 */
public final class TextureFrame {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final int textureX;
    private final int textureY;

    private final int flags;

    /**
     * Creates a {@link TextureFrame} instance.
     *
     * @param x        the left of the frame rectangle in the frame box
     * @param y        the top of the frame rectangle in the frame box
     * @param width    the width of the frame rectangle, at least {@code 0}
     * @param height   the height of the frame rectangle, at least {@code 0}
     * @param textureX the left of the frame rectangle in the textures
     * @param textureY the top of the frame rectangle in the textures
     * @param flags    the content's flags of the frame
     * @throws IllegalArgumentException if a size is negative
     */
    public TextureFrame(int x, int y, int width, int height, int textureX, int textureY, int flags) {
        if (width < 0 || height < 0) {
            throw Exceptions.newIllegalArgumentException("A texture frame has no negative size: {0}x{1}", width, height);
        }

        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;

        this.textureX = textureX;
        this.textureY = textureY;

        this.flags = flags;
    }

    /**
     * @return the left of the frame rectangle in the frame box
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return the top of the frame rectangle in the frame box
     */
    public int getY() {
        return this.y;
    }

    /**
     * @return the width of the frame rectangle
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the height of the frame rectangle
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the left of the frame rectangle in the textures
     */
    public int getTextureX() {
        return this.textureX;
    }

    /**
     * @return the top of the frame rectangle in the textures
     */
    public int getTextureY() {
        return this.textureY;
    }

    /**
     * @return the content's flags of the frame
     */
    public int getFlags() {
        return this.flags;
    }
}
