package org.fogofwarlabs.sc4j.beaconlink.test.simulation;

import org.jetbrains.annotations.NotNull;

import java.util.SplittableRandom;

/**
 * Gives the hosts of a {@link SimulatedNetwork} the latencies of a few regions: every host lies in a random region, and every pair of regions
 * has a one-way latency between {@value #MINIMUM_LATENCY_MILLIS} and {@value #MAXIMUM_LATENCY_MILLIS} milliseconds, the same in both
 * directions (inside a region the minimum). The latencies have no jitter.
 */
public final class RegionLatencyModel implements SimulatedNetwork.LatencyModel {
    /**
     * The one-way latency inside a region, and the shortest between two regions, in milliseconds.
     */
    public static final long MINIMUM_LATENCY_MILLIS = 5L;
    /**
     * The longest one-way latency between two regions, in milliseconds.
     */
    public static final long MAXIMUM_LATENCY_MILLIS = 150L;

    private final long @NotNull [] @NotNull [] latencyMillis;

    /**
     * Creates a {@link RegionLatencyModel} instance with a random latency for every pair of regions.
     *
     * @param regionCount the number of regions
     * @param random      the source of the latencies
     */
    public RegionLatencyModel(int regionCount, @NotNull SplittableRandom random) {
        this.latencyMillis = new long[regionCount][regionCount];

        for (int first = 0; first < regionCount; first++) {
            this.latencyMillis[first][first] = MINIMUM_LATENCY_MILLIS;

            for (int second = first + 1; second < regionCount; second++) {
                long latency = MINIMUM_LATENCY_MILLIS + random.nextLong(MAXIMUM_LATENCY_MILLIS - MINIMUM_LATENCY_MILLIS + 1L);

                this.latencyMillis[first][second] = latency;
                this.latencyMillis[second][first] = latency;
            }
        }
    }

    /**
     * Places a new host in a random region.
     *
     * @param random the source of the place
     * @return the host's region
     */
    @Override
    public int placeHost(@NotNull SplittableRandom random) {
        return random.nextInt(this.latencyMillis.length);
    }

    /**
     * Gives the one-way latency of one datagram from a host to another.
     *
     * @param source the sender's region
     * @param target the receiver's region
     * @param random the source of jitter, unused
     * @return the latency of the regions, in milliseconds
     */
    @Override
    public long getOneWayMillis(int source, int target, @NotNull SplittableRandom random) {
        return this.latencyMillis[source][target];
    }

    /**
     * Gives the round trip time between two hosts.
     *
     * @param first  the first host's region
     * @param second the second host's region
     * @return twice the latency of the regions, in milliseconds
     */
    @Override
    public double getRoundTripMillis(int first, int second) {
        return 2.0 * this.latencyMillis[first][second];
    }
}
