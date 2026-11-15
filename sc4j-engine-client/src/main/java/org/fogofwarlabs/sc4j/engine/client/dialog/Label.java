package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;

/**
 * Shows a one-line text the user cannot edit, aligned as its definition says.
 */
public final class Label extends Control {
    /**
     * Creates a {@link Label} instance.
     *
     * @param definition the control's definition
     */
    public Label(@NotNull DialogControl definition) {
        super(definition);
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
        int color = this.isEnabled() ? theme.getTextColor() : theme.getDisabledTextColor();

        this.drawText(canvas, theme, this.getText(), x, y, definition.getWidth(), definition.getHeight(), color);
    }
}
