package org.fogofwarlabs.sc4j.engine.client.community;

import org.fogofwarlabs.sc4j.engine.client.dialog.Control;
import org.fogofwarlabs.sc4j.engine.client.dialog.DialogKey;
import org.fogofwarlabs.sc4j.engine.client.dialog.DialogTheme;
import org.fogofwarlabs.sc4j.engine.client.dialog.ListModel;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.text.TextCutUtils;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LatencyClass;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Lists rows of columns to select one from, as the community screens list players, channels and rooms: an optional header row, then one
 * {@link TableRow} per row of a fixed height, each column clipped to its share of the width, and latency bars at the right when the table
 * shows latencies (five bars: good lights five green ones, fair three yellow ones, poor one red one, unknown none). The rows are a
 * {@link ListModel}: a press selects the row under it, the navigation keys move the selection as a list does
 * ({@link org.fogofwarlabs.sc4j.engine.client.dialog.ListBox}) and the wheel scrolls; the selection stays visible when it moves. A screen
 * puts it in the place of a built control of its dialog ({@link org.fogofwarlabs.sc4j.engine.client.dialog.Dialog#replaceControl}).
 */
public final class TableBox extends Control {
    /**
     * The selection of a table without a selected row.
     */
    public static final int NO_SELECTION = ListModel.NO_SELECTION;

    private static final float TEXT_INSET = 3;
    private static final int BAR_COUNT = 5;
    private static final float BAR_WIDTH = 3;
    private static final float BAR_GAP = 1;
    private static final float BARS_MARGIN = 4;
    private static final float BAR_LOWEST_SHARE = 0.3f;
    private static final float BAR_HIGHEST_SHARE = 0.75f;
    private static final float HALF = 2;
    private static final int BOTH_SIDES = 2;
    private static final int GOOD_BARS = 5;
    private static final int FAIR_BARS = 3;
    private static final int POOR_BARS = 1;
    private static final int GOOD_COLOR = 0x48C848FF;
    private static final int FAIR_COLOR = 0xE8C040FF;
    private static final int POOR_COLOR = 0xE04848FF;

    private final int rowHeight;
    private final float @NotNull [] columnShares;
    private final @NotNull List<@NotNull String> headers;
    private final boolean latencyShown;

    private final @NotNull ListModel<TableRow> rows;
    private final @NotNull StringBuilder shownCell;

    /**
     * Creates an empty {@link TableBox} instance.
     *
     * @param definition   the control's definition
     * @param rowHeight    the height of one row, at least {@code 1}
     * @param columnShares the share of each column of the width left of the latency bars, in order, summing to at most {@code 1}
     * @param headers      the header texts of the columns, or an empty list for a table without a header row
     * @param latencyShown whether the rows' latency bars show at the right
     */
    public TableBox(@NotNull DialogControl definition, int rowHeight, float @NotNull [] columnShares, @NotNull List<@NotNull String> headers,
                    boolean latencyShown) {
        super(definition);

        this.rowHeight    = rowHeight;
        this.columnShares = columnShares.clone();
        this.headers      = List.copyOf(headers);
        this.latencyShown = latencyShown;

        this.rows      = new ListModel<>();
        this.shownCell = new StringBuilder();
    }

    /**
     * @return the rows in order, a copy
     */
    public @NotNull List<@NotNull TableRow> getRows() {
        return List.copyOf(this.rows.getItems());
    }

    /**
     * Sets the rows; the selection and the scrolled position stay where they are, within the new rows.
     *
     * @param rows the rows in order, copied
     */
    public void setRows(@NotNull List<@NotNull TableRow> rows) {
        int selection = this.rows.getSelection();
        int firstRow = this.rows.getFirstRow();
        int visibleRows = this.getVisibleRowCount();

        this.rows.setItems(rows);
        this.rows.scrollTo(firstRow, visibleRows);
        this.rows.select(selection == NO_SELECTION ? NO_SELECTION : Math.min(selection, rows.size() - 1), visibleRows);
    }

    /**
     * @return the index of the selected row, or {@link #NO_SELECTION}
     */
    public int getSelection() {
        return this.rows.getSelection();
    }

    /**
     * Selects a row and scrolls it into view.
     *
     * @param selection the row index, or {@link #NO_SELECTION}; an index that is not a row's clears the selection
     */
    public void setSelection(int selection) {
        this.rows.select(selection, this.getVisibleRowCount());
    }

    /**
     * @return the number of whole rows the table shows below its header, at least {@code 1}
     */
    public int getVisibleRowCount() {
        return Math.max(1, (this.getDefinition().getHeight() - this.getHeaderHeight()) / this.rowHeight);
    }

    /**
     * @return {@code true} always: a table takes presses
     */
    @Override
    public boolean isPressable() {
        return true;
    }

    /**
     * @return {@code true} always: a table takes the focus
     */
    @Override
    public boolean isFocusable() {
        return true;
    }

    private int getHeaderHeight() {
        return this.headers.isEmpty() ? 0 : this.rowHeight;
    }


    /**
     * Called when a press goes to the control: selects the row under the pointer.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     */
    @Override
    protected void onPressed(float x, float y) {
        this.rows.selectAt(y - this.getHeaderHeight(), this.rowHeight, this.getVisibleRowCount());
    }

    /**
     * Activates the control: a table reports no activation, so Enter reaches the dialog's default control.
     *
     * @return {@code false} always
     */
    @Override
    public boolean activate() {
        return false;
    }

    /**
     * Called when the press of the control is released on it: the press selected already, so the table reports the change.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     * @return {@code true} if a row is selected, {@code false} otherwise
     */
    @Override
    protected boolean onReleased(float x, float y) {
        return this.rows.getSelection() != NO_SELECTION;
    }

    /**
     * Takes a key while the control has the focus: Up and Down move the selection by one, from the first row without a selection;
     * Page Up, Page Down, Home and End move it as {@link ListModel#moveSelection} does.
     *
     * @param key the key
     * @return {@code true} if the control used the key, {@code false} to let the dialog handle it
     */
    @Override
    public boolean handleKey(@NotNull DialogKey key) {
        switch (key) {
            case UP, DOWN -> this.rows.step(key == DialogKey.DOWN, this.getVisibleRowCount());
            case HOME, END, PAGE_UP, PAGE_DOWN -> this.rows.moveSelection(key, this.getVisibleRowCount());
            default -> {
                return false;
            }
        }

        return true;
    }

    /**
     * Takes wheel steps while the control is hovered or focused: scrolls the rows.
     *
     * @param steps the steps, positive toward the end
     * @return {@code true} always
     */
    @Override
    public boolean handleWheel(int steps) {
        this.rows.scrollTo(this.rows.getFirstRow() + steps, this.getVisibleRowCount());

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
        float columnsWidth = this.latencyShown ? width - BARS_MARGIN * HALF - BAR_COUNT * (BAR_WIDTH + BAR_GAP) : width;
        float rowsTop = y + this.getHeaderHeight();
        int firstRow = this.rows.getFirstRow();
        int lastRow = Math.min(this.rows.getCount(), firstRow + this.getVisibleRowCount());

        theme.drawFieldBackground(canvas, x, y, width, height);

        if (!this.headers.isEmpty()) {
            this.drawCells(canvas, theme, this.headers, theme.getHighlightTextColor(), x, y, columnsWidth);
            canvas.fillRectangle(x, rowsTop - theme.getFrameThickness(), width, theme.getFrameThickness(), theme.getFrameColor());
        }

        for (int row = firstRow; row < lastRow; row++) {
            TableRow shown = this.rows.getItem(row);
            float rowTop = rowsTop + (row - firstRow) * this.rowHeight;
            int color = this.isEnabled() ? shown.getColor() : theme.getDisabledTextColor();
            LatencyClass latency = shown.getLatency();

            if (row == this.rows.getSelection()) {
                theme.drawSelection(canvas, x, rowTop, width, this.rowHeight);
            }

            this.drawCells(canvas, theme, shown.getCells(), color, x, rowTop, columnsWidth);

            if (this.latencyShown && latency != null) {
                this.drawBars(canvas, theme, latency, x + columnsWidth + BARS_MARGIN, rowTop);
            }
        }

        theme.drawFieldOutline(canvas, x, y, width, height, this.isFocused());
    }

    /**
     * Draws the cells of one row, each in its column; a text wider than its column is cut to the part that fits before
     * {@value TextCutUtils#CUT_MARK}.
     */
    private void drawCells(@NotNull Canvas canvas, @NotNull DialogTheme theme, @NotNull List<@NotNull String> cells, int color,
                           float x, float top, float columnsWidth) {
        Font font = theme.getFont();
        StringBuilder shown = this.shownCell;
        float inset = Math.max(TEXT_INSET, theme.getFieldPadding());
        float left = x;
        float textTop = top + (this.rowHeight - font.getLineHeight()) / HALF;

        for (int column = 0; column < this.columnShares.length && column < cells.size(); column++) {
            float columnWidth = columnsWidth * this.columnShares[column];
            float room = columnWidth - (column == 0 ? BOTH_SIDES * inset : inset + TEXT_INSET);
            String cell = cells.get(column);
            float textLeft = left + (column == 0 ? inset : TEXT_INSET);

            shown.setLength(0);
            shown.append(cell);
            TextCutUtils.cut(font, shown, room);

            canvas.setClip((int) textLeft, (int) top, (int) Math.max(room, 0), this.rowHeight);
            font.draw(canvas, shown, textLeft, textTop, color);
            left += columnWidth;
        }

        canvas.setClip(0, 0, -1, -1);
    }

    private void drawBars(@NotNull Canvas canvas, @NotNull DialogTheme theme, @NotNull LatencyClass latency, float left, float top) {
        int lit;
        int color;

        switch (latency) {
            case GOOD -> {
                lit   = GOOD_BARS;
                color = GOOD_COLOR;
            }
            case FAIR -> {
                lit   = FAIR_BARS;
                color = FAIR_COLOR;
            }
            case POOR -> {
                lit   = POOR_BARS;
                color = POOR_COLOR;
            }
            default -> {
                return;
            }
        }

        float bottom = top + this.rowHeight * BAR_HIGHEST_SHARE + (this.rowHeight * (1 - BAR_HIGHEST_SHARE)) / HALF;
        float step = this.rowHeight * (BAR_HIGHEST_SHARE - BAR_LOWEST_SHARE) / (BAR_COUNT - 1);

        for (int bar = 0; bar < BAR_COUNT; bar++) {
            float barHeight = this.rowHeight * BAR_LOWEST_SHARE + bar * step;

            canvas.fillRectangle(left + bar * (BAR_WIDTH + BAR_GAP), bottom - barHeight, BAR_WIDTH, barHeight,
                                 bar < lit ? color : theme.getFrameColor());
        }
    }
}
