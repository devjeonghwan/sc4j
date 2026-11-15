package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkin;
import org.jetbrains.annotations.NotNull;

/**
 * Shows the plate of the bottom console in a HUD rectangle: the box behind the console's blocks (minimap, selection panel, command card and a
 * queue panel), seen in the gaps between them. A pointer press on the plate belongs to the HUD, not to the world view behind it. The plate
 * draws in the console box of its {@link HudSkin}.
 */
public final class ConsolePlate extends HudBlock {
    /**
     * Creates a {@link ConsolePlate} instance with an empty rectangle at the origin and the default skin.
     */
    public ConsolePlate() {
    }

    /**
     * Draws the block: the console box over the rectangle.
     *
     * @param canvas the canvas, between its begin and end
     */
    @Override
    public void draw(@NotNull Canvas canvas) {
        this.getSkin().getConsoleBackground().draw(canvas, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }
}
