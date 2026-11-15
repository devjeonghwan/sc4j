package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the combo box model: selection, opening only while it takes input and has items, the upward rule, the item under the
 * pointer and the open list's press.
 */
final class ComboBoxModelTest {
    private static final float WIDTH = 80;
    private static final float ROW_HEIGHT = 12;
    private static final float BAR_HEIGHT = 14;

    private static @NotNull ComboBoxModel<String> createModel() {
        ComboBoxModel<String> model = new ComboBoxModel<>();

        model.setItems(List.of("Red", "Green", "Blue"));

        return model;
    }

    @Test
    void set_open_should_open_only_with_items_while_taking_input() {
        ComboBoxModel<String> model = new ComboBoxModel<>();

        model.setOpen(true);

        Assertions.assertFalse(model.isOpen(), "no items");

        model.addItem("Red");
        model.setInputEnabled(false);
        model.setOpen(true);

        Assertions.assertFalse(model.isOpen(), "no input");

        model.setInputEnabled(true);
        model.setOpen(true, true);

        Assertions.assertTrue(model.isOpen());
        Assertions.assertTrue(model.isUpward());

        model.setInputEnabled(false);

        Assertions.assertFalse(model.isOpen(), "a combo box that takes no input closes");
        Assertions.assertFalse(model.isUpward());
    }

    @Test
    void is_upward_needed_should_hold_only_without_room_below_and_more_room_above() {
        float listHeight = 3 * ROW_HEIGHT;

        Assertions.assertTrue(ComboBoxModel.isUpwardNeeded(BAR_HEIGHT, listHeight, 400, 40));
        Assertions.assertFalse(ComboBoxModel.isUpwardNeeded(BAR_HEIGHT, listHeight, 400, 60), "the list fits below");
        Assertions.assertFalse(ComboBoxModel.isUpwardNeeded(BAR_HEIGHT, listHeight, 10, 40), "less room above");
    }

    @Test
    void find_item_should_return_the_enabled_row_under_the_pointer_of_the_open_list() {
        ComboBoxModel<String> model = createModel();

        Assertions.assertEquals(ComboBoxModel.NO_SELECTION, model.findItem(1, 1, WIDTH, ROW_HEIGHT), "the list is closed");

        model.setOpen(true);
        model.setItemEnabled(1, false);

        Assertions.assertEquals(0, model.findItem(1, 1, WIDTH, ROW_HEIGHT));
        Assertions.assertEquals(ComboBoxModel.NO_SELECTION, model.findItem(1, ROW_HEIGHT + 1, WIDTH, ROW_HEIGHT), "a disabled item");
        Assertions.assertEquals(2, model.findItem(1, 2 * ROW_HEIGHT + 1, WIDTH, ROW_HEIGHT));
        Assertions.assertEquals(ComboBoxModel.NO_SELECTION, model.findItem(1, 3 * ROW_HEIGHT + 1, WIDTH, ROW_HEIGHT), "below the list");
        Assertions.assertEquals(ComboBoxModel.NO_SELECTION, model.findItem(1, -1, WIDTH, ROW_HEIGHT), "above the list");
        Assertions.assertEquals(ComboBoxModel.NO_SELECTION, model.findItem(WIDTH, 1, WIDTH, ROW_HEIGHT), "right of the list");
    }

    @Test
    void choose_item_should_select_only_another_item() {
        ComboBoxModel<String> model = createModel();

        model.select(1);

        Assertions.assertFalse(model.chooseItem(ComboBoxModel.NO_SELECTION));
        Assertions.assertFalse(model.chooseItem(1));
        Assertions.assertTrue(model.chooseItem(2));
        Assertions.assertEquals(2, model.getSelection());

        model.select(3);

        Assertions.assertEquals(ComboBoxModel.NO_SELECTION, model.getSelection(), "an index out of range clears the selection");
    }

    @Test
    void step_should_start_at_the_first_item_and_stop_at_the_ends() {
        ComboBoxModel<String> model = createModel();

        model.step(false);

        Assertions.assertEquals(0, model.getSelection());

        model.step(false);

        Assertions.assertEquals(0, model.getSelection());

        model.select(2);
        model.step(true);

        Assertions.assertEquals(2, model.getSelection());

        model.setOpen(true);
        model.setItems(List.of("Red"));

        Assertions.assertFalse(model.isOpen(), "new items close the list");
        Assertions.assertEquals(ComboBoxModel.NO_SELECTION, model.getSelection());
    }
}
