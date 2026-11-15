package org.fogofwarlabs.sc4j.engine.client.display;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the interface surface of one frame: the window's framebuffer in physical pixels and the same area in logical units, the units
 * a client lays its interface out in and receives the pointer in. The logical size is the framebuffer size divided by the
 * {@link DisplayScale}, rounded, at least one unit; the logical area covers the whole framebuffer, so each axis has its own pixel scale
 * (framebuffer pixels per logical unit), equal to the display scale up to the rounding.
 * <p>
 * The engine lays interfaces out in at least {@value #MINIMUM_WIDTH} by {@value #MINIMUM_HEIGHT} logical units: a surface whose logical
 * area is smaller is fitted ({@link #fitTo}) at a lower display scale, so that every layout made for the minimum area fits.
 */
public final class UiSurface {
    /**
     * The smallest logical width the engine lays an interface out in.
     */
    public static final int MINIMUM_WIDTH = 640;
    /**
     * The smallest logical height the engine lays an interface out in.
     */
    public static final int MINIMUM_HEIGHT = 360;

    /**
     * Creates a {@link UiSurface} instance for a framebuffer at a display scale.
     *
     * @param framebufferWidth  the framebuffer width in pixels
     * @param framebufferHeight the framebuffer height in pixels
     * @param scale             the display scale
     * @return the created {@link UiSurface} instance
     * @throws IllegalArgumentException if a framebuffer dimension is not positive
     */
    public static @NotNull UiSurface of(int framebufferWidth, int framebufferHeight, @NotNull DisplayScale scale) {
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            throw Exceptions.newIllegalArgumentException("Invalid framebuffer size {0}x{1}", framebufferWidth, framebufferHeight);
        }

        float factor = scale.getScale();

        return new UiSurface(framebufferWidth, framebufferHeight, scale,
                             Math.max(1, Math.round(framebufferWidth / factor)), Math.max(1, Math.round(framebufferHeight / factor)));
    }


    private final int framebufferWidth;
    private final int framebufferHeight;
    private final @NotNull DisplayScale scale;
    private final int width;
    private final int height;

    private UiSurface(int framebufferWidth, int framebufferHeight, @NotNull DisplayScale scale, int width, int height) {
        this.framebufferWidth  = framebufferWidth;
        this.framebufferHeight = framebufferHeight;
        this.scale             = scale;
        this.width             = width;
        this.height            = height;
    }

    /**
     * @return the framebuffer width in pixels
     */
    public int getFramebufferWidth() {
        return this.framebufferWidth;
    }

    /**
     * @return the framebuffer height in pixels
     */
    public int getFramebufferHeight() {
        return this.framebufferHeight;
    }

    /**
     * @return the display scale the surface was made with
     */
    public @NotNull DisplayScale getScale() {
        return this.scale;
    }

    /**
     * @return the width in logical units
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the height in logical units
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the framebuffer pixels per logical unit across
     */
    public float getPixelScaleX() {
        return this.framebufferWidth / (float) this.width;
    }

    /**
     * @return the framebuffer pixels per logical unit down
     */
    public float getPixelScaleY() {
        return this.framebufferHeight / (float) this.height;
    }

    /**
     * @return {@code true} if one logical unit is one framebuffer pixel on both axes, {@code false} otherwise
     */
    public boolean isPixelExact() {
        return this.width == this.framebufferWidth && this.height == this.framebufferHeight;
    }

    /**
     * Converts a framebuffer column to a logical column.
     *
     * @param framebufferX the column in framebuffer pixels
     * @return the column in logical units
     */
    public float toLogicalX(float framebufferX) {
        return framebufferX * this.width / this.framebufferWidth;
    }

    /**
     * Converts a framebuffer row to a logical row.
     *
     * @param framebufferY the row in framebuffer pixels
     * @return the row in logical units
     */
    public float toLogicalY(float framebufferY) {
        return framebufferY * this.height / this.framebufferHeight;
    }

    /**
     * Converts a logical column to a framebuffer column.
     *
     * @param logicalX the column in logical units
     * @return the column in framebuffer pixels
     */
    public float toFramebufferX(float logicalX) {
        return logicalX * this.framebufferWidth / this.width;
    }

    /**
     * Converts a logical row to a framebuffer row.
     *
     * @param logicalY the row in logical units
     * @return the row in framebuffer pixels
     */
    public float toFramebufferY(float logicalY) {
        return logicalY * this.framebufferHeight / this.height;
    }

    /**
     * Fits the surface to a logical area: returns a surface of the same framebuffer whose logical area is at least the given size. When this
     * surface's logical area is large enough on both axes, it is returned; otherwise the display scale is lowered (never raised) until the
     * smaller axis fits, so the content keeps its proportions and shows whole, at fewer framebuffer pixels per logical unit.
     *
     * @param width  the logical width needed
     * @param height the logical height needed
     * @return this surface, or the created {@link UiSurface} instance at the lowered display scale
     */
    public @NotNull UiSurface fitTo(int width, int height) {
        if (this.width >= width && this.height >= height) {
            return this;
        }

        float fitted = Math.min(this.framebufferWidth / (float) width, this.framebufferHeight / (float) height);
        float factor = fitted / this.scale.getScale();

        return of(this.framebufferWidth, this.framebufferHeight, DisplayScale.of(this.scale.getContentScale(), this.scale.getUserScale() * factor));
    }

    /**
     * @return the surface as text, for example {@code "1280x720 units on 2560x1440 pixels"}
     */
    @Override
    public @NotNull String toString() {
        return this.width + "x" + this.height + " units on " + this.framebufferWidth + "x" + this.framebufferHeight + " pixels";
    }
}
