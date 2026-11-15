package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.skin.BoxStyle;
import org.fogofwarlabs.sc4j.engine.client.skin.ButtonStyle;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds how the engine's default controls look: the font of their text and their packed {@code 0xRRGGBBAA} colours, and optionally the boxes of a
 * skin: a panel box drawn instead of the panel colours, button boxes drawn instead of the button colours, a field box drawn under text inputs,
 * drop-down lists, lists and tables instead of their fill colour and frame, and a selection box drawn under the selected entry of a list, a table
 * or an open drop-down list instead of the selection colour ({@link #withPanelStyle}, {@link #withButtonStyle}, {@link #withFieldStyle},
 * {@link #withSelectionStyle}), for example nine-slice pictures. A mod chooses the theme of its dialogs, for example per faction.
 */
public final class DialogTheme {
    private static final int DEFAULT_TEXT_COLOR = 0xE0E0E0FF;
    private static final int DEFAULT_DISABLED_TEXT_COLOR = 0x808080FF;
    private static final int DEFAULT_HIGHLIGHT_TEXT_COLOR = 0xFFFFFFFF;
    private static final int DEFAULT_BACKGROUND_COLOR = 0x101820E0;
    private static final int DEFAULT_FILL_COLOR = 0x283848FF;
    private static final int DEFAULT_PRESSED_FILL_COLOR = 0x182028FF;
    private static final int DEFAULT_FRAME_COLOR = 0x6080A0FF;
    private static final int DEFAULT_FOCUS_COLOR = 0xFFD040FF;
    private static final int DEFAULT_SELECTION_COLOR = 0x4060A0FF;
    private static final float DEFAULT_FRAME_THICKNESS = 1;

    /**
     * Creates a {@link DialogTheme} instance with the engine's default colours.
     *
     * @param font the font of the control texts
     * @return the created {@link DialogTheme} instance
     */
    public static @NotNull DialogTheme createDefault(@NotNull Font font) {
        return new DialogTheme(font,
                               DEFAULT_TEXT_COLOR, DEFAULT_DISABLED_TEXT_COLOR, DEFAULT_HIGHLIGHT_TEXT_COLOR,
                               DEFAULT_BACKGROUND_COLOR, DEFAULT_FILL_COLOR, DEFAULT_PRESSED_FILL_COLOR,
                               DEFAULT_FRAME_COLOR, DEFAULT_FOCUS_COLOR, DEFAULT_SELECTION_COLOR,
                               DEFAULT_FRAME_THICKNESS);
    }


    private final @NotNull Font font;

    private final int textColor;
    private final int disabledTextColor;
    private final int highlightTextColor;

    private final int backgroundColor;
    private final int fillColor;
    private final int pressedFillColor;

    private final int frameColor;
    private final int focusColor;
    private final int selectionColor;

    private final float frameThickness;

    private final @Nullable BoxStyle panelStyle;
    private final @Nullable ButtonStyle buttonStyle;
    private final @Nullable BoxStyle fieldStyle;
    private final float fieldPadding;
    private final @Nullable BoxStyle selectionStyle;

    /**
     * Creates a {@link DialogTheme} instance without skin boxes.
     *
     * @param font               the font of the control texts
     * @param textColor          the colour of enabled texts
     * @param disabledTextColor  the colour of disabled texts
     * @param highlightTextColor the colour of hovered texts
     * @param backgroundColor    the fill of panels
     * @param fillColor          the fill of buttons, inputs and lists
     * @param pressedFillColor   the fill of pressed buttons
     * @param frameColor         the outline of controls
     * @param focusColor         the outline of the focused control
     * @param selectionColor     the fill of selected list entries, checked boxes and selected buttons without boxes
     * @param frameThickness     the outline thickness
     */
    public DialogTheme(@NotNull Font font,
                       int textColor, int disabledTextColor, int highlightTextColor,
                       int backgroundColor, int fillColor, int pressedFillColor,
                       int frameColor, int focusColor, int selectionColor,
                       float frameThickness) {
        this(font,
             textColor, disabledTextColor, highlightTextColor,
             backgroundColor, fillColor, pressedFillColor,
             frameColor, focusColor, selectionColor,
             frameThickness,
             null, null, null, 0, null);
    }

    private DialogTheme(@NotNull Font font,
                        int textColor, int disabledTextColor, int highlightTextColor,
                        int backgroundColor, int fillColor, int pressedFillColor,
                        int frameColor, int focusColor, int selectionColor,
                        float frameThickness,
                        @Nullable BoxStyle panelStyle, @Nullable ButtonStyle buttonStyle, @Nullable BoxStyle fieldStyle, float fieldPadding,
                        @Nullable BoxStyle selectionStyle) {
        this.font = font;

        this.textColor          = textColor;
        this.disabledTextColor  = disabledTextColor;
        this.highlightTextColor = highlightTextColor;

        this.backgroundColor  = backgroundColor;
        this.fillColor        = fillColor;
        this.pressedFillColor = pressedFillColor;

        this.frameColor     = frameColor;
        this.focusColor     = focusColor;
        this.selectionColor = selectionColor;

        this.frameThickness = frameThickness;

        this.panelStyle     = panelStyle;
        this.buttonStyle    = buttonStyle;
        this.fieldStyle     = fieldStyle;
        this.fieldPadding   = fieldPadding;
        this.selectionStyle = selectionStyle;
    }

    /**
     * @return the font of the control texts
     */
    public @NotNull Font getFont() {
        return this.font;
    }

    /**
     * @return the colour of enabled texts
     */
    public int getTextColor() {
        return this.textColor;
    }

    /**
     * @return the colour of disabled texts
     */
    public int getDisabledTextColor() {
        return this.disabledTextColor;
    }

    /**
     * @return the colour of hovered texts
     */
    public int getHighlightTextColor() {
        return this.highlightTextColor;
    }

    /**
     * @return the fill of panels
     */
    public int getBackgroundColor() {
        return this.backgroundColor;
    }

    /**
     * @return the fill of buttons, inputs and lists
     */
    public int getFillColor() {
        return this.fillColor;
    }

    /**
     * @return the fill of pressed buttons
     */
    public int getPressedFillColor() {
        return this.pressedFillColor;
    }

    /**
     * @return the outline of controls
     */
    public int getFrameColor() {
        return this.frameColor;
    }

    /**
     * @return the outline of the focused control
     */
    public int getFocusColor() {
        return this.focusColor;
    }

    /**
     * @return the fill of selected list entries, checked boxes and selected buttons without boxes
     */
    public int getSelectionColor() {
        return this.selectionColor;
    }

    /**
     * @return the outline thickness
     */
    public float getFrameThickness() {
        return this.frameThickness;
    }

    /**
     * @return the box of panels, or {@code null} for the background and frame colours
     */
    public @Nullable BoxStyle getPanelStyle() {
        return this.panelStyle;
    }

    /**
     * @return the boxes of buttons, or {@code null} for the fill and frame colours
     */
    public @Nullable ButtonStyle getButtonStyle() {
        return this.buttonStyle;
    }

    /**
     * @return the box of text inputs, drop-down lists, lists and tables, or {@code null} for the fill and frame colours
     */
    public @Nullable BoxStyle getFieldStyle() {
        return this.fieldStyle;
    }

    /**
     * @return the distance of a field's text from its edges, the border of the field box; {@code 0} without field box
     */
    public float getFieldPadding() {
        return this.fieldPadding;
    }

    /**
     * @return the box under the selected entry of lists, tables and open drop-down lists, or {@code null} for the selection colour
     */
    public @Nullable BoxStyle getSelectionStyle() {
        return this.selectionStyle;
    }


    /**
     * Creates a {@link DialogTheme} instance like this one with another panel box.
     *
     * @param style the box of panels, or {@code null} for the background and frame colours
     * @return the created {@link DialogTheme} instance
     */
    public @NotNull DialogTheme withPanelStyle(@Nullable BoxStyle style) {
        return new DialogTheme(this.font,
                               this.textColor, this.disabledTextColor, this.highlightTextColor,
                               this.backgroundColor, this.fillColor, this.pressedFillColor,
                               this.frameColor, this.focusColor, this.selectionColor,
                               this.frameThickness,
                               style, this.buttonStyle, this.fieldStyle, this.fieldPadding, this.selectionStyle);
    }

    /**
     * Creates a {@link DialogTheme} instance like this one with other button boxes; a focused button keeps the focus outline over its box.
     *
     * @param style the boxes of buttons, or {@code null} for the fill and frame colours
     * @return the created {@link DialogTheme} instance
     */
    public @NotNull DialogTheme withButtonStyle(@Nullable ButtonStyle style) {
        return new DialogTheme(this.font,
                               this.textColor, this.disabledTextColor, this.highlightTextColor,
                               this.backgroundColor, this.fillColor, this.pressedFillColor,
                               this.frameColor, this.focusColor, this.selectionColor,
                               this.frameThickness,
                               this.panelStyle, style, this.fieldStyle, this.fieldPadding, this.selectionStyle);
    }

    /**
     * Creates a {@link DialogTheme} instance like this one with another field box: text inputs, drop-down lists and their open lists, lists and
     * tables draw it under their content instead of the fill colour and the frame; a focused field keeps the focus outline over its box.
     *
     * @param style   the box of fields, or {@code null} for the fill and frame colours
     * @param padding the distance of a field's text from its edges, the border of the box
     * @return the created {@link DialogTheme} instance
     */
    public @NotNull DialogTheme withFieldStyle(@Nullable BoxStyle style, float padding) {
        return new DialogTheme(this.font,
                               this.textColor, this.disabledTextColor, this.highlightTextColor,
                               this.backgroundColor, this.fillColor, this.pressedFillColor,
                               this.frameColor, this.focusColor, this.selectionColor,
                               this.frameThickness,
                               this.panelStyle, this.buttonStyle, style, style != null ? padding : 0, this.selectionStyle);
    }

    /**
     * Creates a {@link DialogTheme} instance like this one with another selection box: lists, tables and open drop-down lists draw it under their
     * selected entry instead of the selection colour.
     *
     * @param style the box under a selected entry, or {@code null} for the selection colour
     * @return the created {@link DialogTheme} instance
     */
    public @NotNull DialogTheme withSelectionStyle(@Nullable BoxStyle style) {
        return new DialogTheme(this.font,
                               this.textColor, this.disabledTextColor, this.highlightTextColor,
                               this.backgroundColor, this.fillColor, this.pressedFillColor,
                               this.frameColor, this.focusColor, this.selectionColor,
                               this.frameThickness,
                               this.panelStyle, this.buttonStyle, this.fieldStyle, this.fieldPadding, style);
    }


    /**
     * Draws the ground of a field: the field box, or the fill colour without one.
     *
     * @param canvas the canvas that draws
     * @param x      the field's left edge on the canvas
     * @param y      the field's top edge on the canvas
     * @param width  the field's width
     * @param height the field's height
     */
    public void drawFieldBackground(@NotNull Canvas canvas, float x, float y, float width, float height) {
        BoxStyle style = this.fieldStyle;

        if (style != null) {
            style.draw(canvas, x, y, width, height);
        } else {
            canvas.fillRectangle(x, y, width, height, this.fillColor);
        }
    }

    /**
     * Draws the mark of a selected entry of a list, a table or an open drop-down list: the selection box, or the selection colour without one.
     *
     * @param canvas the canvas that draws
     * @param x      the entry's left edge on the canvas
     * @param y      the entry's top edge on the canvas
     * @param width  the entry's width
     * @param height the entry's height
     */
    public void drawSelection(@NotNull Canvas canvas, float x, float y, float width, float height) {
        BoxStyle style = this.selectionStyle;

        if (style != null) {
            style.draw(canvas, x, y, width, height);
        } else {
            canvas.fillRectangle(x, y, width, height, this.selectionColor);
        }
    }

    /**
     * Draws the outline of a field over its content: the focus outline while it is focused, and without field box the frame otherwise.
     *
     * @param canvas  the canvas that draws
     * @param x       the field's left edge on the canvas
     * @param y       the field's top edge on the canvas
     * @param width   the field's width
     * @param height  the field's height
     * @param focused whether the field has the focus
     */
    public void drawFieldOutline(@NotNull Canvas canvas, float x, float y, float width, float height, boolean focused) {
        if (focused) {
            canvas.drawRectangle(x, y, width, height, this.frameThickness, this.focusColor);
        } else if (this.fieldStyle == null) {
            canvas.drawRectangle(x, y, width, height, this.frameThickness, this.frameColor);
        }
    }
}
