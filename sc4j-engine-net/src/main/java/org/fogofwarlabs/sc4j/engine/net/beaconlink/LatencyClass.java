package org.fogofwarlabs.sc4j.engine.net.beaconlink;

import org.jetbrains.annotations.NotNull;

/**
 * Lists the classes of an estimated round trip time ({@link LatencyEstimate}), from the best: the bars of a room or player list and the
 * latency filter of a room list.
 */
public enum LatencyClass {
    /**
     * At most {@value #GOOD_MILLIS} milliseconds.
     */
    GOOD(LatencyClass.GOOD_MILLIS),
    /**
     * At most {@value #FAIR_MILLIS} milliseconds.
     */
    FAIR(LatencyClass.FAIR_MILLIS),
    /**
     * More than {@value #FAIR_MILLIS} milliseconds.
     */
    POOR(Long.MAX_VALUE),
    /**
     * No estimate yet: the network coordinate of one side has not settled.
     */
    UNKNOWN(Long.MAX_VALUE);

    /**
     * The longest round trip time of {@link #GOOD}, in milliseconds.
     */
    public static final long GOOD_MILLIS = 100L;
    /**
     * The longest round trip time of {@link #FAIR}, in milliseconds.
     */
    public static final long FAIR_MILLIS = 200L;

    /**
     * Finds the class of a round trip time.
     *
     * @param roundTripMillis the round trip time, in milliseconds, at least {@code 0}
     * @return {@link #GOOD}, {@link #FAIR} or {@link #POOR}
     */
    public static @NotNull LatencyClass of(long roundTripMillis) {
        if (roundTripMillis <= GOOD_MILLIS) {
            return GOOD;
        }

        return roundTripMillis <= FAIR_MILLIS ? FAIR : POOR;
    }


    private final long maximumMillis;

    LatencyClass(long maximumMillis) {
        this.maximumMillis = maximumMillis;
    }

    /**
     * @return the longest round trip time of the class in milliseconds, {@link Long#MAX_VALUE} for {@link #POOR} and {@link #UNKNOWN}
     */
    public long getMaximumMillis() {
        return this.maximumMillis;
    }
}
