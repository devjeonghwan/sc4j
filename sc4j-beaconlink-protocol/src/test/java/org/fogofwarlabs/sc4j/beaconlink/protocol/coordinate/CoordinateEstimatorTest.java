package org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Tests the Vivaldi estimator: hosts that sample each other in a space of points and access links find coordinates whose estimates have a
 * small median relative error, a remote host gives one sample per sample interval, a coordinate settles only after samples of enough remote
 * hosts with a low error, samples out of range are ignored, and a settled coordinate rejects the samples of lying or delaying remotes.
 */
final class CoordinateEstimatorTest {
    private static final long RANDOM_SEED = 20261110L;
    private static final int HOST_COUNT = 64;
    private static final int ROUNDS = 100;
    private static final int SAMPLES_PER_ROUND = 4;
    private static final double SPACE_MILLIS = 200.0;
    private static final double MINIMUM_ACCESS_MILLIS = 1.0;
    private static final double ACCESS_RANGE_MILLIS = 20.0;
    private static final double REQUIRED_MEDIAN_ERROR = 0.1;
    private static final double MEDIAN = 0.5;
    private static final long ROUND_TRIP_MILLIS = 80L;
    private static final double SETTLED_REMOTE_ERROR = 0.1;
    private static final int SETTLING_ROUNDS = 10;
    private static final double REQUIRED_ESTIMATE_ERROR = 0.05;
    private static final int LIAR_COUNT = 20;
    private static final long DELAYED_ROUND_TRIP_MILLIS = 4_000L;

    private static double findRoundTripMillis(double @NotNull [] first, double @NotNull [] second) {
        double x = first[0] - second[0];
        double y = first[1] - second[1];

        return Math.sqrt(x * x + y * y) + first[2] + second[2];
    }

    private static @NotNull IdentityKey createKey(@NotNull SplittableRandom random) {
        byte[] bytes = new byte[IdentityKey.LENGTH];

        random.nextBytes(bytes);

        return IdentityKey.of(bytes);
    }

    /**
     * Places {@value #HOST_COUNT} hosts in a plane with access links and lets them sample each other for {@value #ROUNDS} sample intervals.
     *
     * @return the time after the last round
     */
    private static long settle(@NotNull SplittableRandom random, @NotNull List<double @NotNull []> places, @NotNull List<@NotNull IdentityKey> keys,
                               @NotNull List<@NotNull CoordinateEstimator> estimators) {
        for (int index = 0; index < HOST_COUNT; index++) {
            places.add(new double[]{random.nextDouble() * SPACE_MILLIS, random.nextDouble() * SPACE_MILLIS,
                                    MINIMUM_ACCESS_MILLIS + random.nextDouble() * ACCESS_RANGE_MILLIS});
            keys.add(createKey(random));
            estimators.add(new CoordinateEstimator(random.split()));
        }

        for (int round = 0; round < ROUNDS; round++) {
            long nowMillis = round * CoordinateEstimator.SAMPLE_INTERVAL_MILLIS;

            for (int index = 0; index < HOST_COUNT; index++) {
                for (int sample = 0; sample < SAMPLES_PER_ROUND; sample++) {
                    int other = random.nextInt(HOST_COUNT - 1);

                    other += other >= index ? 1 : 0;

                    long measured = Math.round(findRoundTripMillis(places.get(index), places.get(other)));

                    estimators.get(index).update(keys.get(other), estimators.get(other).getReport(), measured, nowMillis);
                }
            }
        }

        return ROUNDS * CoordinateEstimator.SAMPLE_INTERVAL_MILLIS;
    }

    /**
     * Finds the median relative error of the estimates between one host and every other host.
     */
    private static double findMedianError(@NotNull List<double @NotNull []> places, @NotNull List<@NotNull CoordinateEstimator> estimators,
                                          int host) {
        List<Double> errors = new ArrayList<>();

        for (int other = 0; other < HOST_COUNT; other++) {
            if (other != host) {
                double measured = findRoundTripMillis(places.get(host), places.get(other));
                double estimated = estimators.get(host).getCoordinate().estimateRoundTripMillis(estimators.get(other).getCoordinate());

                errors.add(Math.abs(estimated - measured) / measured);
            }
        }

        Collections.sort(errors);

        return errors.get((int) (errors.size() * MEDIAN));
    }

    @Test
    void update_should_find_coordinates_with_a_small_median_error_in_a_space_with_access_links() {
        SplittableRandom random = new SplittableRandom(RANDOM_SEED);
        List<double[]> places = new ArrayList<>();
        List<IdentityKey> keys = new ArrayList<>();
        List<CoordinateEstimator> estimators = new ArrayList<>();

        settle(random, places, keys, estimators);

        List<Double> errors = new ArrayList<>();

        for (int first = 0; first < HOST_COUNT; first++) {
            for (int second = first + 1; second < HOST_COUNT; second++) {
                double measured = findRoundTripMillis(places.get(first), places.get(second));
                double estimated = estimators.get(first).getCoordinate().estimateRoundTripMillis(estimators.get(second).getCoordinate());

                errors.add(Math.abs(estimated - measured) / measured);
            }
        }

        Collections.sort(errors);

        double median = errors.get((int) (errors.size() * MEDIAN));

        Assertions.assertTrue(median <= REQUIRED_MEDIAN_ERROR, "median relative error " + median);
        Assertions.assertTrue(estimators.stream().allMatch(CoordinateEstimator::isSettled), "every coordinate settles");
    }

    @Test
    void update_should_reject_the_samples_of_lying_and_delaying_remotes_while_settled() {
        SplittableRandom random = new SplittableRandom(RANDOM_SEED);
        List<double[]> places = new ArrayList<>();
        List<IdentityKey> keys = new ArrayList<>();
        List<CoordinateEstimator> estimators = new ArrayList<>();
        long nowMillis = settle(random, places, keys, estimators);
        CoordinateEstimator victim = estimators.getFirst();
        double[] corner = {NetworkCoordinate.MAXIMUM_MILLIS, NetworkCoordinate.MAXIMUM_MILLIS, NetworkCoordinate.MAXIMUM_MILLIS};
        CoordinateReport far = new CoordinateReport(new NetworkCoordinate(corner, 0.0), 0.0);
        CoordinateReport near = new CoordinateReport(victim.getCoordinate(), 0.0);
        double medianBefore = findMedianError(places, estimators, 0);
        int outliers = 0;

        Assertions.assertTrue(victim.isSettled());

        // Liars with full confidence, each once, between the victim's honest samples: half claim to sit at the far corner and answer at
        // once, half claim the victim's own place and answer late.
        for (int round = 0; round < LIAR_COUNT; round++) {
            boolean farLiar = round % 2 == 0;
            int honest = 1 + round % (HOST_COUNT - 1);
            long honestMillis = Math.round(findRoundTripMillis(places.getFirst(), places.get(honest)));

            if (victim.update(createKey(random), farLiar ? far : near, farLiar ? ROUND_TRIP_MILLIS : DELAYED_ROUND_TRIP_MILLIS, nowMillis) ==
                CoordinateSample.OUTLIER) {
                outliers++;
            }

            victim.update(keys.get(honest), estimators.get(honest).getReport(), honestMillis, nowMillis);
            nowMillis += CoordinateEstimator.SAMPLE_INTERVAL_MILLIS;
        }

        double median = findMedianError(places, estimators, 0);

        Assertions.assertEquals(LIAR_COUNT, outliers, "every lie is an outlier");
        Assertions.assertTrue(median <= Math.max(REQUIRED_MEDIAN_ERROR, medianBefore * 2), "median relative error of the victim " + median);
        Assertions.assertTrue(victim.isSettled(), victim.toString());
    }

    @Test
    void update_should_take_one_sample_per_remote_and_interval_and_settle_after_enough_remotes() {
        SplittableRandom random = new SplittableRandom(RANDOM_SEED);
        CoordinateEstimator estimator = new CoordinateEstimator(random.split());
        CoordinateReport remote = new CoordinateReport(new NetworkCoordinate(new double[]{30.0, 0.0, 0.0}, 2.0), SETTLED_REMOTE_ERROR);
        IdentityKey remoteKey = createKey(random);

        Assertions.assertEquals(CoordinateSample.SKIPPED, estimator.update(remoteKey, remote, -1L, 0L));
        Assertions.assertEquals(CoordinateSample.SKIPPED, estimator.update(remoteKey, remote, CoordinateEstimator.MAXIMUM_ROUND_TRIP_MILLIS + 1L, 0L));
        Assertions.assertEquals(0, estimator.getSampleCount());
        Assertions.assertEquals(CoordinateReport.MAXIMUM_ERROR, estimator.getError());
        Assertions.assertNull(estimator.getSettledCoordinate());
        Assertions.assertEquals(-1L, estimator.estimateRoundTripMillis(remote.getCoordinate()));

        for (int round = 0; round < SETTLING_ROUNDS; round++) {
            long nowMillis = round * CoordinateEstimator.SAMPLE_INTERVAL_MILLIS;

            Assertions.assertEquals(CoordinateSample.TAKEN, estimator.update(remoteKey, remote, ROUND_TRIP_MILLIS, nowMillis));
            Assertions.assertEquals(CoordinateSample.SKIPPED, estimator.update(remoteKey, remote, ROUND_TRIP_MILLIS, nowMillis + 1L),
                                    "one sample per interval");
        }

        Assertions.assertTrue(estimator.getError() <= CoordinateEstimator.SETTLED_ERROR, estimator.toString());
        Assertions.assertFalse(estimator.isSettled(), "one remote host does not settle a coordinate");

        for (int index = 1; index < CoordinateEstimator.SETTLED_REMOTES; index++) {
            estimator.update(createKey(random), remote, ROUND_TRIP_MILLIS, SETTLING_ROUNDS * CoordinateEstimator.SAMPLE_INTERVAL_MILLIS);
        }

        long estimated = estimator.estimateRoundTripMillis(remote.getCoordinate());

        Assertions.assertTrue(estimator.isSettled(), estimator.toString());
        Assertions.assertNotNull(estimator.getSettledCoordinate());
        Assertions.assertTrue(Math.abs(estimated - ROUND_TRIP_MILLIS) <= ROUND_TRIP_MILLIS * REQUIRED_ESTIMATE_ERROR, "estimate " + estimated);
    }
}
