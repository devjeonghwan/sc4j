package org.fogofwarlabs.sc4j.engine.client.skin;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.jetbrains.annotations.NotNull;

/**
 * Draws a box as a filled rectangle with an optional outline inside it, the engine default look: the fill first, then the outline. A fully
 * transparent fill or outline, or an outline without thickness, is not drawn.
 */
public final class ColorBoxStyle implements BoxStyle {
    /**
     * The style that draws nothing.
     */
    public static final @NotNull ColorBoxStyle NONE = new ColorBoxStyle(0);

    private static final int ALPHA_MASK = 0xFF;

    private final int fillColor;
    private final int frameColor;
    private final float frameThickness;

    /**
     * Creates a {@link ColorBoxStyle} instance without outline.
     *
     * @param fillColor the packed {@code 0xRRGGBBAA} fill
     */
    public ColorBoxStyle(int fillColor) {
        this(fillColor, 0, 0);
    }

    /**
     * Creates a {@link ColorBoxStyle} instance.
     *
     * @param fillColor      the packed {@code 0xRRGGBBAA} fill
     * @param frameColor     the packed {@code 0xRRGGBBAA} outline
     * @param frameThickness the outline thickness, {@code 0} for none
     */
    public ColorBoxStyle(int fillColor, int frameColor, float frameThickness) {
        this.fillColor      = fillColor;
        this.frameColor     = frameColor;
        this.frameThickness = frameThickness;
    }

    /**
     * @return the packed {@code 0xRRGGBBAA} fill
     */
    public int getFillColor() {
        return this.fillColor;
    }

    /**
     * @return the packed {@code 0xRRGGBBAA} outline
     */
    public int getFrameColor() {
        return this.frameColor;
    }

    /**
     * @return the outline thickness, {@code 0} for none
     */
    public float getFrameThickness() {
        return this.frameThickness;
    }


    /**
     * Draws the box: the fill and the outline inside the rectangle.
     *
     * @param canvas the canvas, between its begin and end
     * @param x      the left coordinate
     * @param y      the top coordinate
     * @param width  the width
     * @param height the height
     */
    @Override
    public void draw(@NotNull Canvas canvas, float x, float y, float width, float height) {
        if ((this.fillColor & ALPHA_MASK) != 0) {
            canvas.fillRectangle(x, y, width, height, this.fillColor);
        }

        if (this.frameThickness > 0 && (this.frameColor & ALPHA_MASK) != 0) {
            canvas.drawRectangle(x, y, width, height, this.frameThickness, this.frameColor);
        }
    }
}
