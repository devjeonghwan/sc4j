package org.fogofwarlabs.sc4j.engine.resource.text;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;

/**
 * Describes the picture of one character of a {@link FontFace}: how far the pen advances, where the stored pixels sit
 * relative to the pen position on the line top, and where they are in the font's glyph image (a rectangle of
 * {@code width} by {@code height} pixels at {@code imageX}, {@code imageY}).
 */
public final class FontGlyph {
    private final int codePoint;
    private final int advance;

    private final int offsetX;
    private final int offsetY;
    private final int width;
    private final int height;

    private final int imageX;
    private final int imageY;

    /**
     * Creates a {@link FontGlyph} instance.
     *
     * @param codePoint the Unicode code point of the character
     * @param advance   how far the pen moves after the character, in pixels
     * @param offsetX   the left of the stored pixels relative to the pen position
     * @param offsetY   the top of the stored pixels relative to the line top
     * @param width     the width of the stored pixels, at least {@code 0}
     * @param height    the height of the stored pixels, at least {@code 0}
     * @param imageX    the left of the stored pixels in the font's glyph image
     * @param imageY    the top of the stored pixels in the font's glyph image
     * @throws IllegalArgumentException if the code point, a size or an image position is negative
     */
    public FontGlyph(int codePoint, int advance, int offsetX, int offsetY, int width, int height, int imageX, int imageY) {
        if (codePoint < 0 || width < 0 || height < 0 || imageX < 0 || imageY < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid glyph {0}: {1}x{2} at {3}, {4}", codePoint, width, height, imageX, imageY);
        }

        this.codePoint = codePoint;
        this.advance   = advance;

        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width   = width;
        this.height  = height;

        this.imageX = imageX;
        this.imageY = imageY;
    }

    /**
     * @return the Unicode code point of the character
     */
    public int getCodePoint() {
        return this.codePoint;
    }

    /**
     * @return how far the pen moves after the character, in pixels
     */
    public int getAdvance() {
        return this.advance;
    }

    /**
     * @return the left of the stored pixels relative to the pen position
     */
    public int getOffsetX() {
        return this.offsetX;
    }

    /**
     * @return the top of the stored pixels relative to the line top
     */
    public int getOffsetY() {
        return this.offsetY;
    }

    /**
     * @return the width of the stored pixels
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the height of the stored pixels
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the left of the stored pixels in the font's glyph image
     */
    public int getImageX() {
        return this.imageX;
    }

    /**
     * @return the top of the stored pixels in the font's glyph image
     */
    public int getImageY() {
        return this.imageY;
    }
}
