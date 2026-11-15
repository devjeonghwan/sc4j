package org.fogofwarlabs.sc4j.engine.client.host;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests that the frame pacer waits for the running deadline of each paced frame.
 */
final class FramePacerTest {
    private static final int FRAMES_PER_SECOND = 100;
    private static final int PACED_FRAMES = 5;
    private static final long PERIOD_NANOSECONDS = 1_000_000_000L / FRAMES_PER_SECOND;

    @Test
    void wait_for_next_frame_should_keep_one_period_per_frame() {
        FramePacer pacer = new FramePacer();
        long started = System.nanoTime();

        for (int frame = 0; frame < PACED_FRAMES; frame++) {
            pacer.waitForNextFrame(FRAMES_PER_SECOND);
        }

        Assertions.assertTrue(System.nanoTime() - started >= PACED_FRAMES * PERIOD_NANOSECONDS);
    }

    @Test
    void wait_for_next_frame_should_reject_a_limit_that_is_not_positive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FramePacer().waitForNextFrame(0));
    }
}
