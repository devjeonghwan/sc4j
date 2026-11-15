package org.fogofwarlabs.sc4j.engine.client.cursor;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.game.SkirmishScenario;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests the cursor set: its pictures with their fallbacks, the order pictures of a waiting action, and the scroll contexts.
 */
final class CursorSetTest {
    private static @NotNull DetailSet<Picture> createCursor(int hotSpot) {
        return DetailSet.of(new Picture(2, 2, PixelLayer.ofRgba(new int[4]), null, hotSpot, hotSpot, 0, 0, 0, 0));
    }

    @Test
    void find_cursor_should_fall_back_and_prefer_the_waiting_order_picture() throws IOException {
        SkirmishScenario scenario = new SkirmishSessionScenario().getScenario();
        DetailSet<Picture> arrow = createCursor(0);
        DetailSet<Picture> target = createCursor(1);
        DetailSet<Picture> move = createCursor(1);
        DetailSet<Picture> scrollUp = createCursor(0);
        CursorSet cursors = new CursorSet();

        cursors.setCursor(CursorContext.NORMAL, arrow);
        cursors.setCursor(CursorContext.TARGET, target);
        cursors.setCursor(CursorContext.SCROLL_UP, scrollUp);
        cursors.setOrderCursor(scenario.getMove(), move);

        Assertions.assertSame(arrow, cursors.findCursor(CursorContext.HOVER_ENEMY, null));
        Assertions.assertSame(arrow, cursors.findCursor(CursorContext.PLACE_INVALID, null), "an invalid site falls back to the site, then the arrow");
        Assertions.assertSame(scrollUp, cursors.findCursor(CursorContext.SCROLL_UP_LEFT, null));
        Assertions.assertSame(move, cursors.findCursor(CursorContext.TARGET, scenario.getMove()));
        Assertions.assertSame(target, cursors.findCursor(CursorContext.TARGET, scenario.getPatrol()));
        Assertions.assertNull(new CursorSet().findCursor(CursorContext.NORMAL, null), "the platform arrow");
        Assertions.assertEquals(CursorContext.SCROLL_DOWN_RIGHT, CursorContext.ofScroll(1, 1));
        Assertions.assertNull(CursorContext.ofScroll(0, 0));
    }
}
