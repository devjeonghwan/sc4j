package org.fogofwarlabs.sc4j.engine.client.input;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the pairing of button presses into double presses.
 */
final class DoublePressDetectorTest {
    private static final long START = 10_000;
    private static final long SHORT_DELAY = 200;
    private static final int X = 40;
    private static final int Y = 30;

    @Test
    void press_should_pair_presses_at_the_same_pixel_within_the_interval() {
        DoublePressDetector detector = new DoublePressDetector();

        Assertions.assertFalse(detector.press(PointerButton.LEFT, START, X, Y));
        Assertions.assertTrue(detector.press(PointerButton.LEFT, START + SHORT_DELAY, X, Y));
        Assertions.assertFalse(detector.press(PointerButton.LEFT, START + SHORT_DELAY * 2, X, Y), "a pair starts over");
        Assertions.assertFalse(detector.press(PointerButton.LEFT, START + SHORT_DELAY * 2 + DoublePressDetector.DEFAULT_TIME_LIMIT, X, Y),
                               "too late");
        Assertions.assertFalse(detector.press(PointerButton.LEFT, START + SHORT_DELAY * 3 + DoublePressDetector.DEFAULT_TIME_LIMIT, X + 1, Y),
                               "another pixel");
    }

    @Test
    void press_should_track_buttons_separately_and_ignore_other_buttons() {
        DoublePressDetector detector = new DoublePressDetector(SHORT_DELAY * 2);

        Assertions.assertFalse(detector.press(PointerButton.LEFT, START, X, Y));
        Assertions.assertFalse(detector.press(PointerButton.RIGHT, START + 1, X, Y));
        Assertions.assertFalse(detector.press(PointerButton.BUTTON_4, START + 2, X + 1, Y), "other buttons leave the state");
        Assertions.assertFalse(detector.press(PointerButton.BUTTON_4, START + 3, X + 1, Y));
        Assertions.assertTrue(detector.press(PointerButton.LEFT, START + SHORT_DELAY, X, Y));
        Assertions.assertTrue(detector.press(PointerButton.RIGHT, START + SHORT_DELAY, X, Y));
        Assertions.assertEquals(SHORT_DELAY * 2, detector.getTimeLimit());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new DoublePressDetector(0));
    }
}
