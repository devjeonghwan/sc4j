package org.fogofwarlabs.sc4j.engine.net;

/**
 * Supplies a network time that only the test advances, so that resends and timeouts need no real sleep.
 */
public final class ManualNetworkClock implements NetworkClock {
    private long millis;

    /**
     * Creates a {@link ManualNetworkClock} instance at time {@code 0}.
     */
    public ManualNetworkClock() {
        this.millis = 0L;
    }

    /**
     * @return the current time in milliseconds from an arbitrary origin
     */
    @Override
    public long getMillis() {
        return this.millis;
    }


    /**
     * Advances the time.
     *
     * @param millis the milliseconds to add
     */
    public void advance(long millis) {
        this.millis += millis;
    }
}
