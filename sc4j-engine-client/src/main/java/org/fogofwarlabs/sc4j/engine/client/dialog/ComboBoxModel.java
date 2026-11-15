package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Holds the items of a combo box, which of them can be chosen, the selected item and its open list: whether it is open, in which
 * direction and the item under the pointer. A combo box control draws the box and the list in its own look; the model holds no look.
 * <p>
 * The list opens only while the combo box takes input and has items. While it is open it takes the next press first, before every other
 * control: the press chooses the item under it ({@link #findItem}, {@link #chooseItem}) and closes the list, wherever it lands.
 *
 * @param <T> the type of the items
 */
public final class ComboBoxModel<T> {
    /**
     * The selection of a combo box without a selected item, and the item of a point outside the open list's items.
     */
    public static final int NO_SELECTION = -1;

    /**
     * Checks whether a list opens above its box: when it does not fit below the box and there is more room above.
     *
     * @param boxHeight  the height of the closed box
     * @param listHeight the height of the open list
     * @param roomAbove  the room from the top of the screen down to the box's top
     * @param roomBelow  the room from the box's top down to the bottom of the screen
     * @return {@code true} if the list opens upward, {@code false} otherwise
     */
    public static boolean isUpwardNeeded(float boxHeight, float listHeight, float roomAbove, float roomBelow) {
        return boxHeight + listHeight > roomBelow && roomAbove > roomBelow - boxHeight;
    }


    private final @NotNull List<T> items;
    private final @NotNull List<Boolean> itemsEnabled;

    private int selection;
    private boolean inputEnabled;
    private boolean open;
    private boolean upward;
    private int hoveredItem;

    /**
     * Creates an empty, closed {@link ComboBoxModel} instance that takes input.
     */
    public ComboBoxModel() {
        this.items        = new ArrayList<>();
        this.itemsEnabled = new ArrayList<>();

        this.selection    = NO_SELECTION;
        this.inputEnabled = true;
        this.open         = false;
        this.upward       = false;
        this.hoveredItem  = NO_SELECTION;
    }

    /**
     * @return the number of items
     */
    public int getItemCount() {
        return this.items.size();
    }

    /**
     * @param index the item index
     * @return the item
     * @throws IndexOutOfBoundsException if the index is not an item's
     */
    public @NotNull T getItem(int index) {
        return this.items.get(index);
    }

    /**
     * @return the items in order, unmodifiable
     */
    public @NotNull List<T> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    /**
     * Sets the items, each one that can be chosen; clears the selection and closes the list.
     *
     * @param items the items in order, copied
     */
    public void setItems(@NotNull Collection<? extends T> items) {
        this.clear();

        for (T item : items) {
            this.addItem(item);
        }
    }

    /**
     * Appends an item that can be chosen.
     *
     * @param item the item
     */
    public void addItem(@NotNull T item) {
        this.items.add(item);
        this.itemsEnabled.add(true);
    }

    /**
     * Removes the items, clears the selection and closes the list.
     */
    public void clear() {
        this.items.clear();
        this.itemsEnabled.clear();

        this.selection   = NO_SELECTION;
        this.open        = false;
        this.upward      = false;
        this.hoveredItem = NO_SELECTION;
    }

    /**
     * @param index the item index
     * @return {@code true} if the item can be chosen from the open list, {@code false} otherwise
     * @throws IndexOutOfBoundsException if the index is not an item's
     */
    public boolean isItemEnabled(int index) {
        return this.itemsEnabled.get(index);
    }

    /**
     * Sets whether an item can be chosen from the open list.
     *
     * @param index   the item index
     * @param enabled whether the item can be chosen
     * @throws IndexOutOfBoundsException if the index is not an item's
     */
    public void setItemEnabled(int index, boolean enabled) {
        this.itemsEnabled.set(index, enabled);
    }

    /**
     * @return the selected item's index, or {@link #NO_SELECTION}
     */
    public int getSelection() {
        return this.selection;
    }

    /**
     * Selects an item.
     *
     * @param index the item index; an index that is not an item's clears the selection
     */
    public void select(int index) {
        this.selection = index < 0 || index >= this.items.size() ? NO_SELECTION : index;
    }

    /**
     * Moves the selection by one item, stopping at the first and the last; without a selection the first item is selected.
     *
     * @param down {@code true} for the next item, {@code false} for the previous one
     */
    public void step(boolean down) {
        if (this.items.isEmpty()) {
            return;
        }

        int index = this.selection == NO_SELECTION ? 0 : this.selection + (down ? 1 : -1);

        this.select(Math.clamp(index, 0, this.items.size() - 1));
    }

    /**
     * @return {@code true} if the combo box takes input, {@code false} otherwise
     */
    public boolean isInputEnabled() {
        return this.inputEnabled;
    }

    /**
     * Sets whether the combo box takes input; one that does not closes its list.
     *
     * @param inputEnabled whether the combo box takes input
     */
    public void setInputEnabled(boolean inputEnabled) {
        this.inputEnabled = inputEnabled;

        if (!inputEnabled) {
            this.setOpen(false);
        }
    }

    /**
     * @return {@code true} if the list is open, {@code false} otherwise
     */
    public boolean isOpen() {
        return this.open;
    }

    /**
     * @return {@code true} if the open list lies above the box, {@code false} below it or while closed
     */
    public boolean isUpward() {
        return this.upward;
    }

    /**
     * Opens or closes the list below the box; no item is under the pointer yet.
     *
     * @param open whether the list is open; it opens only while the combo box takes input and has items
     */
    public void setOpen(boolean open) {
        this.setOpen(open, false);
    }

    /**
     * Opens or closes the list in a direction; no item is under the pointer yet.
     *
     * @param open   whether the list is open; it opens only while the combo box takes input and has items
     * @param upward whether the open list lies above the box, for example {@link #isUpwardNeeded}
     */
    public void setOpen(boolean open, boolean upward) {
        this.open        = open && this.inputEnabled && !this.items.isEmpty();
        this.upward      = this.open && upward;
        this.hoveredItem = NO_SELECTION;
    }

    /**
     * @return the item under the pointer in the open list, or {@link #NO_SELECTION}
     */
    public int getHoveredItem() {
        return this.hoveredItem;
    }

    /**
     * Sets the item under the pointer in the open list.
     *
     * @param index the item index, usually from {@link #findItem}, or {@link #NO_SELECTION}
     */
    public void setHoveredItem(int index) {
        this.hoveredItem = index;
    }


    /**
     * Finds the item that can be chosen at a point of the open list.
     *
     * @param offsetX   the point's offset right of the list's left edge
     * @param offsetY   the point's offset below the list's first row
     * @param width     the list's width
     * @param rowHeight the height of a row
     * @return the item index, or {@link #NO_SELECTION} while the list is closed, outside its rows and on an item that cannot be chosen
     */
    public int findItem(float offsetX, float offsetY, float width, float rowHeight) {
        if (!this.open || offsetX < 0 || offsetX >= width || offsetY < 0) {
            return NO_SELECTION;
        }

        int index = (int) (offsetY / rowHeight);

        if (index >= this.items.size() || !this.itemsEnabled.get(index)) {
            return NO_SELECTION;
        }

        return index;
    }

    /**
     * Chooses the item a press of the open list landed on: it becomes the selection when it is an item other than the selected one. The
     * press also closes the list; the owner closes it when its press ends.
     *
     * @param index the item index from {@link #findItem}, or {@link #NO_SELECTION} for a press outside the items
     * @return {@code true} if the selection changed, {@code false} otherwise
     */
    public boolean chooseItem(int index) {
        if (index == NO_SELECTION || index == this.selection) {
            return false;
        }

        this.select(index);

        return true;
    }
}
