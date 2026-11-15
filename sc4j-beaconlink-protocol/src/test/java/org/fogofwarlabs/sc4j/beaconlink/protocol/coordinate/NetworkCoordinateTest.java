package org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate;

import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the wire form of network coordinates and their reports: values are written in whole milliseconds, an optional coordinate reads back
 * as absent or present, and a reader refuses values out of range without throwing.
 */
final class NetworkCoordinateTest {
    private static final long UNSIGNED_32_MASK = 0xffffffffL;
    private static final int UNKNOWN_FLAG = 2;
    private static final int TOO_LARGE_ERROR_CODE = 151;

    private static byte @NotNull [] write(@NotNull NetworkCoordinate coordinate) {
        WireWriter writer = new WireWriter(NetworkCoordinate.LENGTH);

        coordinate.write(writer);

        return writer.toByteArray();
    }

    private static byte @NotNull [] writeValues(long @NotNull [] values) {
        WireWriter writer = new WireWriter(NetworkCoordinate.LENGTH);

        for (long value : values) {
            writer.writeUnsigned32(value & UNSIGNED_32_MASK);
        }

        return writer.toByteArray();
    }

    @Test
    void write_should_round_every_value_to_whole_milliseconds() {
        NetworkCoordinate coordinate = new NetworkCoordinate(new double[]{12.4, -7.6, 0.5}, 3.49);
        byte[] bytes = write(coordinate);
        NetworkCoordinate read = NetworkCoordinate.read(new WireReader(bytes, 0, bytes.length));

        Assertions.assertEquals(NetworkCoordinate.LENGTH, bytes.length);
        Assertions.assertNotNull(read);
        Assertions.assertEquals(new NetworkCoordinate(new double[]{12.0, -8.0, 1.0}, 3.0), read);
        Assertions.assertEquals(Math.sqrt(12.0 * 12.0 + 8.0 * 8.0 + 1.0) + 3.0, read.estimateRoundTripMillis(NetworkCoordinate.ORIGIN), 1.0e-9);
    }

    @Test
    void read_should_refuse_values_out_of_range() {
        long tooFar = NetworkCoordinate.MAXIMUM_MILLIS + 1L;
        byte[] farComponent = writeValues(new long[]{0L, -tooFar, 0L, 0L});
        byte[] negativeHeight = writeValues(new long[]{0L, 0L, 0L, -1L});
        byte[] highHeight = writeValues(new long[]{0L, 0L, 0L, tooFar});
        WireReader reader = new WireReader(farComponent, 0, farComponent.length);

        Assertions.assertNull(NetworkCoordinate.read(reader));
        Assertions.assertTrue(reader.isFailed());
        Assertions.assertNull(NetworkCoordinate.read(new WireReader(negativeHeight, 0, negativeHeight.length)));
        Assertions.assertNull(NetworkCoordinate.read(new WireReader(highHeight, 0, highHeight.length)));

        // The smallest int: its absolute value is negative, so a check by absolute value would let it through to the constructor.
        byte[] smallestComponent = writeValues(new long[]{Integer.MIN_VALUE, 0L, 0L, 0L});
        WireReader smallestReader = new WireReader(smallestComponent, 0, smallestComponent.length);

        Assertions.assertNull(NetworkCoordinate.read(smallestReader));
        Assertions.assertTrue(smallestReader.isFailed());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NetworkCoordinate(new double[]{Double.NaN, 0.0, 0.0}, 0.0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NetworkCoordinate(new double[]{0.0, 0.0}, 0.0));

        WireWriter writer = new WireWriter(CoordinateReport.LENGTH);

        NetworkCoordinate.ORIGIN.write(writer);
        writer.writeUnsigned8(TOO_LARGE_ERROR_CODE);

        byte[] largeError = writer.toByteArray();

        Assertions.assertNull(CoordinateReport.read(new WireReader(largeError, 0, largeError.length)));
    }

    @Test
    void read_optional_should_read_back_absent_and_present_coordinates() {
        NetworkCoordinate coordinate = new NetworkCoordinate(new double[]{-300.0, 25.0, 9.0}, 12.0);
        WireWriter writer = new WireWriter(1 + NetworkCoordinate.MAXIMUM_OPTIONAL_LENGTH + 1);

        NetworkCoordinate.writeOptional(writer, null);
        NetworkCoordinate.writeOptional(writer, coordinate);
        writer.writeUnsigned8(UNKNOWN_FLAG);

        byte[] bytes = writer.toByteArray();
        WireReader reader = new WireReader(bytes, 0, bytes.length);

        Assertions.assertNull(NetworkCoordinate.readOptional(reader));
        Assertions.assertFalse(reader.isFailed());
        Assertions.assertEquals(coordinate, NetworkCoordinate.readOptional(reader));
        Assertions.assertNull(NetworkCoordinate.readOptional(reader));
        Assertions.assertTrue(reader.isFailed(), "an unknown flag fails the reader");
    }
}
