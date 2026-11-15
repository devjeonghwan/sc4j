package org.fogofwarlabs.sc4j.beaconlink.protocol.test;

import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;

/**
 * Supplies a protocol time that only the test advances, so that expiry, replay windows and gossip rounds need no real sleep.
 */
public final class ManualProtocolClock implements ProtocolClock {
    /**
     * The epoch millisecond a new clock starts at (a fixed day in 2027).
     */
    public static final long START_MILLIS = 1_800_000_000_000L;

    private long epochMillis;

    /**
     * Creates a {@link ManualProtocolClock} instance at {@link #START_MILLIS}.
     */
    public ManualProtocolClock() {
        this.epochMillis = START_MILLIS;
    }

    /**
     * @return the current time in milliseconds since the epoch
     */
    @Override
    public long getEpochMillis() {
        return this.epochMillis;
    }


    /**
     * Advances the time.
     *
     * @param millis the milliseconds to add
     */
    public void advance(long millis) {
        this.epochMillis += millis;
    }
}
