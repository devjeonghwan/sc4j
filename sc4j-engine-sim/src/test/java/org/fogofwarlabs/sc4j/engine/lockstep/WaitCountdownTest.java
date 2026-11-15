package org.fogofwarlabs.sc4j.engine.lockstep;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the waiting dialog's countdown: it opens with the full count, counts one second down only after more than a step time, stops at
 * zero, and a dialog reopened within the resume time continues below its last count.
 */
final class WaitCountdownTest {
    private static final int SECONDS = 5;
    private static final long STEP_MILLIS = 1000L;
    private static final long RESUME_MILLIS = 10_000L;
    private static final int RESUME_REDUCTION_SECONDS = 2;

    @Test
    void advance_should_open_the_dialog_and_count_down_after_more_than_a_step() {
        WaitCountdown countdown = new WaitCountdown(SECONDS, STEP_MILLIS, RESUME_MILLIS, RESUME_REDUCTION_SECONDS);

        Assertions.assertEquals(WaitCountdown.CLOSED, countdown.getCount());
        Assertions.assertFalse(countdown.isOpen());
        Assertions.assertTrue(countdown.advance(100L));
        Assertions.assertEquals(SECONDS, countdown.getCount());

        // Exactly one step later nothing changes; one millisecond more counts a second down.
        Assertions.assertFalse(countdown.advance(100L + STEP_MILLIS));
        Assertions.assertEquals(SECONDS, countdown.getCount());
        Assertions.assertFalse(countdown.advance(101L + STEP_MILLIS));
        Assertions.assertEquals(SECONDS - 1, countdown.getCount());

        long now = 101L + STEP_MILLIS;

        for (int step = 0; step < SECONDS + 2; step++) {
            now += STEP_MILLIS + 1L;
            countdown.advance(now);
        }

        Assertions.assertEquals(0, countdown.getCount());
        Assertions.assertTrue(countdown.isExpired());
        Assertions.assertTrue(countdown.close(now));
        Assertions.assertFalse(countdown.close(now));
        Assertions.assertEquals(WaitCountdown.CLOSED, countdown.getCount());
    }

    @Test
    void advance_should_continue_below_the_last_count_within_the_resume_time() {
        WaitCountdown countdown = new WaitCountdown(SECONDS, STEP_MILLIS, RESUME_MILLIS, RESUME_REDUCTION_SECONDS);

        countdown.advance(0L);
        countdown.advance(STEP_MILLIS + 1L);
        countdown.close(STEP_MILLIS + 1L);

        // The last count was one below the full count; the reopened dialog starts two seconds below it.
        Assertions.assertTrue(countdown.advance(STEP_MILLIS + 1L + RESUME_MILLIS));
        Assertions.assertEquals(SECONDS - 1 - RESUME_REDUCTION_SECONDS, countdown.getCount());

        countdown.close(STEP_MILLIS + 1L + RESUME_MILLIS);

        // A dialog closed longer ago than the resume time starts over.
        Assertions.assertTrue(countdown.advance(STEP_MILLIS + 2L + 2L * RESUME_MILLIS));
        Assertions.assertEquals(SECONDS, countdown.getCount());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new WaitCountdown(-1, STEP_MILLIS, RESUME_MILLIS, RESUME_REDUCTION_SECONDS));
    }
}
