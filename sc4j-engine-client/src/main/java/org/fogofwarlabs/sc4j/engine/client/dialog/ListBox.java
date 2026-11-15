package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Lists entries in rows of a fixed height to select one from, on a {@link ListModel}: a press selects the row under it, Up and Down
 * move the selection by one, Page Up and Page Down by the shown rows less one, Home and End to the ends, and the wheel scrolls. The
 * selection stays visible when it moves.
 */
public final class ListBox extends Control {
    /**
     * The selection of a list without a selected entry.
     */
    public static final int NO_SELECTION = ListModel.NO_SELECTION;

    private static final float TEXT_INSET = 3;

    private final int rowHeight;
    private final @NotNull ListModel<String> model;

    /**
     * Creates an empty {@link ListBox} instance.
     *
     * @param definition the control's definition
     * @param rowHeight  the height of one row, at least {@code 1}
     */
    public ListBox(@NotNull DialogControl definition, int rowHeight) {
        super(definition);

        this.rowHeight = rowHeight;
        this.model     = new ListModel<>();
    }

    /**
     * @return the entries in order, unmodifiable
     */
    public @NotNull List<String> getEntries() {
        return this.model.getItems();
    }

    /**
     * Sets the entries and clears the selection.
     *
     * @param entries the entries in order, copied
     */
    public void setEntries(@NotNull List<String> entries) {
        this.model.setItems(entries);
    }

    /**
     * @return the index of the selected entry, or {@link #NO_SELECTION}
     */
    public int getSelection() {
        return this.model.getSelection();
    }

    /**
     * Selects an entry and scrolls it into view.
     *
     * @param selection the entry index, or {@link #NO_SELECTION}; an index that is not an entry's clears the selection
     */
    public void setSelection(int selection) {
        this.model.select(selection, this.getVisibleRowCount());
    }

    /**
     * @return the index of the entry in the top row
     */
    public int getFirstVisibleRow() {
        return this.model.getFirstRow();
    }

    /**
     * @return the number of whole rows the list shows, at least {@code 1}
     */
    public int getVisibleRowCount() {
        return Math.max(1, this.getDefinition().getHeight() / this.rowHeight);
    }

    /**
     * @return {@code true} always: a list takes presses
     */
    @Override
    public boolean isPressable() {
        return true;
    }

    /**
     * @return {@code true} always: a list takes the focus
     */
    @Override
    public boolean isFocusable() {
        return true;
    }


    /**
     * Called when a press goes to the control: selects the row under the pointer.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     */
    @Override
    protected void onPressed(float x, float y) {
        this.model.selectAt(y, this.rowHeight, this.getVisibleRowCount());
    }

    /**
     * Activates the control: a list reports no activation, so Enter reaches the dialog's default control.
     *
     * @return {@code false} always
     */
    @Override
    public boolean activate() {
        return false;
    }

    /**
     * Called when the press of the control is released on it: the press selected already, so the list reports the change.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     * @return {@code true} if an entry is selected, {@code false} otherwise
     */
    @Override
    protected boolean onReleased(float x, float y) {
        return this.model.getSelection() != NO_SELECTION;
    }

    /**
     * Takes a key while the control has the focus: Up and Down move the selection by one, from the first entry without a selection;
     * Page Up, Page Down, Home and End move it as {@link ListModel#moveSelection} does.
     *
     * @param key the key
     * @return {@code true} if the control used the key, {@code false} to let the dialog handle it
     */
    @Override
    public boolean handleKey(@NotNull DialogKey key) {
        switch (key) {
            case UP, DOWN -> this.model.step(key == DialogKey.DOWN, this.getVisibleRowCount());
            case HOME, END, PAGE_UP, PAGE_DOWN -> this.model.moveSelection(key, this.getVisibleRowCount());
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
        this.model.scrollTo(this.model.getFirstRow() + steps, this.getVisibleRowCount());

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
        int color = this.isEnabled() ? theme.getTextColor() : theme.getDisabledTextColor();
        float inset = Math.max(TEXT_INSET, theme.getFieldPadding());
        int firstRow = this.model.getFirstRow();
        int lastRow = Math.min(this.model.getCount(), firstRow + this.getVisibleRowCount());

        theme.drawFieldBackground(canvas, x, y, definition.getWidth(), definition.getHeight());

        for (int row = firstRow; row < lastRow; row++) {
            float rowTop = y + (row - firstRow) * this.rowHeight;

            if (row == this.model.getSelection()) {
                theme.drawSelection(canvas, x, rowTop, definition.getWidth(), this.rowHeight);
            }

            theme.getFont().draw(canvas, this.model.getItem(row), x + inset, rowTop, color);
        }

        theme.drawFieldOutline(canvas, x, y, definition.getWidth(), definition.getHeight(), this.isFocused());
    }
}
