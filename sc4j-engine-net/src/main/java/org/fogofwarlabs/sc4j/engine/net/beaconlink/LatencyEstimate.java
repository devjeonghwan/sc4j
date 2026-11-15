package org.fogofwarlabs.sc4j.engine.net.beaconlink;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the estimated round trip time to another participant before any channel to it exists, from the network coordinates of both sides
 * (a room's host, a player of a channel). A room or player list sorts by it (known estimates from the shortest, unknown ones last) and
 * filters by its class ({@link #isWithin}). It is unknown while the network coordinate of either side has not settled; a channel then
 * measures the real delay.
 */
public final class LatencyEstimate implements Comparable<LatencyEstimate> {
    /**
     * The estimate of a participant whose round trip time cannot be estimated yet.
     */
    public static final @NotNull LatencyEstimate UNKNOWN = new LatencyEstimate(-1L);

    /**
     * Creates a {@link LatencyEstimate} instance of a known round trip time.
     *
     * @param roundTripMillis the estimated round trip time, in milliseconds
     * @return the created {@link LatencyEstimate} instance
     * @throws IllegalArgumentException if the time is negative
     */
    public static @NotNull LatencyEstimate of(long roundTripMillis) {
        if (roundTripMillis < 0L) {
            throw Exceptions.newIllegalArgumentException("The estimated round trip time {0} ms is negative.", roundTripMillis);
        }

        return new LatencyEstimate(roundTripMillis);
    }


    private final long roundTripMillis;

    private LatencyEstimate(long roundTripMillis) {
        this.roundTripMillis = roundTripMillis;
    }

    /**
     * @return {@code true} if the round trip time is estimated, {@code false} otherwise
     */
    public boolean isKnown() {
        return this.roundTripMillis >= 0L;
    }

    /**
     * @return the estimated round trip time in milliseconds, or {@code -1} if it is unknown
     */
    public long getRoundTripMillis() {
        return this.roundTripMillis;
    }

    /**
     * @return the class of the estimated round trip time, {@link LatencyClass#UNKNOWN} if it is unknown
     */
    public @NotNull LatencyClass getLatencyClass() {
        return this.isKnown() ? LatencyClass.of(this.roundTripMillis) : LatencyClass.UNKNOWN;
    }


    /**
     * Checks the estimate against a filter limit.
     *
     * @param limit the worst class the filter lets through
     * @return {@code true} if the round trip time is known and its class is the limit or better, {@code false} otherwise
     */
    public boolean isWithin(@NotNull LatencyClass limit) {
        return this.isKnown() && this.roundTripMillis <= limit.getMaximumMillis();
    }

    /**
     * Compares two estimates for a list order: known estimates from the shortest round trip time, unknown ones after them.
     *
     * @param other the other estimate
     * @return a negative number, zero or a positive number if this estimate comes before, with or after the other one
     */
    @Override
    public int compareTo(@NotNull LatencyEstimate other) {
        if (this.isKnown() != other.isKnown()) {
            return this.isKnown() ? -1 : 1;
        }

        return Long.compare(this.roundTripMillis, other.roundTripMillis);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is an estimate of the same round trip time, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof LatencyEstimate estimate && this.roundTripMillis == estimate.roundTripMillis;
    }

    /**
     * @return the hash code of the round trip time
     */
    @Override
    public int hashCode() {
        return Long.hashCode(this.roundTripMillis);
    }

    /**
     * @return the round trip time, or {@code unknown}, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.isKnown() ? "~" + this.roundTripMillis + " ms" : "unknown";
    }
}
