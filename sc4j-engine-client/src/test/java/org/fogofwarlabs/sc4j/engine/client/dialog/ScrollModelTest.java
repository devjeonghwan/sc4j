package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the scroll model: the parts under the pointer, steps, pages, the thumb's place and drag, and the repeat of a held part.
 */
final class ScrollModelTest {
    private static final int LENGTH = 100;
    private static final int THUMB = 10;
    private static final int TRAVEL = LENGTH - THUMB;
    private static final float STEP_END = 2;
    private static final float STEP_START = LENGTH - 2;
    private static final int POINTER_LIMIT = LENGTH - 1 - THUMB / 2;

    private static @NotNull ScrollPart findPart(@NotNull ScrollModel model, float offset) {
        return model.findPart(offset, STEP_END, STEP_START, STEP_END + model.getThumbOffset(TRAVEL), THUMB);
    }

    @Test
    void press_should_step_page_and_drag_the_thumb() {
        ScrollModel model = new ScrollModel();

        model.setRange(0, 10);
        model.setPage(2);

        Assertions.assertEquals(ScrollPart.STEP_BACK, findPart(model, 1));
        Assertions.assertFalse(model.press(ScrollPart.STEP_BACK), "the value stays at the minimum");
        Assertions.assertEquals(ScrollPart.STEP_FORWARD, findPart(model, STEP_START));
        Assertions.assertTrue(model.press(ScrollPart.STEP_FORWARD));
        Assertions.assertEquals(1, model.getValue());
        Assertions.assertEquals(9, model.getThumbOffset(TRAVEL), "(1 * 90 + 5) / 10");

        model.release();

        Assertions.assertEquals(ScrollPart.PAGE_FORWARD, findPart(model, 50));
        Assertions.assertTrue(model.press(ScrollPart.PAGE_FORWARD));
        Assertions.assertEquals(3, model.getValue());

        model.release();

        Assertions.assertEquals(ScrollPart.PAGE_BACK, findPart(model, 20));
        Assertions.assertEquals(ScrollPart.THUMB, findPart(model, 30));
        Assertions.assertFalse(model.press(ScrollPart.THUMB));
        Assertions.assertTrue(model.isDragging());
        Assertions.assertTrue(model.dragTo(60, 0, POINTER_LIMIT, THUMB, TRAVEL));
        Assertions.assertEquals(6, model.getValue(), "11 * 55 / 90");
        Assertions.assertEquals(55, model.getThumbOffset(TRAVEL), "the dragged thumb follows the pointer");

        model.release();

        Assertions.assertFalse(model.isDragging());
        Assertions.assertFalse(model.dragTo(0, 0, POINTER_LIMIT, THUMB, TRAVEL), "no drag after the release");
        Assertions.assertEquals(54, model.getThumbOffset(TRAVEL), "(6 * 90 + 5) / 10");
    }

    @Test
    void get_thumb_offset_should_rest_at_the_ends_of_the_travel() {
        ScrollModel model = new ScrollModel();

        Assertions.assertEquals(0, model.getThumbOffset(TRAVEL), "an empty range");

        model.setRange(5, 8);
        model.setValue(8);

        Assertions.assertEquals(TRAVEL, model.getThumbOffset(TRAVEL));

        model.setValue(-1);

        Assertions.assertEquals(5, model.getValue(), "the value is clamped to the range");
        Assertions.assertEquals(0, model.getThumbOffset(TRAVEL));
    }

    @Test
    void repeat_should_step_held_arrows_every_tick_and_held_pages_every_third_tick_under_the_pointer() {
        ScrollModel model = new ScrollModel();

        model.setRange(0, 20);
        model.setPage(5);
        model.press(ScrollPart.STEP_FORWARD);

        Assertions.assertEquals(1, model.getValue(), "the press steps at once");
        Assertions.assertTrue(model.repeat());
        Assertions.assertTrue(model.repeat());
        Assertions.assertEquals(3, model.getValue());

        model.movePointer(ScrollPart.PAGE_FORWARD);

        Assertions.assertFalse(model.isPartPressed(ScrollPart.STEP_FORWARD));
        Assertions.assertFalse(model.repeat(), "the pointer left the held part");

        model.movePointer(ScrollPart.STEP_FORWARD);

        Assertions.assertTrue(model.repeat());
        Assertions.assertEquals(4, model.getValue());

        model.release();
        model.press(ScrollPart.PAGE_FORWARD);

        Assertions.assertEquals(9, model.getValue());
        Assertions.assertTrue(model.isPartPressed(ScrollPart.PAGE_FORWARD));
        Assertions.assertFalse(model.repeat());
        Assertions.assertFalse(model.repeat());
        Assertions.assertTrue(model.repeat());
        Assertions.assertEquals(14, model.getValue());

        model.release();

        Assertions.assertEquals(ScrollPart.NONE, model.getHeldPart());
        Assertions.assertFalse(model.repeat());
    }
}
