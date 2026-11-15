package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkin;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Holds the lines of a HUD tooltip and draws them in the skin's tooltip box ({@link HudSkin#getTooltipBackground}, padded by
 * {@link HudSkin#getTooltipPadding}) over a HUD rectangle, for example the hovered command button: the box stands {@value #GAP} logical units
 * above the rectangle's top, its left side at the rectangle's left side, moved left as far as it needs to stay on the canvas. The HUD blocks
 * share it: a block fills the lines of what the pointer hovers and draws the tooltip after its own drawing. A line keeps the text it is given
 * until the tooltip is cleared, so a reused text buffer must not change before the drawing.
 */
public final class Tooltip {
    /**
     * The gap between the tooltip box and the rectangle under it, in logical units.
     */
    public static final float GAP = 4;

    private static final int INITIAL_LINES = 4;

    private final @NotNull Font font;

    private final @NotNull List<@NotNull CharSequence> texts;
    private int @NotNull [] colors;

    /**
     * Creates a {@link Tooltip} instance without lines.
     *
     * @param font the font of the lines
     */
    public Tooltip(@NotNull Font font) {
        this.font = font;

        this.texts  = new ArrayList<>(INITIAL_LINES);
        this.colors = new int[INITIAL_LINES];
    }

    /**
     * Removes every line.
     */
    public void clear() {
        this.texts.clear();
    }

    /**
     * Adds a line under the others; an empty text adds nothing.
     *
     * @param text  the text, kept until the tooltip is cleared
     * @param color the packed {@code 0xRRGGBBAA} colour of the text
     */
    public void addLine(@NotNull CharSequence text, int color) {
        if (text.isEmpty()) {
            return;
        }

        int line = this.texts.size();

        if (line == this.colors.length) {
            this.colors = Arrays.copyOf(this.colors, line * 2);
        }

        this.texts.add(text);
        this.colors[line] = color;
    }

    /**
     * Draws the lines in the skin's tooltip box over a rectangle; a tooltip without lines draws nothing.
     *
     * @param canvas the canvas, between its begin and end
     * @param skin   the skin whose tooltip box the lines stand in
     * @param left   the left coordinate of the rectangle under the tooltip
     * @param top    the top coordinate of the rectangle under the tooltip
     */
    public void draw(@NotNull Canvas canvas, @NotNull HudSkin skin, float left, float top) {
        if (this.texts.isEmpty()) {
            return;
        }

        float padding = skin.getTooltipPadding();
        float textWidth = 0;

        for (int line = 0; line < this.texts.size(); line++) {
            textWidth = Math.max(textWidth, this.font.measure(this.texts.get(line)));
        }

        float width = textWidth + 2 * padding;
        float height = this.texts.size() * this.font.getLineHeight() + 2 * padding;
        float x = Math.max(Math.min(left, canvas.getWidth() - width), 0);
        float y = top - GAP - height;

        skin.getTooltipBackground().draw(canvas, x, y, width, height);

        for (int line = 0; line < this.texts.size(); line++) {
            this.font.draw(canvas, this.texts.get(line), x + padding, y + padding + line * this.font.getLineHeight(), this.colors[line]);
        }
    }
}
