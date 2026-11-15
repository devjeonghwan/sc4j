package org.fogofwarlabs.sc4j.engine.net.beaconlink;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the estimated round trip time: its class, the filter limit and the list order.
 */
final class LatencyEstimateTest {
    @Test
    void is_within_should_let_through_known_estimates_of_the_limit_class_or_better() {
        LatencyEstimate good = LatencyEstimate.of(LatencyClass.GOOD_MILLIS);
        LatencyEstimate fair = LatencyEstimate.of(LatencyClass.GOOD_MILLIS + 1L);

        Assertions.assertEquals(LatencyClass.GOOD, good.getLatencyClass());
        Assertions.assertEquals(LatencyClass.FAIR, fair.getLatencyClass());
        Assertions.assertEquals(LatencyClass.FAIR_MILLIS, LatencyClass.FAIR.getMaximumMillis());
        Assertions.assertTrue(good.isWithin(LatencyClass.GOOD));
        Assertions.assertFalse(fair.isWithin(LatencyClass.GOOD));
        Assertions.assertTrue(fair.isWithin(LatencyClass.POOR));
        Assertions.assertFalse(LatencyEstimate.UNKNOWN.isWithin(LatencyClass.POOR), "an unknown estimate passes no limit");
    }

    @Test
    void compare_to_should_order_known_estimates_from_the_shortest_and_unknown_ones_last() {
        Assertions.assertTrue(LatencyEstimate.of(1L).compareTo(LatencyEstimate.of(2L)) < 0);
        Assertions.assertTrue(LatencyEstimate.UNKNOWN.compareTo(LatencyEstimate.of(Long.MAX_VALUE)) > 0);
    }
}
