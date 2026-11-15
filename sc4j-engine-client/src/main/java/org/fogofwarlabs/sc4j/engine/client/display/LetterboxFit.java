package org.fogofwarlabs.sc4j.engine.client.display;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Fits an interface designed for a fixed area (for example a 4:3 area) into a logical area of another size: the design area keeps its aspect
 * ratio and is as large as the logical area allows; the rest of the logical area is left over as bars. The design area is centred, or placed
 * by an alignment on each axis ({@code 0} at the area's start, {@code 1} at its end), for example standing on the bottom edge. A client that
 * lays its interface out in a fixed area makes the fit on the logical surface ({@link #of(UiSurface, float, float, float, float)}), draws
 * through it and maps the pointer back with {@link #toDesignX} and {@link #toDesignY}; {@link #toFramebuffer} gives the same fit in the
 * surface's framebuffer pixels.
 */
public final class LetterboxFit {
    /**
     * The alignment that places the design area at the start of an axis: the left or top edge.
     */
    public static final float START = 0f;
    /**
     * The alignment that centres the design area on an axis.
     */
    public static final float CENTER = 0.5f;
    /**
     * The alignment that places the design area at the end of an axis: the right or bottom edge.
     */
    public static final float END = 1f;

    /**
     * Creates a {@link LetterboxFit} instance that fits a design area into the whole logical area of an interface surface. The logical area is
     * taken at its exact size, the framebuffer divided by the display scale without rounding to whole units, so that one design unit covers the
     * same framebuffer pixels across and down and the fit in framebuffer pixels ({@link #toFramebuffer}) is the fit to the framebuffer.
     *
     * @param surface             the interface surface
     * @param designWidth         the width of the design area
     * @param designHeight        the height of the design area
     * @param horizontalAlignment the horizontal alignment, from {@link #START} (left) to {@link #END} (right)
     * @param verticalAlignment   the vertical alignment, from {@link #START} (top) to {@link #END} (bottom)
     * @return the created {@link LetterboxFit} instance, in logical units
     * @throws IllegalArgumentException if a dimension is not positive or an alignment is outside {@link #START} to {@link #END}
     */
    public static @NotNull LetterboxFit of(@NotNull UiSurface surface, float designWidth, float designHeight,
                                           float horizontalAlignment, float verticalAlignment) {
        float factor = surface.getScale().getScale();

        return of(surface.getFramebufferWidth() / factor, surface.getFramebufferHeight() / factor, designWidth, designHeight,
                  horizontalAlignment, verticalAlignment);
    }

    /**
     * Creates a {@link LetterboxFit} instance that centres the design area.
     *
     * @param areaWidth    the logical area width
     * @param areaHeight   the logical area height
     * @param designWidth  the width of the design area
     * @param designHeight the height of the design area
     * @return the created {@link LetterboxFit} instance
     * @throws IllegalArgumentException if a dimension is not positive
     */
    public static @NotNull LetterboxFit of(float areaWidth, float areaHeight, float designWidth, float designHeight) {
        return of(areaWidth, areaHeight, designWidth, designHeight, CENTER, CENTER);
    }

    /**
     * Creates a {@link LetterboxFit} instance that places the design area by an alignment on each axis: the share of the bars on that axis
     * that lies before the design area.
     *
     * @param areaWidth           the logical area width
     * @param areaHeight          the logical area height
     * @param designWidth         the width of the design area
     * @param designHeight        the height of the design area
     * @param horizontalAlignment the horizontal alignment, from {@link #START} (left) to {@link #END} (right)
     * @param verticalAlignment   the vertical alignment, from {@link #START} (top) to {@link #END} (bottom)
     * @return the created {@link LetterboxFit} instance
     * @throws IllegalArgumentException if a dimension is not positive or an alignment is outside {@link #START} to {@link #END}
     */
    public static @NotNull LetterboxFit of(float areaWidth, float areaHeight, float designWidth, float designHeight,
                                           float horizontalAlignment, float verticalAlignment) {
        if (!(areaWidth > 0) || !(areaHeight > 0) || !(designWidth > 0) || !(designHeight > 0)) {
            throw Exceptions.newIllegalArgumentException("Invalid area {0}x{1} or design area {2}x{3}", areaWidth, areaHeight,
                                                         designWidth, designHeight);
        }

        if (!(horizontalAlignment >= START && horizontalAlignment <= END) || !(verticalAlignment >= START && verticalAlignment <= END)) {
            throw Exceptions.newIllegalArgumentException("Invalid alignment {0}, {1}", horizontalAlignment, verticalAlignment);
        }

        float scale = Math.min(areaWidth / designWidth, areaHeight / designHeight);

        return new LetterboxFit(scale, (areaWidth - designWidth * scale) * horizontalAlignment,
                                (areaHeight - designHeight * scale) * verticalAlignment);
    }


    private final float scale;
    private final float left;
    private final float top;

    private LetterboxFit(float scale, float left, float top) {
        this.scale = scale;
        this.left  = left;
        this.top   = top;
    }

    /**
     * @return the logical units per design unit
     */
    public float getScale() {
        return this.scale;
    }

    /**
     * @return the left edge of the fitted design area in logical units
     */
    public float getLeft() {
        return this.left;
    }

    /**
     * @return the top edge of the fitted design area in logical units
     */
    public float getTop() {
        return this.top;
    }

    /**
     * Returns this fit, made on the logical area of a surface ({@link #of(UiSurface, float, float, float, float)}), in the surface's framebuffer
     * pixels: its scale and edges times the display scale. At a display scale of {@code 1} the fit is returned unchanged.
     *
     * @param surface the interface surface whose logical area the fit was made on
     * @return the fit in framebuffer pixels per design unit
     */
    public @NotNull LetterboxFit toFramebuffer(@NotNull UiSurface surface) {
        float factor = surface.getScale().getScale();

        if (factor == 1) {
            return this;
        }

        return new LetterboxFit(this.scale * factor, this.left * factor, this.top * factor);
    }


    /**
     * Converts a design column to a logical column.
     *
     * @param designX the column in design units
     * @return the column in logical units
     */
    public float toLogicalX(float designX) {
        return this.left + designX * this.scale;
    }

    /**
     * Converts a design row to a logical row.
     *
     * @param designY the row in design units
     * @return the row in logical units
     */
    public float toLogicalY(float designY) {
        return this.top + designY * this.scale;
    }

    /**
     * Converts a logical column to a design column; columns in the bars fall outside the design area.
     *
     * @param logicalX the column in logical units
     * @return the column in design units
     */
    public float toDesignX(float logicalX) {
        return (logicalX - this.left) / this.scale;
    }

    /**
     * Converts a logical row to a design row; rows in the bars fall outside the design area.
     *
     * @param logicalY the row in logical units
     * @return the row in design units
     */
    public float toDesignY(float logicalY) {
        return (logicalY - this.top) / this.scale;
    }
}
