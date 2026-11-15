package org.fogofwarlabs.sc4j.engine.client.community;

import org.fogofwarlabs.sc4j.engine.client.dialog.Control;
import org.fogofwarlabs.sc4j.engine.client.dialog.DialogTheme;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.text.TextLayout;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a chat log in a dialog: lines of text, each in its own colour, wrapped to the control's width, the newest at the bottom. The wheel scrolls
 * back; a new line scrolls to the bottom again. It draws on the theme's field box ({@link DialogTheme#withFieldStyle}). The log keeps its newest
 * lines up to a limit. A screen puts it in the place of a built control of its dialog
 * ({@link org.fogofwarlabs.sc4j.engine.client.dialog.Dialog#replaceControl}).
 */
public final class ChatLogBox extends Control {
    private static final float TEXT_INSET = 3;
    private static final int BOTH_SIDES = 2;

    /**
     * One line of the log, or one wrapped row of it: the text and its colour.
     */
    private static final class Line {
        private final @NotNull String text;
        private final int color;

        private Line(@NotNull String text, int color) {
            this.text  = text;
            this.color = color;
        }
    }

    private final int maximumLines;
    private final @NotNull List<Line> lines;

    /**
     * The rows of the lines wrapped in the font and width of the last drawing, or empty when a line changed since.
     */
    private final @NotNull List<Line> rows;
    private @Nullable Font wrappedFont;
    private float wrappedWidth;
    private int visibleRows;

    /**
     * The rows the view is scrolled back from the newest row.
     */
    private int scrolledRows;

    /**
     * Creates an empty {@link ChatLogBox} instance.
     *
     * @param definition   the control's definition
     * @param maximumLines the most lines the log keeps, at least {@code 1}; the oldest go first
     */
    public ChatLogBox(@NotNull DialogControl definition, int maximumLines) {
        super(definition);

        this.maximumLines = maximumLines;
        this.lines        = new ArrayList<>();

        this.rows         = new ArrayList<>();
        this.wrappedFont  = null;
        this.wrappedWidth = 0;
        this.visibleRows  = 1;

        this.scrolledRows = 0;
    }

    /**
     * @return the texts of the kept lines, oldest first
     */
    public @NotNull List<@NotNull String> getTexts() {
        List<String> texts = new ArrayList<>(this.lines.size());

        for (Line line : this.lines) {
            texts.add(line.text);
        }

        return texts;
    }

    /**
     * @return the rows the view is scrolled back from the newest row, {@code 0} at the bottom
     */
    public int getScrolledRows() {
        return this.scrolledRows;
    }

    /**
     * @return {@code true} always: the log takes presses, so the wheel reaches it
     */
    @Override
    public boolean isPressable() {
        return true;
    }


    /**
     * Adds a line at the bottom and scrolls to it; the oldest line goes once the log is full.
     *
     * @param text  the line, wrapped when it is wider than the log
     * @param color the packed {@code 0xRRGGBBAA} colour of the line
     */
    public void addLine(@NotNull String text, int color) {
        this.lines.add(new Line(text, color));

        while (this.lines.size() > this.maximumLines) {
            this.lines.removeFirst();
        }

        this.rows.clear();
        this.scrolledRows = 0;
    }

    /**
     * Removes every line.
     */
    public void clear() {
        this.lines.clear();
        this.rows.clear();
        this.scrolledRows = 0;
    }

    /**
     * Activates the control: the log reports no activation, so Enter reaches the dialog's default control.
     *
     * @return {@code false} always
     */
    @Override
    public boolean activate() {
        return false;
    }

    /**
     * Takes wheel steps while the control is hovered or focused: scrolls the rows back or toward the newest.
     *
     * @param steps the steps, positive toward the end
     * @return {@code true} always
     */
    @Override
    public boolean handleWheel(int steps) {
        int lastScroll = Math.max(0, this.rows.size() - this.visibleRows);

        this.scrolledRows = Math.clamp(this.scrolledRows - steps, 0, lastScroll);

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
        Font font = theme.getFont();
        float inset = Math.max(TEXT_INSET, theme.getFieldPadding());
        float textWidth = width - inset * BOTH_SIDES;

        if (this.rows.isEmpty() || this.wrappedFont != font || this.wrappedWidth != textWidth) {
            this.wrap(font, textWidth);
        }

        this.visibleRows  = Math.max(1, (int) ((height - inset * BOTH_SIDES) / font.getLineHeight()));
        this.scrolledRows = Math.min(this.scrolledRows, Math.max(0, this.rows.size() - this.visibleRows));

        int lastRow = this.rows.size() - this.scrolledRows;
        int firstRow = Math.max(0, lastRow - this.visibleRows);

        theme.drawFieldBackground(canvas, x, y, width, height);
        theme.drawFieldOutline(canvas, x, y, width, height, false);
        canvas.setClip((int) x, (int) y, (int) width, (int) height);

        for (int row = firstRow; row < lastRow; row++) {
            Line shown = this.rows.get(row);

            font.draw(canvas, shown.text, x + inset, y + inset + (row - firstRow) * font.getLineHeight(), shown.color);
        }

        canvas.setClip(0, 0, -1, -1);
    }

    private void wrap(@NotNull Font font, float textWidth) {
        TextLayout layout = new TextLayout(font, textWidth);

        this.rows.clear();

        for (Line line : this.lines) {
            for (String row : layout.wrap(line.text)) {
                this.rows.add(new Line(row, line.color));
            }
        }

        this.wrappedFont  = font;
        this.wrappedWidth = textWidth;
    }
}
