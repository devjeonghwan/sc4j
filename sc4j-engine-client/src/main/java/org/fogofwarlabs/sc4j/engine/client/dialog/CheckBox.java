package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Holds a checked state on a {@link CheckModel}: a check box toggles it when activated; an option button ({@link #isOption}) checks
 * itself and clears the other option buttons of its dialog with the same {@value #GROUP_ATTRIBUTE} attribute.
 * <p>
 * The box is drawn at the left edge, a square of the control's height, with the text right of it.
 */
public final class CheckBox extends Control {
    /**
     * The attribute naming the group of an option button.
     */
    public static final @NotNull String GROUP_ATTRIBUTE = "group";

    private static final float MARK_INSET = 3;
    private static final float TEXT_GAP = 4;
    private static final float TWICE = 2;

    private final @NotNull CheckModel model;
    private final @NotNull Dialog dialog;

    /**
     * Creates a {@link CheckBox} instance, unchecked.
     *
     * @param definition the control's definition
     * @param option     {@code true} for an option button of a group, {@code false} for a check box
     * @param dialog     the dialog the option button clears the group of
     */
    public CheckBox(@NotNull DialogControl definition, boolean option, @NotNull Dialog dialog) {
        super(definition);

        this.model  = new CheckModel(option);
        this.dialog = dialog;
    }

    /**
     * @return {@code true} for an option button of a group, {@code false} for a check box
     */
    public boolean isOption() {
        return this.model.isOption();
    }

    /**
     * @return {@code true} if the box is checked, {@code false} otherwise
     */
    public boolean isChecked() {
        return this.model.isChecked();
    }

    /**
     * Sets the checked state without clearing an option group.
     *
     * @param checked {@code true} to check the box
     */
    public void setChecked(boolean checked) {
        this.model.setChecked(checked);
    }

    /**
     * @return {@code true} always: a check box takes presses
     */
    @Override
    public boolean isPressable() {
        return true;
    }

    /**
     * @return {@code true} always: a check box takes the focus
     */
    @Override
    public boolean isFocusable() {
        return true;
    }


    /**
     * Activates the control: a check box toggles; an option button checks itself and clears the rest of its group.
     *
     * @return {@code true} always: the dialog reports the change
     */
    @Override
    public boolean activate() {
        if (this.model.isOption()) {
            String group = this.getDefinition().getAttributes().get(GROUP_ATTRIBUTE);

            for (Control control : this.dialog.getAllControls()) {
                if (control instanceof CheckBox other && other != this && other.isOption() &&
                    Objects.equals(group, other.getDefinition().getAttributes().get(GROUP_ATTRIBUTE))) {
                    other.model.setChecked(false);
                }
            }
        }

        this.model.activate();

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
        float size = definition.getHeight();
        int frame = this.isFocused() ? theme.getFocusColor() : theme.getFrameColor();

        canvas.fillRectangle(x, y, size, size, theme.getFillColor());
        canvas.drawRectangle(x, y, size, size, theme.getFrameThickness(), frame);

        if (this.model.isChecked()) {
            canvas.fillRectangle(x + MARK_INSET, y + MARK_INSET, size - TWICE * MARK_INSET, size - TWICE * MARK_INSET, theme.getSelectionColor());
        }

        this.drawText(canvas, theme, this.getText(), x + size + TEXT_GAP, y, definition.getWidth() - size - TEXT_GAP, size, this.getTextColor(theme));
    }
}
