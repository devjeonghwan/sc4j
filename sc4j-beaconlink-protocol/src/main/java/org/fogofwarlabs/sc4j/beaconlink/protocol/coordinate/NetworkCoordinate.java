package org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Places a host in the network's latency space (a Vivaldi coordinate): a point of {@value #DIMENSIONS} dimensions and a height, in
 * milliseconds. The estimated round trip time between two hosts is the distance of their points plus both heights; the height stands for the
 * host's access link, which every path of the host crosses.
 * <p>
 * On the wire ({@value #LENGTH} bytes): the {@value #DIMENSIONS} components and the height, each a signed 32-bit count of whole milliseconds,
 * so that a coordinate never tells more than a millisecond. A component lies within {@value #MAXIMUM_MILLIS} milliseconds of the origin,
 * the height between {@code 0} and {@value #MAXIMUM_MILLIS}; a reader refuses anything else.
 */
public final class NetworkCoordinate {
    /**
     * The number of dimensions of the point.
     */
    public static final int DIMENSIONS = 3;
    /**
     * The length of the wire form.
     */
    public static final int LENGTH = (DIMENSIONS + 1) * Integer.BYTES;
    /**
     * The length of the wire form of an optional coordinate that is present: the presence flag and the coordinate.
     */
    public static final int MAXIMUM_OPTIONAL_LENGTH = 1 + LENGTH;
    /**
     * The largest distance of a component from the origin and the largest height, in milliseconds.
     */
    public static final int MAXIMUM_MILLIS = 10_000;
    /**
     * The coordinate of a host that knows nothing yet: the origin without height.
     */
    public static final @NotNull NetworkCoordinate ORIGIN = new NetworkCoordinate(new double[DIMENSIONS], 0.0);

    private static final long UNSIGNED_32_MASK = 0xffffffffL;
    private static final int ABSENT = 0;
    private static final int PRESENT = 1;

    /**
     * Reads a coordinate.
     *
     * @param reader the reader
     * @return the coordinate, or {@code null} if the reader failed or a value is out of range
     */
    public static @Nullable NetworkCoordinate read(@NotNull WireReader reader) {
        double[] components = new double[DIMENSIONS];
        boolean valid = true;

        for (int axis = 0; axis < DIMENSIONS; axis++) {
            int component = (int) reader.readUnsigned32();

            // No Math.abs: the absolute value of the smallest int is negative and would pass.
                               valid &= component >= -MAXIMUM_MILLIS && component <= MAXIMUM_MILLIS;
            components[axis] = component;
        }

        int height = (int) reader.readUnsigned32();

        if (!valid || height < 0 || height > MAXIMUM_MILLIS) {
            reader.fail();
        }

        return reader.isFailed() ? null : new NetworkCoordinate(components, height);
    }

    /**
     * Reads an optional coordinate: the presence flag (one byte, {@code 0} absent, {@code 1} present) and the coordinate if present.
     *
     * @param reader the reader
     * @return the coordinate, or {@code null} if it is absent; the reader fails if the flag or a value is out of range
     */
    public static @Nullable NetworkCoordinate readOptional(@NotNull WireReader reader) {
        int flag = reader.readUnsigned8();

        if (flag == ABSENT) {
            return null;
        }

        if (flag != PRESENT) {
            reader.fail();

            return null;
        }

        return read(reader);
    }

    /**
     * Writes an optional coordinate: the presence flag and the coordinate if present.
     *
     * @param writer     the writer
     * @param coordinate the coordinate, or {@code null} for none
     */
    public static void writeOptional(@NotNull WireWriter writer, @Nullable NetworkCoordinate coordinate) {
        if (coordinate == null) {
            writer.writeUnsigned8(ABSENT);
        } else {
            writer.writeUnsigned8(PRESENT);
            coordinate.write(writer);
        }
    }


    private final double @NotNull [] components;
    private final double height;

    /**
     * Creates a {@link NetworkCoordinate} instance.
     *
     * @param components the {@value #DIMENSIONS} components of the point, in milliseconds; copied
     * @param height     the height, in milliseconds
     * @throws IllegalArgumentException if there are not {@value #DIMENSIONS} components, or a value is not finite or out of range
     */
    public NetworkCoordinate(double @NotNull [] components, double height) {
        if (components.length != DIMENSIONS) {
            throw Exceptions.newIllegalArgumentException("A network coordinate has {0} components, not {1}.", DIMENSIONS, components.length);
        }

        for (double component : components) {
            if (!(Math.abs(component) <= MAXIMUM_MILLIS)) {
                throw Exceptions.newIllegalArgumentException("The coordinate component {0} is not within {1} ms of the origin.",
                                                             component, MAXIMUM_MILLIS);
            }
        }

        if (!(height >= 0.0 && height <= MAXIMUM_MILLIS)) {
            throw Exceptions.newIllegalArgumentException("The coordinate height {0} is outside 0..{1} ms.", height, MAXIMUM_MILLIS);
        }

        this.components = components.clone();
        this.height     = height;
    }

    /**
     * Gets one component of the point.
     *
     * @param axis the axis, {@code 0} to {@value #DIMENSIONS} minus one
     * @return the component, in milliseconds
     */
    public double getComponent(int axis) {
        return this.components[axis];
    }

    /**
     * @return the height, in milliseconds
     */
    public double getHeight() {
        return this.height;
    }


    /**
     * Estimates the round trip time to another host: the distance of the two points plus both heights.
     *
     * @param other the other host's coordinate
     * @return the estimated round trip time, in milliseconds
     */
    public double estimateRoundTripMillis(@NotNull NetworkCoordinate other) {
        double sum = 0.0;

        for (int axis = 0; axis < DIMENSIONS; axis++) {
            double difference = this.components[axis] - other.components[axis];

            sum += difference * difference;
        }

        return Math.sqrt(sum) + this.height + other.height;
    }

    /**
     * Writes the coordinate, every value rounded to whole milliseconds.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        for (double component : this.components) {
            writer.writeUnsigned32(Math.round(component) & UNSIGNED_32_MASK);
        }

        writer.writeUnsigned32(Math.round(this.height) & UNSIGNED_32_MASK);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is a coordinate with the same values, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof NetworkCoordinate coordinate && Arrays.equals(this.components, coordinate.components) &&
               Double.compare(this.height, coordinate.height) == 0;
    }

    /**
     * @return the hash code of the values
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.components) ^ Double.hashCode(this.height);
    }

    /**
     * @return the components and the height in whole milliseconds, for logs
     */
    @Override
    public @NotNull String toString() {
        return "(" + Math.round(this.components[0]) + ", " + Math.round(this.components[1]) + ", " + Math.round(this.components[2]) + ") + " +
               Math.round(this.height) + " ms";
    }
}
