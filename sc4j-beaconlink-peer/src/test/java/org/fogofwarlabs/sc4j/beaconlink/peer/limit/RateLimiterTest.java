package org.fogofwarlabs.sc4j.beaconlink.peer.limit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the token-bucket rate limiter: a key has its burst, then its refill rate (per second or per longer period), and a full table removes
 * only refilled buckets.
 */
final class RateLimiterTest {
    private static final long NOW_MILLIS = 1_000_000L;

    @Test
    void try_acquire_should_allow_the_burst_then_the_refill_rate() {
        RateLimiter<String> limiter = new RateLimiter<>(8, 3, 2);

        for (int count = 0; count < 3; count++) {
            Assertions.assertTrue(limiter.tryAcquire("a", NOW_MILLIS));
        }

        Assertions.assertFalse(limiter.tryAcquire("a", NOW_MILLIS));
        Assertions.assertTrue(limiter.tryAcquire("b", NOW_MILLIS));
        Assertions.assertFalse(limiter.tryAcquire("a", NOW_MILLIS + 499L));
        Assertions.assertTrue(limiter.tryAcquire("a", NOW_MILLIS + 500L));
        Assertions.assertFalse(limiter.tryAcquire("a", NOW_MILLIS + 500L));
        Assertions.assertTrue(limiter.tryAcquire("a", NOW_MILLIS + 1_000_000L));
    }

    @Test
    void try_acquire_should_refill_one_token_per_period_below_one_per_second() {
        RateLimiter<String> limiter = new RateLimiter<>(8, 2, 1, 10_000L);

        Assertions.assertTrue(limiter.tryAcquire("a", NOW_MILLIS));
        Assertions.assertTrue(limiter.tryAcquire("a", NOW_MILLIS));
        Assertions.assertFalse(limiter.tryAcquire("a", NOW_MILLIS + 9_999L));
        Assertions.assertTrue(limiter.tryAcquire("a", NOW_MILLIS + 10_000L));
        Assertions.assertFalse(limiter.tryAcquire("a", NOW_MILLIS + 10_000L));
    }

    @Test
    void try_acquire_should_refuse_new_keys_while_the_table_is_full_of_active_buckets() {
        RateLimiter<String> limiter = new RateLimiter<>(2, 2, 1);

        Assertions.assertTrue(limiter.tryAcquire("a", NOW_MILLIS));
        Assertions.assertTrue(limiter.tryAcquire("b", NOW_MILLIS));
        Assertions.assertFalse(limiter.tryAcquire("c", NOW_MILLIS));
        Assertions.assertFalse(limiter.tryAcquire("c", NOW_MILLIS + 999L));
        Assertions.assertTrue(limiter.tryAcquire("c", NOW_MILLIS + 1000L));
        Assertions.assertEquals(1, limiter.getSize());
    }
}
