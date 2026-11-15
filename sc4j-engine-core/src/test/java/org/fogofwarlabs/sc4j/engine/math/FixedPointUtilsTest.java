package org.fogofwarlabs.sc4j.engine.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the fixed-point operations and their rounding.
 */
final class FixedPointUtilsTest {
    private static final int BITS = FixedPointUtils.DEFAULT_FRACTION_BITS;

    @Test
    void conversions_round_toward_negative_infinity() {
        Assertions.assertEquals(768, FixedPointUtils.fromInteger(3, BITS));
        Assertions.assertEquals(3, FixedPointUtils.toInteger(1000, BITS));
        Assertions.assertEquals(-4, FixedPointUtils.toInteger(-1000, BITS));
        Assertions.assertEquals(1000 - 768, FixedPointUtils.getFraction(1000, BITS));
    }

    @Test
    void multiply_and_divide_keep_the_format() {
        int oneAndHalf = 384;
        int two = 512;

        Assertions.assertEquals(768, FixedPointUtils.multiply(oneAndHalf, two, BITS));
        Assertions.assertEquals(-768, FixedPointUtils.multiply(-oneAndHalf, two, BITS));
        Assertions.assertEquals(oneAndHalf, FixedPointUtils.divide(768, two, BITS));
        Assertions.assertEquals(-oneAndHalf, FixedPointUtils.divide(-768, two, BITS));
        Assertions.assertEquals(85, FixedPointUtils.divide(256, 768, BITS));
        Assertions.assertThrows(ArithmeticException.class, () -> FixedPointUtils.divide(1, 0, BITS));
    }
}
