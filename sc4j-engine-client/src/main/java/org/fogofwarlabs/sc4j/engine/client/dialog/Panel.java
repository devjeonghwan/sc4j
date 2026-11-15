package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.Texture;
import org.fogofwarlabs.sc4j.engine.client.skin.BoxStyle;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Frames or groups other controls: the engine's default for panels, animations and the mod's own controls. It draws its picture
 * when the mod gave it one, else the theme's panel box ({@link DialogTheme#getPanelStyle}), else the theme's background and frame.
 */
public final class Panel extends Control {
    private static final int OPAQUE_WHITE = 0xFFFFFFFF;

    private @Nullable Texture image;

    /**
     * Creates a {@link Panel} instance.
     *
     * @param definition the control's definition
     */
    public Panel(@NotNull DialogControl definition) {
        super(definition);

        this.image = null;
    }

    /**
     * @return the picture drawn over the panel's bounds, or {@code null}
     */
    public @Nullable Texture getImage() {
        return this.image;
    }

    /**
     * Sets the picture drawn over the panel's bounds; the panel does not close it.
     *
     * @param image the picture, or {@code null} for the theme's background
     */
    public void setImage(@Nullable Texture image) {
        this.image = image;
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
        Texture image = this.image;
        BoxStyle style = theme.getPanelStyle();

        if (image != null) {
            canvas.drawTexture(image, x, y, definition.getWidth(), definition.getHeight(), OPAQUE_WHITE);

            return;
        }

        if (style != null) {
            style.draw(canvas, x, y, definition.getWidth(), definition.getHeight());

            return;
        }

        canvas.fillRectangle(x, y, definition.getWidth(), definition.getHeight(), theme.getBackgroundColor());
        canvas.drawRectangle(x, y, definition.getWidth(), definition.getHeight(), theme.getFrameThickness(), theme.getFrameColor());
    }
}
