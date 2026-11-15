package org.fogofwarlabs.sc4j.engine.client.text;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Holds how a {@link Font} lays out and places a line: the colour codes of the text, whether the font's kerning applies, the
 * canvas units per font unit and whether glyphs are placed on whole canvas pixels.
 * <p>
 * The pen of a styled line is in canvas units: it starts at the given position and moves by each advance times the scale, so a
 * caller that knows the canvas pixels of its own coordinates (an interface drawn scaled into a window) positions the line in
 * those pixels and draws the font at the scale, with the same arithmetic for every glyph. A style is immutable and can be shared
 * by any number of lines and fonts.
 */
public final class TextStyle {
    /**
     * The scale of text drawn one canvas unit per font unit.
     */
    public static final float UNIT_SCALE = 1;

    /**
     * The style of plain text: no colour codes, kerning on, one canvas unit per font unit, glyphs at their exact pen positions.
     */
    public static final @NotNull TextStyle DEFAULT = builder().build();

    /**
     * Builds {@link TextStyle} instances.
     */
    public static final class Builder {
        private @NotNull TextColorHook colors;
        private boolean kerning;
        private float scale;
        private boolean pixelSnapped;

        private Builder(@NotNull TextColorHook colors, boolean kerning, float scale, boolean pixelSnapped) {
            this.colors       = colors;
            this.kerning      = kerning;
            this.scale        = scale;
            this.pixelSnapped = pixelSnapped;
        }

        /**
         * Sets the colour codes of the text.
         *
         * @param colors the colour codes, or {@link TextColorHook#NONE}
         * @return this instance for chaining
         */
        public @NotNull Builder setColors(@NotNull TextColorHook colors) {
            this.colors = colors;

            return this;
        }

        /**
         * Sets whether the font adds its kerning between glyphs. What the kerning is belongs to the font; a font without
         * kerning draws the same either way.
         *
         * @param kerning whether the kerning applies
         * @return this instance for chaining
         */
        public @NotNull Builder setKerning(boolean kerning) {
            this.kerning = kerning;

            return this;
        }

        /**
         * Sets the canvas units per font unit: glyph offsets, sizes and advances are multiplied by it.
         *
         * @param scale the scale, greater than {@code 0}
         * @return this instance for chaining
         */
        public @NotNull Builder setScale(float scale) {
            this.scale = scale;

            return this;
        }

        /**
         * Sets whether each glyph is placed on whole canvas pixels: its left and top edges are rounded to the nearest whole
         * canvas unit. The pen keeps its exact position, so the rounding does not add up along a line.
         *
         * @param pixelSnapped whether glyphs are placed on whole canvas pixels
         * @return this instance for chaining
         */
        public @NotNull Builder setPixelSnapped(boolean pixelSnapped) {
            this.pixelSnapped = pixelSnapped;

            return this;
        }

        /**
         * Builds a {@link TextStyle} instance from the builder's values.
         *
         * @return the built {@link TextStyle} instance
         * @throws IllegalArgumentException if the scale is not greater than {@code 0}
         */
        public @NotNull TextStyle build() {
            if (!(this.scale > 0)) {
                throw Exceptions.newIllegalArgumentException("Invalid text scale {0}", this.scale);
            }

            return new TextStyle(this);
        }
    }

    /**
     * Creates a {@link Builder} instance with the values of {@link #DEFAULT}.
     *
     * @return the created {@link Builder} instance
     */
    public static @NotNull Builder builder() {
        return new Builder(TextColorHook.NONE, true, UNIT_SCALE, false);
    }

    /**
     * Creates a {@link Builder} instance with the values of a style, to derive a style that differs in some of them.
     *
     * @param style the style whose values the builder starts with
     * @return the created {@link Builder} instance
     */
    public static @NotNull Builder builder(@NotNull TextStyle style) {
        return new Builder(style.colors, style.kerning, style.scale, style.pixelSnapped);
    }


    private final @NotNull TextColorHook colors;
    private final boolean kerning;
    private final float scale;
    private final boolean pixelSnapped;

    private TextStyle(@NotNull Builder builder) {
        this.colors       = builder.colors;
        this.kerning      = builder.kerning;
        this.scale        = builder.scale;
        this.pixelSnapped = builder.pixelSnapped;
    }

    /**
     * @return the colour codes of the text
     */
    public @NotNull TextColorHook getColors() {
        return this.colors;
    }

    /**
     * @return {@code true} if the font adds its kerning between glyphs, {@code false} otherwise
     */
    public boolean isKerning() {
        return this.kerning;
    }

    /**
     * @return the canvas units per font unit
     */
    public float getScale() {
        return this.scale;
    }

    /**
     * @return {@code true} if each glyph is placed on whole canvas pixels, {@code false} if at its exact pen position
     */
    public boolean isPixelSnapped() {
        return this.pixelSnapped;
    }
}
