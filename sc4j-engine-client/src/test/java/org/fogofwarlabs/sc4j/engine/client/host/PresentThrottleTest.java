package org.fogofwarlabs.sc4j.engine.client.host;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests that the present throttle lets every pass present while inactive and only one pass per draw interval while active, lengthened by a
 * frame rate limit whose period is longer.
 */
final class PresentThrottleTest {
    private static final int DRAW_INTERVAL = 16;
    private static final long START = 1000;
    /**
     * A frame rate limit with a period longer than the draw interval ({@code 33.3} ms, presented every {@code 34}) and one with a shorter period.
     */
    private static final int LOW_FRAME_RATE_LIMIT = 30;
    private static final int LOW_LIMIT_INTERVAL = 34;
    private static final int HIGH_FRAME_RATE_LIMIT = 144;

    @Test
    void inactive_throttle_presents_every_pass() {
        PresentThrottle throttle = new PresentThrottle(DRAW_INTERVAL);

        Assertions.assertFalse(throttle.isActive());
        Assertions.assertTrue(throttle.takePresentDue(START));
        Assertions.assertTrue(throttle.takePresentDue(START));
        Assertions.assertTrue(throttle.takePresentDue(START + 1));
    }

    @Test
    void active_throttle_presents_once_per_draw_interval_from_its_start() {
        PresentThrottle throttle = new PresentThrottle(DRAW_INTERVAL);

        throttle.setActive(true, START);

        Assertions.assertFalse(throttle.takePresentDue(START), "the first interval starts at the activation");
        Assertions.assertFalse(throttle.takePresentDue(START + DRAW_INTERVAL - 1));
        Assertions.assertTrue(throttle.takePresentDue(START + DRAW_INTERVAL));
        Assertions.assertFalse(throttle.takePresentDue(START + DRAW_INTERVAL + 1), "the next interval starts at the presented pass");
        // A late pass starts the next interval at its own time, not at the missed deadline.
        Assertions.assertTrue(throttle.takePresentDue(START + 3 * DRAW_INTERVAL + 5));
        Assertions.assertFalse(throttle.takePresentDue(START + 4 * DRAW_INTERVAL + 4));
        Assertions.assertTrue(throttle.takePresentDue(START + 4 * DRAW_INTERVAL + 5));

        throttle.setActive(false, START + 4 * DRAW_INTERVAL + 6);

        Assertions.assertTrue(throttle.takePresentDue(START + 4 * DRAW_INTERVAL + 6));
    }

    @Test
    void frame_rate_limit_lengthens_the_draw_interval_only_when_its_period_is_longer() {
        PresentThrottle throttle = new PresentThrottle(DRAW_INTERVAL);

        throttle.setFrameRateLimit(HIGH_FRAME_RATE_LIMIT);

        Assertions.assertEquals(DRAW_INTERVAL, throttle.getPresentIntervalMilliseconds());

        throttle.setFrameRateLimit(LOW_FRAME_RATE_LIMIT);
        throttle.setActive(true, START);

        Assertions.assertEquals(LOW_LIMIT_INTERVAL, throttle.getPresentIntervalMilliseconds());
        Assertions.assertFalse(throttle.takePresentDue(START + DRAW_INTERVAL), "the limit's period, not the draw interval");
        Assertions.assertFalse(throttle.takePresentDue(START + LOW_LIMIT_INTERVAL - 1));
        Assertions.assertTrue(throttle.takePresentDue(START + LOW_LIMIT_INTERVAL));
        Assertions.assertFalse(throttle.takePresentDue(START + 2 * LOW_LIMIT_INTERVAL - 1));
        Assertions.assertTrue(throttle.takePresentDue(START + 2 * LOW_LIMIT_INTERVAL));

        throttle.setFrameRateLimit(0);

        Assertions.assertTrue(throttle.takePresentDue(START + 2 * LOW_LIMIT_INTERVAL + DRAW_INTERVAL), "no limit: the draw interval again");

        throttle.setActive(false, START + 3 * LOW_LIMIT_INTERVAL);
        throttle.setFrameRateLimit(LOW_FRAME_RATE_LIMIT);

        Assertions.assertTrue(throttle.takePresentDue(START + 3 * LOW_LIMIT_INTERVAL), "an inactive throttle leaves the limit to the host");
        Assertions.assertTrue(throttle.takePresentDue(START + 3 * LOW_LIMIT_INTERVAL));
    }

    @Test
    void repeated_activation_keeps_the_running_interval() {
        PresentThrottle throttle = new PresentThrottle(DRAW_INTERVAL);

        throttle.setActive(true, START);
        throttle.setActive(true, START + DRAW_INTERVAL - 1);

        Assertions.assertTrue(throttle.takePresentDue(START + DRAW_INTERVAL));
    }

    @Test
    void constructor_should_reject_an_interval_that_is_not_positive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PresentThrottle(0));
    }

    @Test
    void getters_should_return_the_draw_interval_and_the_set_frame_rate_limit() {
        PresentThrottle throttle = new PresentThrottle(DRAW_INTERVAL);

        Assertions.assertEquals(DRAW_INTERVAL, throttle.getDrawIntervalMilliseconds());
        Assertions.assertEquals(0, throttle.getFrameRateLimit());

        throttle.setFrameRateLimit(LOW_FRAME_RATE_LIMIT);

        Assertions.assertEquals(LOW_FRAME_RATE_LIMIT, throttle.getFrameRateLimit());
    }

    @Test
    void set_frame_rate_limit_should_reject_a_negative_limit() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PresentThrottle(DRAW_INTERVAL).setFrameRateLimit(-1));
    }
}
