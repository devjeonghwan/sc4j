package org.fogofwarlabs.sc4j.engine.client.skin;

import org.jetbrains.annotations.NotNull;

/**
 * Holds the boxes of a button in its states: normal, hovered by the pointer, pressed (also a command button whose targeted action waits),
 * disabled, and selected (a dialog button that stays chosen, such as the chosen tab of a row of tabs).
 */
public final class ButtonStyle {
    private final @NotNull BoxStyle normal;
    private final @NotNull BoxStyle hovered;
    private final @NotNull BoxStyle pressed;
    private final @NotNull BoxStyle disabled;
    private final @NotNull BoxStyle selected;

    /**
     * Creates a {@link ButtonStyle} instance whose selected box is the pressed box.
     *
     * @param normal   the box of the normal state
     * @param hovered  the box while the pointer is over the button
     * @param pressed  the box while the button is pressed
     * @param disabled the box while the button is disabled
     */
    public ButtonStyle(@NotNull BoxStyle normal, @NotNull BoxStyle hovered, @NotNull BoxStyle pressed, @NotNull BoxStyle disabled) {
        this(normal, hovered, pressed, disabled, pressed);
    }

    /**
     * Creates a {@link ButtonStyle} instance.
     *
     * @param normal   the box of the normal state
     * @param hovered  the box while the pointer is over the button
     * @param pressed  the box while the button is pressed
     * @param disabled the box while the button is disabled
     * @param selected the box while the button is selected
     */
    public ButtonStyle(@NotNull BoxStyle normal, @NotNull BoxStyle hovered, @NotNull BoxStyle pressed, @NotNull BoxStyle disabled,
                       @NotNull BoxStyle selected) {
        this.normal   = normal;
        this.hovered  = hovered;
        this.pressed  = pressed;
        this.disabled = disabled;
        this.selected = selected;
    }

    /**
     * @return the box of the normal state
     */
    public @NotNull BoxStyle getNormal() {
        return this.normal;
    }

    /**
     * @return the box while the pointer is over the button
     */
    public @NotNull BoxStyle getHovered() {
        return this.hovered;
    }

    /**
     * @return the box while the button is pressed
     */
    public @NotNull BoxStyle getPressed() {
        return this.pressed;
    }

    /**
     * @return the box while the button is disabled
     */
    public @NotNull BoxStyle getDisabled() {
        return this.disabled;
    }

    /**
     * @return the box while the button is selected
     */
    public @NotNull BoxStyle getSelected() {
        return this.selected;
    }


    /**
     * Returns the box of a button state: pressed before disabled before hovered before normal.
     *
     * @param enabled whether the button is enabled
     * @param pressed whether the button is pressed
     * @param hovered whether the pointer is over the button
     * @return the box of the state
     */
    public @NotNull BoxStyle findStyle(boolean enabled, boolean pressed, boolean hovered) {
        if (pressed) {
            return this.pressed;
        }

        if (!enabled) {
            return this.disabled;
        }

        return hovered ? this.hovered : this.normal;
    }
}
