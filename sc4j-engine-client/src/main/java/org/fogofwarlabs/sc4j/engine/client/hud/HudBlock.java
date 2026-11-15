package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkin;
import org.jetbrains.annotations.NotNull;

/**
 * Holds one building block of the in-game HUD (resource bar, minimap, selection panel, command card): its rectangle in framebuffer pixels,
 * its drawing on a {@link Canvas} and the input it takes. A HUD lays its blocks out with {@link #setBounds} and draws them over the world view;
 * a pointer press inside a block's rectangle belongs to the block, not to the world view behind it. The block draws in its {@link HudSkin},
 * by default {@link HudSkin#DEFAULT}.
 */
public abstract class HudBlock {
    private float x;
    private float y;
    private float width;
    private float height;

    private @NotNull HudSkin skin;

    /**
     * Creates a {@link HudBlock} instance with an empty rectangle at the origin and the default skin.
     */
    protected HudBlock() {
        this.x      = 0;
        this.y      = 0;
        this.width  = 0;
        this.height = 0;

        this.skin = HudSkin.DEFAULT;
    }

    /**
     * @return the left coordinate
     */
    public final float getX() {
        return this.x;
    }

    /**
     * @return the top coordinate
     */
    public final float getY() {
        return this.y;
    }

    /**
     * @return the width
     */
    public final float getWidth() {
        return this.width;
    }

    /**
     * @return the height
     */
    public final float getHeight() {
        return this.height;
    }

    /**
     * Sets the rectangle of the block.
     *
     * @param x      the left coordinate
     * @param y      the top coordinate
     * @param width  the width
     * @param height the height
     */
    public final void setBounds(float x, float y, float width, float height) {
        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;
    }

    /**
     * @return the skin the block draws in
     */
    public final @NotNull HudSkin getSkin() {
        return this.skin;
    }

    /**
     * Sets the skin the block draws in.
     *
     * @param skin the skin
     */
    public final void setSkin(@NotNull HudSkin skin) {
        this.skin = skin;
    }


    /**
     * Returns whether the block shows something now. The default always does; a block that shows something only at times, for example the
     * queue of a selected producer, tells its host so, and the host gives its room to the other blocks meanwhile.
     *
     * @return {@code true} if the block shows something now, {@code false} otherwise
     */
    public boolean isShown() {
        return true;
    }

    /**
     * Checks whether a framebuffer position lies in the block.
     *
     * @param pointerX the framebuffer column
     * @param pointerY the framebuffer row
     * @return {@code true} if the position lies in the rectangle, {@code false} otherwise
     */
    public final boolean contains(float pointerX, float pointerY) {
        return pointerX >= this.x && pointerY >= this.y && pointerX < this.x + this.width && pointerY < this.y + this.height;
    }

    /**
     * Draws the block.
     *
     * @param canvas the canvas, between its begin and end
     */
    public abstract void draw(@NotNull Canvas canvas);

    /**
     * Handles an input event. The default takes pointer presses and releases inside the rectangle without acting on them, so that they do not
     * reach the world view behind the block.
     *
     * @param event the event
     * @return {@code true} if the block took the event, {@code false} otherwise
     */
    public boolean handleInput(@NotNull InputEvent event) {
        return (event.getType() == InputEvent.Type.BUTTON_PRESSED || event.getType() == InputEvent.Type.BUTTON_RELEASED) &&
               this.contains(event.getX(), event.getY());
    }
}
