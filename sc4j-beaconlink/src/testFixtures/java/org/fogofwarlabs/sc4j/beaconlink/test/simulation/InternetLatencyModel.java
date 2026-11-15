package org.fogofwarlabs.sc4j.beaconlink.test.simulation;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Gives the hosts of a {@link SimulatedNetwork} latencies with the shape of measured Internet round trip time matrices (as the King,
 * Meridian and PlanetLab data sets show it), synthesized from the run's seed since a test reads nothing from outside the repository:
 * <ul>
 *     <li>hosts cluster on six continents by a fixed share of users, spread around each continent's center;</li>
 *     <li>the propagation follows the great-circle distance at the speed of light in fiber ({@value #FIBER_KILOMETERS_PER_MILLI} km per
 *         millisecond), inflated by the route of each pair ({@value #MINIMUM_ROUTE_INFLATION} to
 *         {@value #MAXIMUM_ROUTE_INFLATION} times);</li>
 *     <li>{@value #DETOUR_SHARE} of the pairs take a detour of {@value #MINIMUM_DETOUR} to {@value #MAXIMUM_DETOUR} times on top, which
 *         breaks the triangle inequality as real routing does;</li>
 *     <li>every host has an access link with a log-normal one-way delay (median {@value #ACCESS_MEDIAN_MILLIS} ms, at most
 *         {@value #MAXIMUM_ACCESS_MILLIS} ms), which every path of the host crosses;</li>
 *     <li>every datagram gets an exponential queueing jitter of {@value #JITTER_SHARE} of its latency on average.</li>
 * </ul>
 * The round trip time of a pair ({@link #getRoundTripMillis}) is the one without jitter, the value a measured matrix holds.
 */
public final class InternetLatencyModel implements SimulatedNetwork.LatencyModel {
    /**
     * The one-way distance light travels in fiber in a millisecond, in kilometers.
     */
    public static final double FIBER_KILOMETERS_PER_MILLI = 200.0;
    /**
     * The smallest route inflation of a pair.
     */
    public static final double MINIMUM_ROUTE_INFLATION = 1.2;
    /**
     * The largest route inflation of a pair.
     */
    public static final double MAXIMUM_ROUTE_INFLATION = 1.8;
    /**
     * The share of pairs that take a detour.
     */
    public static final double DETOUR_SHARE = 0.05;
    /**
     * The smallest detour factor.
     */
    public static final double MINIMUM_DETOUR = 1.5;
    /**
     * The largest detour factor.
     */
    public static final double MAXIMUM_DETOUR = 2.5;
    /**
     * The median one-way delay of an access link, in milliseconds.
     */
    public static final double ACCESS_MEDIAN_MILLIS = 4.0;
    /**
     * The largest one-way delay of an access link, in milliseconds.
     */
    public static final double MAXIMUM_ACCESS_MILLIS = 40.0;
    /**
     * The mean queueing jitter of a datagram, as a share of its latency.
     */
    public static final double JITTER_SHARE = 0.03;

    private static final double ACCESS_SPREAD = 0.7;
    private static final double SPREAD_KILOMETERS = 700.0;
    private static final double KILOMETERS_PER_DEGREE = 111.2;
    private static final double EARTH_RADIUS_KILOMETERS = 6371.0;
    private static final double MAXIMUM_LATITUDE = 89.0;
    private static final double HALF = 0.5;
    private static final int PAIR_SHIFT = 32;

    /**
     * Lists the continents with their centers and their shares of the hosts.
     */
    private enum Continent {
        NORTH_AMERICA_EAST(40.0, -77.0, 0.25),
        NORTH_AMERICA_WEST(37.0, -122.0, 0.15),
        EUROPE(50.0, 8.0, 0.30),
        EAST_ASIA(35.0, 135.0, 0.15),
        SOUTH_AMERICA(-23.0, -46.0, 0.08),
        OCEANIA(-33.0, 151.0, 0.07);

        private final double latitude;
        private final double longitude;
        private final double share;

        Continent(double latitude, double longitude, double share) {
            this.latitude  = latitude;
            this.longitude = longitude;
            this.share     = share;
        }
    }

    /**
     * Holds one host: its place on the globe and the one-way delay of its access link.
     */
    private static final class Place {
        private final double latitude;
        private final double longitude;
        private final double accessMillis;

        private Place(double latitude, double longitude, double accessMillis) {
            this.latitude     = latitude;
            this.longitude    = longitude;
            this.accessMillis = accessMillis;
        }
    }


    private final @NotNull SplittableRandom pairRandom;
    private final @NotNull List<@NotNull Place> places;
    private final @NotNull Map<@NotNull Long, @NotNull Double> routeFactors;

    /**
     * Creates an {@link InternetLatencyModel} instance without hosts.
     *
     * @param random the source of the route factors of the pairs, split from the run's random source
     */
    public InternetLatencyModel(@NotNull SplittableRandom random) {
        this.pairRandom   = random.split();
        this.places       = new ArrayList<>();
        this.routeFactors = new HashMap<>();
    }

    /**
     * Places a new host: a continent by the shares, a place around its center and an access link.
     *
     * @param random the source of the place
     * @return the host's place number
     */
    @Override
    public int placeHost(@NotNull SplittableRandom random) {
        double pick = random.nextDouble();
        Continent continent = Continent.OCEANIA;

        for (Continent candidate : Continent.values()) {
            if (pick < candidate.share) {
                continent = candidate;

                break;
            }

            pick -= candidate.share;
        }

        double latitude = Math.clamp(continent.latitude + random.nextGaussian() * SPREAD_KILOMETERS / KILOMETERS_PER_DEGREE, -MAXIMUM_LATITUDE,
                                     MAXIMUM_LATITUDE);
        double longitude = continent.longitude + random.nextGaussian() * SPREAD_KILOMETERS / KILOMETERS_PER_DEGREE;
        double accessMillis = Math.min(MAXIMUM_ACCESS_MILLIS, ACCESS_MEDIAN_MILLIS * Math.exp(random.nextGaussian() * ACCESS_SPREAD));

        this.places.add(new Place(latitude, longitude, accessMillis));

        return this.places.size() - 1;
    }

    /**
     * Gives the one-way latency of one datagram from a host to another: half the pair's round trip time and the datagram's jitter.
     *
     * @param source the sender's place number
     * @param target the receiver's place number
     * @param random the source of the jitter
     * @return the latency, in whole milliseconds, at least one
     */
    @Override
    public long getOneWayMillis(int source, int target, @NotNull SplittableRandom random) {
        double oneWay = this.getRoundTripMillis(source, target) * HALF;
        double jitter = -Math.log(1.0 - random.nextDouble()) * JITTER_SHARE;

        return Math.max(1L, Math.round(oneWay * (1.0 + jitter)));
    }

    /**
     * Gives the round trip time between two hosts without jitter: twice the inflated propagation and both access links.
     *
     * @param first  the first host's place number
     * @param second the second host's place number
     * @return the round trip time, in milliseconds
     */
    @Override
    public double getRoundTripMillis(int first, int second) {
        Place from = this.places.get(first);
        Place to = this.places.get(second);
        double propagation = this.findDistanceKilometers(from, to) / FIBER_KILOMETERS_PER_MILLI * this.findRouteFactor(first, second);

        return 2.0 * (propagation + from.accessMillis + to.accessMillis);
    }

    private double findDistanceKilometers(@NotNull Place from, @NotNull Place to) {
        double latitudeFrom = Math.toRadians(from.latitude);
        double latitudeTo = Math.toRadians(to.latitude);
        double latitudeHalf = (latitudeTo - latitudeFrom) * HALF;
        double longitudeHalf = Math.toRadians(to.longitude - from.longitude) * HALF;
        double haversine = Math.sin(latitudeHalf) * Math.sin(latitudeHalf) +
                           Math.cos(latitudeFrom) * Math.cos(latitudeTo) * Math.sin(longitudeHalf) * Math.sin(longitudeHalf);

        return 2.0 * EARTH_RADIUS_KILOMETERS * Math.asin(Math.min(1.0, Math.sqrt(haversine)));
    }

    private double findRouteFactor(int first, int second) {
        long pair = (long) Math.min(first, second) << PAIR_SHIFT | Math.max(first, second);

        return this.routeFactors.computeIfAbsent(pair, ignored -> {
            double factor = MINIMUM_ROUTE_INFLATION + this.pairRandom.nextDouble() * (MAXIMUM_ROUTE_INFLATION - MINIMUM_ROUTE_INFLATION);

            if (this.pairRandom.nextDouble() < DETOUR_SHARE) {
                factor *= MINIMUM_DETOUR + this.pairRandom.nextDouble() * (MAXIMUM_DETOUR - MINIMUM_DETOUR);
            }

            return factor;
        });
    }
}
