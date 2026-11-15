package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Shows the selected entry and opens a list below itself to select another, on a {@link ComboBoxModel}: a press on the closed box
 * opens the list. While the list is open it takes the next press first, wherever it lands: a press on a row selects it, and the press's
 * release closes the list. Up and Down move the selection; Enter opens or closes the list, and losing the focus closes it.
 */
public final class ComboBox extends Control {
    /**
     * The selection of a combo box without a selected entry.
     */
    public static final int NO_SELECTION = ComboBoxModel.NO_SELECTION;

    private static final float TEXT_INSET = 3;
    /**
     * The half width of the drop-down arrow as a share of the box's height, the room the arrow takes in arrow half widths, and its line
     * thickness.
     */
    private static final float ARROW_SHARE = 0.16f;
    private static final float ARROW_ROOM = 3;
    private static final float ARROW_THICKNESS = 2;
    private static final float HALF = 2;

    private final int rowHeight;
    private final @NotNull ComboBoxModel<String> model;

    private boolean listPressed;
    private boolean selectionChanged;

    /**
     * Creates an empty, closed {@link ComboBox} instance.
     *
     * @param definition the control's definition
     * @param rowHeight  the height of one row of the open list, at least {@code 1}
     */
    public ComboBox(@NotNull DialogControl definition, int rowHeight) {
        super(definition);

        this.rowHeight = rowHeight;
        this.model     = new ComboBoxModel<>();

        this.listPressed      = false;
        this.selectionChanged = false;
    }

    /**
     * @return the entries in order, unmodifiable
     */
    public @NotNull List<String> getEntries() {
        return this.model.getItems();
    }

    /**
     * Sets the entries, clears the selection and closes the list.
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
     * Selects an entry.
     *
     * @param selection the entry index, or {@link #NO_SELECTION}; an index that is not an entry's clears the selection
     */
    public void setSelection(int selection) {
        this.model.select(selection);
    }

    /**
     * @return {@code true} if the list is open, {@code false} otherwise
     */
    public boolean isOpen() {
        return this.model.isOpen();
    }

    /**
     * @return {@code true} always: a combo box takes presses
     */
    @Override
    public boolean isPressable() {
        return true;
    }

    /**
     * @return {@code true} always: a combo box takes the focus
     */
    @Override
    public boolean isFocusable() {
        return true;
    }

    /**
     * @return {@code true} if the list is open over the other controls, {@code false} otherwise
     */
    @Override
    public boolean isOverlayOpen() {
        return this.model.isOpen();
    }


    /**
     * Checks whether a point is on the control: on the box, or anywhere while the list is open, which takes the next press first.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     * @return {@code true} if the list is open or the point is inside the box, {@code false} otherwise
     */
    @Override
    public boolean contains(float x, float y) {
        return this.model.isOpen() || super.contains(x, y);
    }

    /**
     * Called when a press goes to the control: a press on the closed box opens the list; a press while the list is open chooses the
     * row under it ({@link ComboBoxModel#chooseItem}), if any.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     */
    @Override
    protected void onPressed(float x, float y) {
        DialogControl definition = this.getDefinition();

        this.listPressed      = this.model.isOpen();
        this.selectionChanged = false;

        if (this.listPressed) {
            int item = this.model.findItem(x, y - definition.getHeight(), definition.getWidth(), this.rowHeight);

            this.selectionChanged = this.model.chooseItem(item);
        } else {
            this.model.setOpen(true);
        }
    }

    /**
     * Called when the press of the control is released on it: the press of an open list closes it.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     * @return {@code true} if the press selected another entry, {@code false} otherwise
     */
    @Override
    protected boolean onReleased(float x, float y) {
        if (this.listPressed) {
            this.model.setOpen(false);
        }

        return this.selectionChanged;
    }

    /**
     * Called when the control loses the focus: closes the list.
     */
    @Override
    protected void onFocusLost() {
        this.model.setOpen(false);
    }

    /**
     * Activates the control: opens or closes the list.
     *
     * @return {@code false} always: opening reports no change
     */
    @Override
    public boolean activate() {
        this.model.setOpen(!this.model.isOpen());

        return false;
    }

    /**
     * Takes a key while the control has the focus: Up and Down move the selection by one, from the first entry without a selection.
     *
     * @param key the key
     * @return {@code true} if the control used the key, {@code false} to let the dialog handle it
     */
    @Override
    public boolean handleKey(@NotNull DialogKey key) {
        if (key != DialogKey.UP && key != DialogKey.DOWN) {
            return false;
        }

        this.model.step(key == DialogKey.DOWN);

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
        String text = this.model.getSelection() == NO_SELECTION ? "" : this.model.getItem(this.model.getSelection());
        float width = definition.getWidth();
        float height = definition.getHeight();
        float inset = Math.max(TEXT_INSET, theme.getFieldPadding());
        float arrowSize = Math.max(height * ARROW_SHARE, 1);
        float arrowCenterX = x + width - inset - arrowSize;
        float arrowCenterY = y + height / HALF;
        int color = this.getTextColor(theme);

        theme.drawFieldBackground(canvas, x, y, width, height);
        canvas.setClip((int) (x + inset), (int) y, (int) (width - 2 * inset - ARROW_ROOM * arrowSize), (int) height);
        this.drawText(canvas, theme, text, x + inset, y, width - 2 * inset - ARROW_ROOM * arrowSize, height, color);
        canvas.setClip(0, 0, -1, -1);
        canvas.drawLine(arrowCenterX - arrowSize, arrowCenterY - arrowSize / HALF, arrowCenterX, arrowCenterY + arrowSize / HALF,
                        ARROW_THICKNESS, color);
        canvas.drawLine(arrowCenterX, arrowCenterY + arrowSize / HALF, arrowCenterX + arrowSize, arrowCenterY - arrowSize / HALF,
                        ARROW_THICKNESS, color);
        theme.drawFieldOutline(canvas, x, y, width, height, this.isFocused());
    }

    /**
     * Draws the open list below the box.
     *
     * @param canvas the canvas that draws
     * @param theme  the look of the controls
     * @param x      the control's left edge on the canvas
     * @param y      the control's top edge on the canvas
     */
    @Override
    protected void drawOverlay(@NotNull Canvas canvas, @NotNull DialogTheme theme, float x, float y) {
        if (!this.model.isOpen()) {
            return;
        }

        DialogControl definition = this.getDefinition();
        float listTop = y + definition.getHeight();
        float listHeight = this.model.getItemCount() * this.rowHeight;
        float inset = Math.max(TEXT_INSET, theme.getFieldPadding());

        canvas.fillRectangle(x, listTop, definition.getWidth(), listHeight, theme.getFillColor());
        theme.drawFieldBackground(canvas, x, listTop, definition.getWidth(), listHeight);

        for (int row = 0; row < this.model.getItemCount(); row++) {
            float rowTop = listTop + row * this.rowHeight;

            if (row == this.model.getSelection()) {
                theme.drawSelection(canvas, x, rowTop, definition.getWidth(), this.rowHeight);
            }

            theme.getFont().draw(canvas, this.model.getItem(row), x + inset, rowTop, theme.getTextColor());
        }

        canvas.drawRectangle(x, listTop, definition.getWidth(), listHeight, theme.getFrameThickness(), theme.getFrameColor());
    }
}
