package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;

import java.util.concurrent.locks.LockSupport;

/**
 * Paces presented frames to a frame rate limit with a running deadline: each frame's deadline is one period after the
 * previous one, so short frames do not drift; a frame that is late by more than a period starts a new deadline at once
 * instead of catching up. The wait parks the thread while the deadline is far and spins for the last part.
 * <p>
 * Only presentation is paced; a client's simulation advances on its own clock.
 */
public final class FramePacer {
    /**
     * The nanoseconds of a second.
     */
    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;
    /**
     * The remaining wait above which the thread parks instead of spinning.
     */
    private static final int PARK_THRESHOLD_NANOSECONDS = 1_500_000;
    /**
     * The part of a parked wait left to the final spin, so a late wake-up does not miss the deadline.
     */
    private static final int SPIN_ALLOWANCE_NANOSECONDS = 1_000_000;

    private long nextDeadline;

    /**
     * Creates a {@link FramePacer} instance without a deadline: the first paced frame starts one.
     */
    public FramePacer() {
        this.nextDeadline = 0;
    }

    /**
     * Waits until the next frame is due under a frame rate limit.
     *
     * @param framesPerSecond the frame rate limit
     * @throws IllegalArgumentException if the limit is not positive
     */
    public void waitForNextFrame(int framesPerSecond) {
        if (framesPerSecond <= 0) {
            throw Exceptions.newIllegalArgumentException("The frame rate limit must be positive: {0}", framesPerSecond);
        }

        long period = NANOSECONDS_PER_SECOND / framesPerSecond;
        long now = System.nanoTime();

        if (this.nextDeadline == 0 || now - this.nextDeadline > period) {
            this.nextDeadline = now;
        }

        this.nextDeadline += period;

        long remaining = this.nextDeadline - now;

        if (remaining > PARK_THRESHOLD_NANOSECONDS) {
            LockSupport.parkNanos(remaining - SPIN_ALLOWANCE_NANOSECONDS);
        }

        while (System.nanoTime() < this.nextDeadline) {
            Thread.onSpinWait();
        }
    }
}
