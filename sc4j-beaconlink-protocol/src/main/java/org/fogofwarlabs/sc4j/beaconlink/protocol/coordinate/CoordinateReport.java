package org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tells another host where a host stands in the latency space: its {@link NetworkCoordinate} and the error of that coordinate (the moving
 * relative error of its estimates, {@code 0} to {@value #MAXIMUM_ERROR}), so that the receiver weighs a sample against it by how much each
 * side trusts its own coordinate. Peers carry it in their cards, in their answers to lookups and in their answers to registrations; the
 * receiver of an answer to its own request updates its coordinate from the measured round trip time.
 * <p>
 * On the wire ({@value #LENGTH} bytes): the coordinate and the error in hundredths (one byte, at most {@code 150}).
 */
public final class CoordinateReport {
    /**
     * The largest error: the error of a host that knows nothing yet.
     */
    public static final double MAXIMUM_ERROR = 1.5;
    /**
     * The length of the wire form.
     */
    public static final int LENGTH = NetworkCoordinate.LENGTH + 1;
    /**
     * The report of a host that knows nothing yet: the origin with the largest error, which a receiver with a
     * settled coordinate gives little weight.
     */
    public static final @NotNull CoordinateReport UNKNOWN = new CoordinateReport(NetworkCoordinate.ORIGIN, MAXIMUM_ERROR);

    private static final double ERROR_SCALE = 100.0;
    private static final int MAXIMUM_ERROR_CODE = (int) Math.round(MAXIMUM_ERROR * ERROR_SCALE);

    /**
     * Reads a report.
     *
     * @param reader the reader
     * @return the report, or {@code null} if the reader failed or a value is out of range
     */
    public static @Nullable CoordinateReport read(@NotNull WireReader reader) {
        NetworkCoordinate coordinate = NetworkCoordinate.read(reader);
        int errorCode = reader.readUnsigned8();

        if (errorCode > MAXIMUM_ERROR_CODE) {
            reader.fail();
        }

        return coordinate == null || reader.isFailed() ? null : new CoordinateReport(coordinate, errorCode / ERROR_SCALE);
    }


    private final @NotNull NetworkCoordinate coordinate;
    private final double error;

    /**
     * Creates a {@link CoordinateReport} instance.
     *
     * @param coordinate the host's coordinate
     * @param error      the error of the coordinate, {@code 0} to {@value #MAXIMUM_ERROR}
     * @throws IllegalArgumentException if the error is out of range
     */
    public CoordinateReport(@NotNull NetworkCoordinate coordinate, double error) {
        if (!(error >= 0.0 && error <= MAXIMUM_ERROR)) {
            throw Exceptions.newIllegalArgumentException("The coordinate error {0} is outside 0..{1}.", error, MAXIMUM_ERROR);
        }

        this.coordinate = coordinate;
        this.error      = error;
    }

    /**
     * @return the host's coordinate
     */
    public @NotNull NetworkCoordinate getCoordinate() {
        return this.coordinate;
    }

    /**
     * @return the error of the coordinate, {@code 0} to {@value #MAXIMUM_ERROR}
     */
    public double getError() {
        return this.error;
    }


    /**
     * Writes the report, the error rounded to hundredths.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        this.coordinate.write(writer);
        writer.writeUnsigned8((int) Math.round(this.error * ERROR_SCALE));
    }

    /**
     * @return the coordinate and the error, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.coordinate + " error " + this.error;
    }
}
