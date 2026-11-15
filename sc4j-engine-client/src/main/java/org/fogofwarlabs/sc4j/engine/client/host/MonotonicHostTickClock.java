package org.fogofwarlabs.sc4j.engine.client.host;

/**
 * Counts milliseconds of the Java virtual machine's monotonic clock as a wrapping 32-bit tick counter: the default {@link HostTickClock} where
 * the platform offers no tick count of its own.
 */
public final class MonotonicHostTickClock implements HostTickClock {
    private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;

    /**
     * Creates a {@link MonotonicHostTickClock} instance.
     */
    public MonotonicHostTickClock() {
    }

    /**
     * Reads the tick counter.
     *
     * @return the milliseconds counter, wrapping at {@code 2^32}
     */
    @Override
    public int readTicks() {
        return (int) (System.nanoTime() / NANOSECONDS_PER_MILLISECOND);
    }
}
