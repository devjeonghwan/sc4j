package org.fogofwarlabs.sc4j.engine.math;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;

/**
 * Computes integer results that would otherwise need floating point, such as distances.
 */
public final class IntegerMathUtils {
    /**
     * The shifts of the approximate distance: a smaller coordinate difference of at most a quarter of the larger one gives the larger one;
     * otherwise the result is the larger difference less {@code 1/64} and {@code 1/16} of it plus {@code 1/128} and {@code 1/4} of one and a half
     * times the smaller difference.
     */
    private static final int STRAIGHT_RATIO_SHIFT = 2;
    private static final int MINIMUM_HALF_SHIFT = 1;
    private static final int MINIMUM_FINE_SHIFT = 7;
    private static final int MAXIMUM_FINE_SHIFT = 6;
    private static final int MAXIMUM_COARSE_SHIFT = 4;
    private static final int MINIMUM_COARSE_SHIFT = 2;

    /**
     * Computes the integer square root: the largest integer whose square does not exceed the value.
     *
     * @param value the value, not negative
     * @return the integer square root
     * @throws IllegalArgumentException if the value is negative
     */
    public static long squareRoot(long value) {
        if (value < 0L) {
            throw Exceptions.newIllegalArgumentException("The square root of a negative value: {0}", value);
        }

        long root = 0L;
        long bit = 1L << (Long.SIZE - 2);

        while (bit > value) {
            bit >>>= 2;
        }

        long remainder = value;

        while (bit != 0L) {
            if (remainder >= root + bit) {
                remainder -= root + bit;
                root = (root >>> 1) + bit;
            } else {
                root >>>= 1;
            }

            bit >>>= 2;
        }

        return root;
    }

    /**
     * Computes the integer distance between two points: the integer square root of the squared Euclidean distance. Each
     * coordinate difference must lie within {@code -(2^31 - 1)} and {@code 2^31 - 1}, so the squared distance fits in a
     * {@code long}.
     *
     * @param firstX  the x coordinate of the first point
     * @param firstY  the y coordinate of the first point
     * @param secondX the x coordinate of the second point
     * @param secondY the y coordinate of the second point
     * @return the distance, rounded toward zero
     */
    public static int getDistance(int firstX, int firstY, int secondX, int secondY) {
        long horizontal = (long) secondX - firstX;
        long vertical = (long) secondY - firstY;

        return (int) squareRoot(horizontal * horizontal + vertical * vertical);
    }

    /**
     * Checks whether two points lie within a distance of each other, measured as the Euclidean distance without a square root.
     *
     * @param firstX   the x coordinate of the first point
     * @param firstY   the y coordinate of the first point
     * @param secondX  the x coordinate of the second point
     * @param secondY  the y coordinate of the second point
     * @param distance the distance, at least {@code 0}
     * @return {@code true} if the points are at most the distance apart, {@code false} otherwise
     */
    public static boolean isWithinDistance(int firstX, int firstY, int secondX, int secondY, int distance) {
        long horizontal = (long) secondX - firstX;
        long vertical = (long) secondY - firstY;
        long range = distance;

        return horizontal * horizontal + vertical * vertical <= range * range;
    }

    /**
     * Computes the approximate integer distance between two points with shifts only: the larger coordinate difference when the smaller one is at
     * most a quarter of it, otherwise a weighted sum of both differences. The arithmetic is {@code int} arithmetic and wraps like it.
     *
     * @param firstX  the x coordinate of the first point
     * @param firstY  the y coordinate of the first point
     * @param secondX the x coordinate of the second point
     * @param secondY the y coordinate of the second point
     * @return the approximate distance
     */
    public static int getApproximateDistance(int firstX, int firstY, int secondX, int secondY) {
        int horizontal = Math.abs(secondX - firstX);
        int vertical = Math.abs(secondY - firstY);
        int maximum = Math.max(horizontal, vertical);
        int minimum = Math.min(horizontal, vertical);

        if ((maximum >> STRAIGHT_RATIO_SHIFT) >= minimum) {
            return maximum;
        }

        int scaledMinimum = minimum + (minimum >> MINIMUM_HALF_SHIFT);

        return maximum + (scaledMinimum >> MINIMUM_FINE_SHIFT) - (maximum >> MAXIMUM_FINE_SHIFT) - (maximum >> MAXIMUM_COARSE_SHIFT) +
               (scaledMinimum >> MINIMUM_COARSE_SHIFT);
    }

    private IntegerMathUtils() {
    }
}
