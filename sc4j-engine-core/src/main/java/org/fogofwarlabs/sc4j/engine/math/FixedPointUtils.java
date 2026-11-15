package org.fogofwarlabs.sc4j.engine.math;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;

/**
 * Computes with fixed-point numbers stored in an {@code int}: the low {@code fractionBits} bits hold the fraction.
 * <p>
 * Each caller chooses its format by the number of fraction bits ({@code 0} up to {@code 30}); a format is a property of
 * the value, so the operations take it as an argument instead of storing it. The operations do not check the format,
 * because they run on the hot path. Conversions to integers round toward negative infinity (arithmetic shift);
 * products are computed in {@code long} and then truncated to {@code int} with {@code 32}-bit overflow; quotients round
 * toward zero.
 */
public final class FixedPointUtils {
    /**
     * The common format of positions and speeds: {@code 8} fraction bits, so one unit is {@code 256}.
     */
    public static final int DEFAULT_FRACTION_BITS = 8;

    /**
     * Converts an integer to fixed point.
     *
     * @param value        the integer
     * @param fractionBits the number of fraction bits of the result
     * @return the fixed-point value, with {@code 32}-bit overflow
     */
    public static int fromInteger(int value, int fractionBits) {
        return value << fractionBits;
    }

    /**
     * Converts a fixed-point value to an integer, rounding toward negative infinity.
     *
     * @param value        the fixed-point value
     * @param fractionBits the number of fraction bits of the value
     * @return the integer part
     */
    public static int toInteger(int value, int fractionBits) {
        return value >> fractionBits;
    }

    /**
     * Returns the fraction bits of a fixed-point value.
     *
     * @param value        the fixed-point value
     * @param fractionBits the number of fraction bits of the value
     * @return the fraction, {@code 0} up to {@code (1 << fractionBits) - 1}
     */
    public static int getFraction(int value, int fractionBits) {
        return value & ((1 << fractionBits) - 1);
    }

    /**
     * Multiplies two fixed-point values of the same format.
     *
     * @param first        the first value
     * @param second       the second value
     * @param fractionBits the number of fraction bits of both values and the result
     * @return the product, rounded toward negative infinity, with {@code 32}-bit overflow
     */
    public static int multiply(int first, int second, int fractionBits) {
        return (int) (((long) first * second) >> fractionBits);
    }

    /**
     * Divides two fixed-point values of the same format.
     *
     * @param dividend     the dividend
     * @param divisor      the divisor
     * @param fractionBits the number of fraction bits of both values and the result
     * @return the quotient, rounded toward zero, with {@code 32}-bit overflow
     * @throws ArithmeticException if the divisor is {@code 0}
     */
    public static int divide(int dividend, int divisor, int fractionBits) {
        if (divisor == 0) {
            throw Exceptions.newArithmeticException("Fixed-point division by zero: {0}", dividend);
        }

        return (int) (((long) dividend << fractionBits) / divisor);
    }

    private FixedPointUtils() {
    }
}
