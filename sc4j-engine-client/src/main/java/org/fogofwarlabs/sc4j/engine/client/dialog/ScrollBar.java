package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;

/**
 * Holds a value between a minimum and a maximum, as a scroll bar or a slider, on a {@link ScrollModel}: horizontal when the control is
 * wider than high, with a square thumb as thick as the bar. A press on the track before or after the thumb moves the value by a page, a
 * press on the thumb drags it; the arrow keys and the wheel step the value by one, Page Up and Page Down move it by a page, Home and End go
 * to the ends.
 */
public final class ScrollBar extends Control {
    private final @NotNull ScrollModel model;

    private boolean valueChanged;

    /**
     * Creates a {@link ScrollBar} instance over {@code 0} to {@code 0} with a page of {@code 1}.
     *
     * @param definition the control's definition
     */
    public ScrollBar(@NotNull DialogControl definition) {
        super(definition);

        this.model = new ScrollModel();

        this.valueChanged = false;
    }

    /**
     * @return the smallest value
     */
    public int getMinimum() {
        return this.model.getMinimum();
    }

    /**
     * @return the largest value
     */
    public int getMaximum() {
        return this.model.getMaximum();
    }

    /**
     * Sets the range and clamps the value into it.
     *
     * @param minimum the smallest value
     * @param maximum the largest value, at least the minimum
     */
    public void setRange(int minimum, int maximum) {
        this.model.setRange(minimum, maximum);
    }

    /**
     * @return the value
     */
    public int getValue() {
        return this.model.getValue();
    }

    /**
     * Sets the value.
     *
     * @param value the value, clamped to the range
     */
    public void setValue(int value) {
        this.model.setValue(value);
    }

    /**
     * @return the step of a press on the track and of Page Up and Page Down
     */
    public int getPage() {
        return this.model.getPage();
    }

    /**
     * Sets the step of a press on the track and of Page Up and Page Down.
     *
     * @param page the step
     */
    public void setPage(int page) {
        this.model.setPage(page);
    }

    /**
     * @return {@code true} if the bar lies horizontally, {@code false} otherwise
     */
    public boolean isHorizontal() {
        return this.getDefinition().getWidth() > this.getDefinition().getHeight();
    }

    /**
     * @return {@code true} always: a scroll bar takes presses
     */
    @Override
    public boolean isPressable() {
        return true;
    }

    /**
     * @return {@code true} always: a scroll bar takes the focus
     */
    @Override
    public boolean isFocusable() {
        return true;
    }

    /**
     * @return the bar's length along its direction
     */
    private int getLength() {
        DialogControl definition = this.getDefinition();

        return this.isHorizontal() ? definition.getWidth() : definition.getHeight();
    }

    /**
     * @return the bar's thickness across its direction, which is the thumb's length
     */
    private int getThickness() {
        DialogControl definition = this.getDefinition();

        return this.isHorizontal() ? definition.getHeight() : definition.getWidth();
    }


    /**
     * Called when a press goes to the control: the track pages, the thumb starts a drag.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     */
    @Override
    protected void onPressed(float x, float y) {
        float offset = this.isHorizontal() ? x : y;

        this.valueChanged = this.model.press(this.findPart(offset));

        if (this.model.isDragging()) {
            this.valueChanged |= this.dragTo(offset);
        }
    }

    /**
     * Called when the pointer moves while the control holds the press: a dragged thumb follows the pointer.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     */
    @Override
    protected void onDragged(float x, float y) {
        float offset = this.isHorizontal() ? x : y;

        if (this.model.isDragging()) {
            this.valueChanged |= this.dragTo(offset);
        } else {
            this.model.movePointer(this.findPart(offset));
        }
    }

    private @NotNull ScrollPart findPart(float offset) {
        int thickness = this.getThickness();
        int travel = this.getLength() - thickness;

        // The bar has no step parts: the track runs from end to end.
        return this.model.findPart(offset, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, this.model.getThumbOffset(travel), thickness);
    }

    private boolean dragTo(float offset) {
        int length = this.getLength();
        int thickness = this.getThickness();

        return this.model.dragTo(offset, 0, length - 1 - thickness / 2, thickness, length - thickness);
    }

    /**
     * Called when the press of the control is released on it.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     * @return {@code true} if the press changed the value, {@code false} otherwise
     */
    @Override
    protected boolean onReleased(float x, float y) {
        this.model.release();

        return this.valueChanged;
    }

    /**
     * Activates the control: a scroll bar reports no activation, so Enter reaches the dialog's default control.
     *
     * @return {@code false} always
     */
    @Override
    public boolean activate() {
        return false;
    }

    /**
     * Takes a key while the control has the focus: the arrow keys step the value, Page Up and Page Down move it by a page, Home and End
     * go to the ends.
     *
     * @param key the key
     * @return {@code true} if the control used the key, {@code false} to let the dialog handle it
     */
    @Override
    public boolean handleKey(@NotNull DialogKey key) {
        int value = this.model.getValue();

        switch (key) {
            case LEFT, UP -> this.model.setValue(value - 1);
            case RIGHT, DOWN -> this.model.setValue(value + 1);
            case PAGE_UP -> this.model.setValue(value - this.model.getPage());
            case PAGE_DOWN -> this.model.setValue(value + this.model.getPage());
            case HOME -> this.model.setValue(this.model.getMinimum());
            case END -> this.model.setValue(this.model.getMaximum());
            default -> {
                return false;
            }
        }

        return true;
    }

    /**
     * Takes wheel steps while the control is hovered or focused: steps the value.
     *
     * @param steps the steps, positive toward the end
     * @return {@code true} always
     */
    @Override
    public boolean handleWheel(int steps) {
        this.model.setValue(this.model.getValue() + steps);

        return true;
    }

    /**
     * Draws the control itself, without its children.
     *
     * @param canvas the canvas that draws
     * @param theme  the look of the controls
     * @param x      the control's left edge on the canvas
     * @param y      the control's top edge on the canvas
     */
    @Override
    protected void drawControl(@NotNull Canvas canvas, @NotNull DialogTheme theme, float x, float y) {
        DialogControl definition = this.getDefinition();
        float width = definition.getWidth();
        float height = definition.getHeight();
        int frame = this.isFocused() ? theme.getFocusColor() : theme.getFrameColor();
        int thickness = this.getThickness();
        int thumbOffset = this.model.getThumbOffset(this.getLength() - thickness);

        canvas.fillRectangle(x, y, width, height, theme.getFillColor());
        canvas.drawRectangle(x, y, width, height, theme.getFrameThickness(), frame);

        if (this.isHorizontal()) {
            canvas.fillRectangle(x + thumbOffset, y, thickness, thickness, theme.getSelectionColor());
        } else {
            canvas.fillRectangle(x, y + thumbOffset, thickness, thickness, theme.getSelectionColor());
        }
    }
}
