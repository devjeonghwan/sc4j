package org.fogofwarlabs.sc4j.engine.client.text;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.jetbrains.annotations.NotNull;

/**
 * Measures and draws one line of text in one typeface and size on a {@link Canvas}.
 * <p>
 * A line holds no line breaks; {@link TextLayout} splits text into lines. The pen starts at the given left edge on the line
 * top and moves right by each character's advance. Characters the font has no glyph for draw nothing and do not move the pen.
 * <p>
 * A {@link TextStyle} says how a line is drawn: its colour codes, whether the font's kerning applies, the canvas units per font
 * unit and whether glyphs are placed on whole canvas pixels. The forms without a style use the font's
 * {@linkplain #getDefaultStyle() default style}.
 */
public interface Font {
    /**
     * @return the distance between the tops of two lines in font units (times the style's scale on the canvas)
     */
    int getLineHeight();

    /**
     * @return the style of the forms without one: the font's own colour codes and kerning at one canvas unit per font unit
     */
    @NotNull TextStyle getDefaultStyle();


    /**
     * Measures how far the pen moves over a line in the {@linkplain #getDefaultStyle() default style}.
     *
     * @param text the line, without line breaks
     * @return the advance of the line in canvas units
     */
    default float measure(@NotNull CharSequence text) {
        return this.measure(text, this.getDefaultStyle());
    }

    /**
     * Measures how far the pen moves over a line in a style.
     *
     * @param text  the line, without line breaks
     * @param style the style: its colour codes do not advance, its kerning and scale apply
     * @return the advance of the line in canvas units
     */
    float measure(@NotNull CharSequence text, @NotNull TextStyle style);

    /**
     * Draws a line in the {@linkplain #getDefaultStyle() default style}.
     *
     * @param canvas the canvas that draws, between its {@link Canvas#begin} and {@link Canvas#end}
     * @param text   the line, without line breaks
     * @param x      the left edge of the pen
     * @param y      the line top
     * @param color  the packed {@code 0xRRGGBBAA} colour the line starts in
     * @return the pen position after the line
     */
    default float draw(@NotNull Canvas canvas, @NotNull CharSequence text, float x, float y, int color) {
        return this.draw(canvas, text, x, y, color, this.getDefaultStyle());
    }

    /**
     * Draws a line in a style. The pen starts at {@code x} and moves by each advance times the style's scale, in canvas units.
     *
     * @param canvas the canvas that draws, between its {@link Canvas#begin} and {@link Canvas#end}
     * @param text   the line, without line breaks
     * @param x      the left edge of the pen in canvas units
     * @param y      the line top in canvas units
     * @param color  the packed {@code 0xRRGGBBAA} colour the line starts in
     * @param style  the style of the line
     * @return the pen position after the line
     */
    float draw(@NotNull Canvas canvas, @NotNull CharSequence text, float x, float y, int color, @NotNull TextStyle style);
}
