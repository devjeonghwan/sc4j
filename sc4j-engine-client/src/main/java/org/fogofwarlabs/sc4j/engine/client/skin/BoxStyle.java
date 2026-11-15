package org.fogofwarlabs.sc4j.engine.client.skin;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the box of a part of the interface into a rectangle: the background and frame of a panel, a HUD block, a slot or a button.
 */
public interface BoxStyle {
    /**
     * Draws the box.
     *
     * @param canvas the canvas, between its begin and end
     * @param x      the left coordinate
     * @param y      the top coordinate
     * @param width  the width
     * @param height the height
     */
    void draw(@NotNull Canvas canvas, float x, float y, float width, float height);
}
