package org.fogofwarlabs.sc4j.engine.client.game;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests the simulation watchdog: a frame that runs past the stall time runs the stall listener once on the watchdog's thread, frames that end in
 * time run nothing, and invalid times are refused.
 */
final class SimulationWatchdogTest {
    private static final long STALL_MILLISECONDS = 50;
    private static final long CHECK_MILLISECONDS = 10;
    /**
     * The longest wait for the listener; far above the stall time so a busy machine does not fail the test.
     */
    private static final long LISTENER_TIMEOUT_SECONDS = 10;
    /**
     * The time a stalled frame stays open after its report, several check periods.
     */
    private static final long AFTER_REPORT_MILLISECONDS = 200;
    private static final long STALLED_FRAME = 42;
    /**
     * A stall time no test frame reaches.
     */
    private static final long LONG_STALL_MILLISECONDS = 10_000;

    @Test
    void a_stalled_frame_runs_the_listener_once_on_the_watchdog_thread() throws InterruptedException {
        CountDownLatch reported = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        String[] threadName = new String[1];

        try (SimulationWatchdog watchdog = new SimulationWatchdog(STALL_MILLISECONDS, CHECK_MILLISECONDS)) {
            watchdog.setStallListener(() -> {
                threadName[0] = Thread.currentThread().getName();
                calls.incrementAndGet();
                reported.countDown();
            });
            watchdog.begin(STALLED_FRAME);

            Assertions.assertTrue(reported.await(LISTENER_TIMEOUT_SECONDS, TimeUnit.SECONDS));

            Thread.sleep(AFTER_REPORT_MILLISECONDS);
            watchdog.end();

            Assertions.assertEquals(1, calls.get(), "one report per stalled frame");
            Assertions.assertNotEquals(Thread.currentThread().getName(), threadName[0]);
        }
    }

    @Test
    void frames_that_end_in_time_run_nothing() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger();

        try (SimulationWatchdog watchdog = new SimulationWatchdog(LONG_STALL_MILLISECONDS, CHECK_MILLISECONDS)) {
            watchdog.setStallListener(calls::incrementAndGet);

            for (int frame = 0; frame < STALLED_FRAME; frame++) {
                watchdog.begin(frame);
                watchdog.end();
            }

            Thread.sleep(AFTER_REPORT_MILLISECONDS);
        }

        Assertions.assertEquals(0, calls.get());
    }

    @Test
    void constructor_should_refuse_times_that_are_not_positive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SimulationWatchdog(0, CHECK_MILLISECONDS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SimulationWatchdog(STALL_MILLISECONDS, -1));
    }

    @Test
    void a_closed_watchdog_starts_no_thread() {
        SimulationWatchdog watchdog = new SimulationWatchdog();

        watchdog.close();
        watchdog.begin(0);
        watchdog.end();
        watchdog.close();
    }
}
