package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the replay window of a channel direction: each counter is accepted once, late counters only inside the window, and a jump forward
 * forgets what fell out of the window.
 */
final class ReplayWindowTest {
    private static final long FAR_AHEAD = 10_000L;

    @Test
    void accept_should_take_each_counter_once_and_late_ones_inside_the_window() {
        ReplayWindow window = new ReplayWindow();

        Assertions.assertEquals(-1L, window.getHighest());
        Assertions.assertFalse(window.isFresh(-1L));
        Assertions.assertTrue(window.isFresh(5L));
        Assertions.assertTrue(window.accept(5L));
        Assertions.assertFalse(window.isFresh(5L));
        Assertions.assertTrue(window.isFresh(3L));
        Assertions.assertFalse(window.accept(3L));
        Assertions.assertFalse(window.isFresh(3L));
        Assertions.assertTrue(window.isFresh(4L));

        Assertions.assertTrue(window.accept(5L + ReplayWindow.SIZE));
        Assertions.assertFalse(window.isFresh(5L), "a counter at the window's edge is fresh");
        Assertions.assertTrue(window.isFresh(6L));
        Assertions.assertFalse(window.isFresh(5L + ReplayWindow.SIZE));

        Assertions.assertTrue(window.accept(FAR_AHEAD));
        Assertions.assertEquals(FAR_AHEAD, window.getHighest());
        Assertions.assertTrue(window.isFresh(FAR_AHEAD - 1L));
        Assertions.assertFalse(window.isFresh(FAR_AHEAD - ReplayWindow.SIZE));
    }
}
