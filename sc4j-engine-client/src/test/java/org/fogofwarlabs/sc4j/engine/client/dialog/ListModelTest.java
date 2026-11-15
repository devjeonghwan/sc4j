package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the list model: selection and scrolling into view, item changes, navigation keys, the wheel and row presses.
 */
final class ListModelTest {
    private static final int ITEMS = 10;
    private static final int VISIBLE_ROWS = 3;
    private static final float ROW_HEIGHT = 16;

    private static @NotNull ListModel<String> createModel() {
        List<String> items = new ArrayList<>();

        for (int index = 0; index < ITEMS; index++) {
            items.add("row " + index);
        }

        ListModel<String> model = new ListModel<>();

        model.setItems(items);

        return model;
    }

    @Test
    void select_should_scroll_the_selection_into_view_and_clear_it_out_of_range() {
        ListModel<String> model = createModel();

        model.select(5, VISIBLE_ROWS);

        Assertions.assertEquals(5, model.getSelection());
        Assertions.assertEquals(3, model.getFirstRow(), "the selection is the last visible row");

        model.select(1, VISIBLE_ROWS);

        Assertions.assertEquals(1, model.getFirstRow(), "the selection is the first visible row");

        model.select(ITEMS, VISIBLE_ROWS);

        Assertions.assertEquals(ListModel.NO_SELECTION, model.getSelection());
        Assertions.assertEquals(1, model.getFirstRow());

        model.scrollTo(ITEMS, VISIBLE_ROWS);

        Assertions.assertEquals(ITEMS - VISIBLE_ROWS, model.getFirstRow(), "the last rows fill the list");
    }

    @Test
    void item_changes_keep_the_selection_on_its_item() {
        ListModel<String> model = createModel();

        model.select(4, VISIBLE_ROWS);
        model.insertItem(2, "inserted");

        Assertions.assertEquals(5, model.getSelection());
        Assertions.assertEquals("row 4", model.getItem(model.getSelection()));

        model.select(ITEMS, VISIBLE_ROWS);
        model.removeItem(0);

        Assertions.assertEquals(ITEMS - 1, model.getSelection(), "a selection past the end moves to the last row");

        model.setItems(List.of("only"));

        Assertions.assertEquals(ListModel.NO_SELECTION, model.getSelection());
        Assertions.assertEquals(0, model.getFirstRow());

        model.select(0, VISIBLE_ROWS);
        model.removeItem(0);

        Assertions.assertEquals(ListModel.NO_SELECTION, model.getSelection());
    }

    @Test
    void move_selection_should_select_only_on_home_or_end_without_selection() {
        ListModel<String> model = createModel();

        for (DialogKey key : new DialogKey[]{DialogKey.UP, DialogKey.DOWN, DialogKey.PAGE_UP, DialogKey.PAGE_DOWN}) {
            model.moveSelection(key, VISIBLE_ROWS);

            Assertions.assertEquals(ListModel.NO_SELECTION, model.getSelection(), key + " needs a selection");
        }

        model.moveSelection(DialogKey.END, VISIBLE_ROWS);

        Assertions.assertEquals(ITEMS - 1, model.getSelection());

        model.moveSelection(DialogKey.HOME, VISIBLE_ROWS);

        Assertions.assertEquals(0, model.getSelection());
        Assertions.assertEquals(0, model.getFirstRow());
        Assertions.assertThrows(IllegalArgumentException.class, () -> model.moveSelection(DialogKey.TAB, VISIBLE_ROWS));
    }

    @Test
    void move_selection_should_page_by_visible_rows_less_one_and_clamp_to_ends() {
        ListModel<String> model = createModel();

        model.moveSelection(DialogKey.HOME, VISIBLE_ROWS);
        model.moveSelection(DialogKey.PAGE_DOWN, VISIBLE_ROWS);

        Assertions.assertEquals(2, model.getSelection(), "0 + 3 - 1");

        model.moveSelection(DialogKey.PAGE_DOWN, VISIBLE_ROWS);

        Assertions.assertEquals(4, model.getSelection());
        Assertions.assertEquals(2, model.getFirstRow(), "the selection stays in view");

        model.moveSelection(DialogKey.PAGE_UP, VISIBLE_ROWS);

        Assertions.assertEquals(2, model.getSelection(), "4 - 3 + 1");

        model.moveSelection(DialogKey.PAGE_UP, VISIBLE_ROWS);

        Assertions.assertEquals(0, model.getSelection(), "2 is not past the visible rows: the first row");

        model.select(ITEMS - 2, VISIBLE_ROWS);
        model.moveSelection(DialogKey.PAGE_DOWN, VISIBLE_ROWS);

        Assertions.assertEquals(ITEMS - 1, model.getSelection(), "8 + 3 is past the last row: the last row");

        model.moveSelection(DialogKey.UP, VISIBLE_ROWS);

        Assertions.assertEquals(ITEMS - 2, model.getSelection());

        model.moveSelection(DialogKey.DOWN, VISIBLE_ROWS);
        model.moveSelection(DialogKey.DOWN, VISIBLE_ROWS);

        Assertions.assertEquals(ITEMS - 1, model.getSelection(), "Down stops at the last row");
    }

    @Test
    void step_should_start_at_the_first_row_and_stop_at_the_ends() {
        ListModel<String> model = createModel();

        model.step(false, VISIBLE_ROWS);

        Assertions.assertEquals(0, model.getSelection(), "without a selection both directions start at the first row");

        model.step(false, VISIBLE_ROWS);

        Assertions.assertEquals(0, model.getSelection());

        model.select(ITEMS - 1, VISIBLE_ROWS);
        model.step(true, VISIBLE_ROWS);

        Assertions.assertEquals(ITEMS - 1, model.getSelection());
    }

    @Test
    void scroll_by_wheel_should_scroll_with_a_scroll_bar_and_page_the_selection_without_one() {
        ListModel<String> model = createModel();

        model.scrollByWheel(true, VISIBLE_ROWS, true);

        Assertions.assertEquals(1, model.getFirstRow());
        Assertions.assertEquals(ListModel.NO_SELECTION, model.getSelection());

        model.scrollByWheel(true, VISIBLE_ROWS, false);

        Assertions.assertEquals(ListModel.NO_SELECTION, model.getSelection(), "without a selection the wheel does nothing");

        model.select(0, VISIBLE_ROWS);
        model.scrollByWheel(true, VISIBLE_ROWS, false);

        Assertions.assertEquals(2, model.getSelection(), "0 + 3 - 1");

        model.scrollByWheel(false, VISIBLE_ROWS, false);

        Assertions.assertEquals(0, model.getSelection(), "2 is not past the visible rows: the first row");
    }

    @Test
    void select_at_should_select_the_row_under_an_offset_from_the_first_shown_row() {
        ListModel<String> model = createModel();

        model.scrollTo(2, VISIBLE_ROWS);

        Assertions.assertTrue(model.selectAt(ROW_HEIGHT + 1, ROW_HEIGHT, VISIBLE_ROWS));
        Assertions.assertEquals(3, model.getSelection());
        Assertions.assertFalse(model.selectAt(-1, ROW_HEIGHT, VISIBLE_ROWS), "above the rows");
        Assertions.assertFalse(model.selectAt(ROW_HEIGHT * ITEMS, ROW_HEIGHT, VISIBLE_ROWS), "below the last row");
        Assertions.assertEquals(3, model.getSelection());
    }
}
