package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;

/**
 * Edits a one-line text through a {@link TextEditModel}: typed characters insert at the cursor, Backspace and Delete remove around it, Left,
 * Right, Home and End move it, each over a whole code point. A press focuses the input. The {@value #MAXIMUM_LENGTH_ATTRIBUTE} attribute
 * limits the length in code points.
 */
public final class TextInput extends Control {
    /**
     * The attribute holding the most code points the text takes.
     */
    public static final @NotNull String MAXIMUM_LENGTH_ATTRIBUTE = "maximumLength";

    private static final int FIRST_PRINTABLE = 0x20;
    private static final float TEXT_INSET = 3;
    private static final float CURSOR_WIDTH = 1;
    private static final float HALF = 2;

    private final @NotNull TextEditModel model;

    /**
     * Creates a {@link TextInput} instance with the cursor after the text.
     *
     * @param definition the control's definition
     * @throws NumberFormatException if the {@value #MAXIMUM_LENGTH_ATTRIBUTE} attribute is not a number
     */
    public TextInput(@NotNull DialogControl definition) {
        super(definition);

        String maximumLength = definition.getAttributes().get(MAXIMUM_LENGTH_ATTRIBUTE);

        this.model = new TextEditModel(this.getText(), maximumLength == null ? TextEditModel.UNLIMITED : Integer.parseInt(maximumLength), "");
    }

    /**
     * @return the cursor position, a UTF-16 index of the text on a code point boundary
     */
    public int getCursor() {
        return this.model.getCaret();
    }

    /**
     * Sets the text and moves the cursor after it.
     *
     * @param text the text
     */
    @Override
    public void setText(@NotNull String text) {
        super.setText(text);

        this.model.setText(text);
    }

    /**
     * @return {@code true} always: a text input takes presses
     */
    @Override
    public boolean isPressable() {
        return true;
    }

    /**
     * @return {@code true} always: a text input takes the focus
     */
    @Override
    public boolean isFocusable() {
        return true;
    }


    /**
     * Activates the control: a text input reports no activation, so Enter reaches the dialog's default control.
     *
     * @return {@code false} always
     */
    @Override
    public boolean activate() {
        return false;
    }

    /**
     * Takes a key while the control has the focus: the editing and cursor keys.
     *
     * @param key the key
     * @return {@code true} if the control used the key, {@code false} to let the dialog handle it
     */
    @Override
    public boolean handleKey(@NotNull DialogKey key) {
        TextEditKey editing = switch (key) {
            case BACKSPACE -> TextEditKey.BACKSPACE;
            case DELETE -> TextEditKey.DELETE;
            case LEFT -> TextEditKey.LEFT;
            case RIGHT -> TextEditKey.RIGHT;
            case HOME -> TextEditKey.HOME;
            case END -> TextEditKey.END;
            default -> null;
        };

        if (editing == null) {
            return false;
        }

        if (this.model.press(editing)) {
            super.setText(this.model.getText());
        }

        return true;
    }

    /**
     * Takes a typed character while the control has the focus: a printable character inserts at the cursor while the text has fewer code
     * points than its limit.
     *
     * @param codePoint the character
     * @return {@code true} if the control used the character, {@code false} to let the dialog handle it
     */
    @Override
    public boolean handleCharacter(int codePoint) {
        if (codePoint < FIRST_PRINTABLE || Character.isISOControl(codePoint)) {
            return false;
        }

        if (this.model.type(codePoint)) {
            super.setText(this.model.getText());
        }

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
        int color = this.isEnabled() ? theme.getTextColor() : theme.getDisabledTextColor();
        float inset = Math.max(TEXT_INSET, theme.getFieldPadding());
        float textTop = y + (height - theme.getFont().getLineHeight()) / HALF;

        theme.drawFieldBackground(canvas, x, y, width, height);
        canvas.setClip((int) (x + inset), (int) y, (int) (width - 2 * inset), (int) height);
        theme.getFont().draw(canvas, this.getText(), x + inset, textTop, color);

        if (this.isFocused()) {
            float cursorLeft = x + inset + theme.getFont().measure(this.getText().substring(0, this.model.getCaret()));

            canvas.fillRectangle(cursorLeft, textTop, CURSOR_WIDTH, theme.getFont().getLineHeight(), theme.getFocusColor());
        }

        canvas.setClip(0, 0, -1, -1);
        theme.drawFieldOutline(canvas, x, y, width, height, this.isFocused());
    }
}
