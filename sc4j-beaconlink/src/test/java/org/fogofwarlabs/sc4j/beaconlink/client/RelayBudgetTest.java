package org.fogofwarlabs.sc4j.beaconlink.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the relay budget of a relayed pair: a full burst passes, the next datagram is dropped, and the bucket refills with the protocol
 * clock up to its burst.
 */
final class RelayBudgetTest {
    private static final long START_MILLIS = 1_000L;
    private static final long REFILL_MILLIS = 10L;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long FULL_MILLIS = 10L * MILLIS_PER_SECOND;

    @Test
    void take_should_pass_a_burst_then_drop_until_the_bucket_refills() {
        RelayBudget budget = new RelayBudget(START_MILLIS);

        for (int datagram = 0; datagram < RelayBudget.BURST_DATAGRAMS; datagram++) {
            Assertions.assertTrue(budget.take(START_MILLIS), "datagram " + datagram + " of the burst");
        }

        Assertions.assertFalse(budget.take(START_MILLIS), "the datagram after the burst");

        long later = START_MILLIS + REFILL_MILLIS;
        int refilled = (int) (REFILL_MILLIS * RelayBudget.DATAGRAMS_PER_SECOND / MILLIS_PER_SECOND);

        for (int datagram = 0; datagram < refilled; datagram++) {
            Assertions.assertTrue(budget.take(later), "refilled datagram " + datagram);
        }

        Assertions.assertFalse(budget.take(later), "the datagram after the refill");

        long full = later + FULL_MILLIS;
        int passed = 0;

        while (budget.take(full)) {
            passed++;
        }

        Assertions.assertEquals(RelayBudget.BURST_DATAGRAMS, passed, "the bucket refills to its burst only");
    }
}
