package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.skin.BoxStyle;
import org.fogofwarlabs.sc4j.engine.client.skin.ButtonStyle;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;

/**
 * Pushes: a released press, Enter while focused or its hotkey activates it. It draws the theme's button box of its state
 * ({@link DialogTheme#getButtonStyle}) with the focus outline when focused, else the theme's fill and frame colours. A selected button, for example
 * the chosen tab of a row of tabs ({@link #setSelected}, attribute {@value #SELECTED_ATTRIBUTE}), stays drawn in the selected box of the button
 * style ({@link ButtonStyle#getSelected}), or without one in the selection colour, with the highlight text, and takes no press and no focus.
 */
public final class Button extends Control {
    /**
     * The definition attribute that selects the button when its value is {@value Control#TRUE_VALUE}.
     */
    public static final @NotNull String SELECTED_ATTRIBUTE = "selected";

    private boolean selected;

    /**
     * Creates a {@link Button} instance, selected when its definition says so.
     *
     * @param definition the control's definition
     */
    public Button(@NotNull DialogControl definition) {
        super(definition);

        this.selected = TRUE_VALUE.equals(definition.getAttributes().get(SELECTED_ATTRIBUTE));
    }

    /**
     * @return {@code true} if the button is selected, for example the chosen tab, {@code false} otherwise
     */
    public boolean isSelected() {
        return this.selected;
    }

    /**
     * Sets whether the button is selected: a selected button stays drawn selected and takes no press.
     *
     * @param selected whether the button is selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * @return {@code true} if the button is not selected (a button takes presses), {@code false} otherwise
     */
    @Override
    public boolean isPressable() {
        return !this.selected;
    }

    /**
     * @return {@code true} if the button is not selected (a button takes the focus), {@code false} otherwise
     */
    @Override
    public boolean isFocusable() {
        return !this.selected;
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
        ButtonStyle style = theme.getButtonStyle();

        if (style != null) {
            BoxStyle box = this.selected ? style.getSelected() : style.findStyle(this.isEnabled(), this.isPressed(), this.isHovered());

            box.draw(canvas, x, y, definition.getWidth(), definition.getHeight());

            if (this.isFocused()) {
                canvas.drawRectangle(x, y, definition.getWidth(), definition.getHeight(), theme.getFrameThickness(), theme.getFocusColor());
            }
        } else {
            int fill;

            if (this.selected) {
                fill = theme.getSelectionColor();
            } else {
                fill = this.isPressed() ? theme.getPressedFillColor() : theme.getFillColor();
            }

            int frame = this.isFocused() ? theme.getFocusColor() : theme.getFrameColor();

            canvas.fillRectangle(x, y, definition.getWidth(), definition.getHeight(), fill);
            canvas.drawRectangle(x, y, definition.getWidth(), definition.getHeight(), theme.getFrameThickness(), frame);
        }

        this.drawText(canvas, theme, this.getText(), x, y, definition.getWidth(), definition.getHeight(),
                      this.selected && this.isEnabled() ? theme.getHighlightTextColor() : this.getTextColor(theme));
    }
}
