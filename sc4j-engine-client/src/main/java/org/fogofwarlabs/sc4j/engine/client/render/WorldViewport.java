package org.fogofwarlabs.sc4j.engine.client.render;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.jetbrains.annotations.NotNull;

/**
 * Describes how the world is shown in a frame: the client's {@link WorldProjection}, the size of the screen area in pixels, the position of
 * its top-left corner on the projected plane (the camera) and the zoom (screen pixels per projected pixel). A world position reaches the
 * screen through the projection, then the camera and the zoom ({@link #worldToScreenX}); with the top-down square projection the projected
 * plane is the world plane. A client keeps one and updates it every frame ({@link #set}), so drawing a frame allocates nothing.
 */
public final class WorldViewport {
    private final @NotNull WorldProjection projection;

    private int width;
    private int height;
    private float cameraX;
    private float cameraY;
    private float zoom;

    /**
     * Creates a {@link WorldViewport} instance of one pixel at the projected origin without zoom.
     *
     * @param projection the projection of the client's world view
     */
    public WorldViewport(@NotNull WorldProjection projection) {
        this.projection = projection;

        this.width   = 1;
        this.height  = 1;
        this.cameraX = 0;
        this.cameraY = 0;
        this.zoom    = 1;
    }

    /**
     * @return the projection of the client's world view
     */
    public @NotNull WorldProjection getProjection() {
        return this.projection;
    }

    /**
     * @return the width of the screen area in pixels
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the height of the screen area in pixels
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the projected x shown at the left edge
     */
    public float getCameraX() {
        return this.cameraX;
    }

    /**
     * @return the projected y shown at the top edge
     */
    public float getCameraY() {
        return this.cameraY;
    }

    /**
     * @return the screen pixels per projected pixel
     */
    public float getZoom() {
        return this.zoom;
    }

    /**
     * Sets the viewport of the next frame.
     *
     * @param width   the width of the screen area in pixels
     * @param height  the height of the screen area in pixels
     * @param cameraX the projected x shown at the left edge
     * @param cameraY the projected y shown at the top edge
     * @param zoom    the screen pixels per projected pixel
     * @throws IllegalArgumentException if a size or the zoom is not positive
     */
    public void set(int width, int height, float cameraX, float cameraY, float zoom) {
        if (width <= 0 || height <= 0 || !(zoom > 0)) {
            throw Exceptions.newIllegalArgumentException("Invalid viewport {0}x{1} at zoom {2}", width, height, zoom);
        }

        this.width   = width;
        this.height  = height;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.zoom    = zoom;
    }

    /**
     * @return the projected x shown at the right edge
     */
    public float getCameraRight() {
        return this.cameraX + this.width / this.zoom;
    }

    /**
     * @return the projected y shown at the bottom edge
     */
    public float getCameraBottom() {
        return this.cameraY + this.height / this.zoom;
    }


    /**
     * Converts an x of the projected plane to a screen x of the viewport.
     *
     * @param projectedX the projected x
     * @return the screen x
     */
    public float toScreenX(float projectedX) {
        return (projectedX - this.cameraX) * this.zoom;
    }

    /**
     * Converts a y of the projected plane to a screen y of the viewport.
     *
     * @param projectedY the projected y
     * @return the screen y
     */
    public float toScreenY(float projectedY) {
        return (projectedY - this.cameraY) * this.zoom;
    }

    /**
     * Converts a world position to a screen x of the viewport through the projection.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the screen x
     */
    public float worldToScreenX(float worldX, float worldY) {
        return this.toScreenX(this.projection.toProjectedX(worldX, worldY));
    }

    /**
     * Converts a world position to a screen y of the viewport through the projection.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the screen y
     */
    public float worldToScreenY(float worldX, float worldY) {
        return this.toScreenY(this.projection.toProjectedY(worldX, worldY));
    }

    /**
     * Writes the screen outline of a world rectangle: the projected outline of {@link WorldProjection#outlineFootprint} moved and scaled to
     * the viewport.
     *
     * @param left    the world x of the left edge
     * @param top     the world y of the top edge
     * @param right   the world x of the right edge
     * @param bottom  the world y of the bottom edge
     * @param corners the {@value WorldProjection#OUTLINE_LENGTH} values written from index {@code 0}, x and y of each corner
     * @throws IndexOutOfBoundsException if the array is shorter than {@value WorldProjection#OUTLINE_LENGTH}
     */
    public void outlineOnScreen(float left, float top, float right, float bottom, float @NotNull [] corners) {
        this.projection.outlineFootprint(left, top, right, bottom, corners);

        for (int index = 0; index < WorldProjection.OUTLINE_LENGTH; index += 2) {
            corners[index]     = this.toScreenX(corners[index]);
            corners[index + 1] = this.toScreenY(corners[index + 1]);
        }
    }
}
