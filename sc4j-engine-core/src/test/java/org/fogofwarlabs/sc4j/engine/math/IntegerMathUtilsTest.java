package org.fogofwarlabs.sc4j.engine.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the integer square root and distance.
 */
final class IntegerMathUtilsTest {
    @Test
    void square_root_should_return_the_largest_root_not_above_the_value() {
        Assertions.assertEquals(0L, IntegerMathUtils.squareRoot(0L));
        Assertions.assertEquals(1L, IntegerMathUtils.squareRoot(3L));
        Assertions.assertEquals(2L, IntegerMathUtils.squareRoot(4L));
        Assertions.assertEquals(99L, IntegerMathUtils.squareRoot(9999L));
        Assertions.assertEquals(3037000499L, IntegerMathUtils.squareRoot(Long.MAX_VALUE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> IntegerMathUtils.squareRoot(-1L));

        for (long value = 0L; value < 5000L; value++) {
            long root = IntegerMathUtils.squareRoot(value);

            Assertions.assertTrue(root * root <= value && (root + 1L) * (root + 1L) > value, "value " + value);
        }
    }

    @Test
    void get_distance_should_round_toward_zero() {
        Assertions.assertEquals(5, IntegerMathUtils.getDistance(1, 1, 4, 5));
        Assertions.assertEquals(1, IntegerMathUtils.getDistance(0, 0, -1, -1));
        Assertions.assertEquals(Integer.MAX_VALUE, IntegerMathUtils.getDistance(0, 0, Integer.MAX_VALUE, 0));
    }
}
