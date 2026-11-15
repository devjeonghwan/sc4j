package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the view scroll model: the bounded offset, following the end of growing content, and the thumb's length, place and drag.
 */
final class ViewScrollModelTest {
    private static final float CONTENT = 400;
    private static final float VIEW = 100;
    private static final float TRACK = 80;
    private static final float MINIMUM_THUMB = 8;

    @Test
    void set_content_should_follow_the_end_until_the_view_scrolls_away() {
        ViewScrollModel model = new ViewScrollModel();

        model.setContent(CONTENT, VIEW);

        Assertions.assertEquals(CONTENT - VIEW, model.getOffset(), "new content starts at its end");
        Assertions.assertTrue(model.isScrollable());

        model.scrollBy(-VIEW);
        model.setContent(CONTENT + VIEW, VIEW);

        Assertions.assertEquals(CONTENT - 2 * VIEW, model.getOffset(), "scrolled away, the offset stays");

        model.scrollBy(-CONTENT);

        Assertions.assertEquals(0, model.getOffset());

        model.scrollToEnd();
        model.setContent(CONTENT + 2 * VIEW, VIEW);

        Assertions.assertEquals(CONTENT + VIEW, model.getOffset());

        model.setContent(VIEW / 2, VIEW);

        Assertions.assertEquals(0, model.getOffset());
        Assertions.assertFalse(model.isScrollable());
    }

    @Test
    void thumb_maps_the_offset_onto_the_track_and_back() {
        ViewScrollModel model = new ViewScrollModel();

        model.setContent(CONTENT, VIEW);

        Assertions.assertEquals(TRACK * VIEW / CONTENT, model.getThumbLength(TRACK, MINIMUM_THUMB));
        Assertions.assertEquals(TRACK - TRACK * VIEW / CONTENT, model.getThumbOffset(TRACK, MINIMUM_THUMB), "the thumb rests at the end");

        model.dragThumb(30, TRACK, MINIMUM_THUMB);

        Assertions.assertEquals(30 / (TRACK - TRACK * VIEW / CONTENT) * (CONTENT - VIEW), model.getOffset());

        model.setContent(CONTENT * 100, VIEW);

        Assertions.assertEquals(MINIMUM_THUMB, model.getThumbLength(TRACK, MINIMUM_THUMB), "a short thumb keeps its minimum length");
    }
}
