package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.jetbrains.annotations.Nullable;

/**
 * Exposes one open dialog to a {@link DialogStack}: its placement and the pointer state of its controls.
 * <p>
 * The coordinates a layer receives are relative to its left and top edges.
 *
 * @param <C> the type of the dialog's controls
 */
public interface DialogLayer<C> {
    /**
     * @return the left edge of the dialog in stack coordinates
     */
    float getLeft();

    /**
     * @return the top edge of the dialog in stack coordinates
     */
    float getTop();

    /**
     * @return {@code true} if the dialog takes all input while open, {@code false} otherwise
     */
    boolean isModal();

    /**
     * @return {@code true} if a control of the dialog holds a press, {@code false} otherwise
     */
    boolean hasPressedControl();


    /**
     * Finds the control a press at a point goes to.
     *
     * @param x the x relative to the dialog
     * @param y the y relative to the dialog
     * @return the enabled control that takes presses there, or {@code null}
     */
    @Nullable C findControlAt(float x, float y);

    /**
     * Finds the control the pointer hovers at a point.
     *
     * @param x the x relative to the dialog
     * @param y the y relative to the dialog
     * @return the visible control that takes the hover there, or {@code null}
     */
    @Nullable C findHoverAt(float x, float y);

    /**
     * Marks a control pressed, or no control.
     *
     * @param control the control the press went to, or {@code null} for the dialog's empty area
     * @param x       the x of the press relative to the dialog
     * @param y       the y of the press relative to the dialog
     */
    void press(@Nullable C control, float x, float y);

    /**
     * Releases the held press.
     *
     * @param x the x relative to the dialog
     * @param y the y relative to the dialog
     * @return the control the release activates (the pressed one, when the pointer is still on it), or {@code null}
     */
    @Nullable C release(float x, float y);

    /**
     * Sets the hovered control.
     *
     * @param control the hovered control, or {@code null} for none
     */
    void setHovered(@Nullable C control);
}
