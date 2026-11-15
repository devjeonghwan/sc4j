package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.jetbrains.annotations.NotNull;

/**
 * Draws status bars (hit points, damage layers, progress) of the engine default world view and HUD: a filled part of the bar's length for the value
 * and the empty colour for the rest. Colours are packed {@code 0xRRGGBBAA}.
 */
public final class StatusBarUtils {
    /**
     * The default colour of hit points.
     */
    public static final int HIT_POINT_COLOR = 0x30C030FF;
    /**
     * The colour of hit points of a wounded unit, at most {@value #WOUNDED_PERCENT} percent of its full hit points.
     */
    public static final int WOUNDED_COLOR = 0xE0C030FF;
    /**
     * The colour of hit points of a badly wounded unit, at most {@value #CRITICAL_PERCENT} percent of its full hit points.
     */
    public static final int CRITICAL_COLOR = 0xE03828FF;
    /**
     * The share in percent of the full hit points at and below which the hit points show wounded.
     */
    public static final int WOUNDED_PERCENT = 66;
    /**
     * The share in percent of the full hit points at and below which the hit points show badly wounded.
     */
    public static final int CRITICAL_PERCENT = 33;
    /**
     * The default colour of damage layers.
     */
    public static final int DAMAGE_LAYER_COLOR = 0x3080E0FF;
    /**
     * The default colour of the empty part.
     */
    public static final int EMPTY_COLOR = 0x202020C0;

    private static final int PERCENT = 100;

    /**
     * Finds the colour of hit points by their share of the full hit points: {@link #HIT_POINT_COLOR}, {@link #WOUNDED_COLOR} at and below
     * {@value #WOUNDED_PERCENT} percent, {@link #CRITICAL_COLOR} at and below {@value #CRITICAL_PERCENT} percent.
     *
     * @param hitPoints        the hit points
     * @param maximumHitPoints the full hit points
     * @return the packed colour
     */
    public static int findHitPointColor(int hitPoints, int maximumHitPoints) {
        long percent = maximumHitPoints > 0 ? (long) hitPoints * PERCENT / maximumHitPoints : PERCENT;

        return percent <= CRITICAL_PERCENT ? CRITICAL_COLOR : percent <= WOUNDED_PERCENT ? WOUNDED_COLOR : HIT_POINT_COLOR;
    }

    /**
     * Draws one bar.
     *
     * @param canvas     the canvas
     * @param x          the left coordinate
     * @param y          the top coordinate
     * @param width      the full width
     * @param height     the height
     * @param value      the value, drawn as the filled part
     * @param maximum    the value of the full bar; a bar with no positive maximum draws nothing
     * @param color      the colour of the filled part
     * @param emptyColor the colour of the rest
     */
    public static void drawBar(@NotNull Canvas canvas, float x, float y, float width, float height,
                               int value, int maximum,
                               int color, int emptyColor) {
        if (maximum <= 0) {
            return;
        }

        float filled = width * Math.clamp(value, 0, maximum) / maximum;

        canvas.fillRectangle(x, y, width, height, emptyColor);

        if (filled > 0) {
            canvas.fillRectangle(x, y, filled, height, color);
        }
    }

    private StatusBarUtils() {
    }
}
