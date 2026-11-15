package org.fogofwarlabs.sc4j.beaconlink.protocol.wire;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Tests the bounded wire reader: fields written by the writer read back, and every malformed input marks the reader failed without an
 * exception.
 */
final class WireReaderTest {
    private static final int CAPACITY = 128;
    private static final int TEXT_LIMIT = 16;
    private static final int PORT = 40123;
    private static final long LARGE_UNSIGNED = 0xfedcba98L;
    private static final long SIGNED_VALUE = -1234567890123L;
    private static final @NotNull String ACCENTED_TEXT = "R" + (char) 0xe9 + "seau";

    @Test
    void fields_read_back_as_written() throws UnknownHostException {
        WireWriter writer = new WireWriter(CAPACITY);
        InetSocketAddress ipv4 = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), PORT);
        InetSocketAddress ipv6 = new InetSocketAddress(InetAddress.getByName("::1"), PORT);

        writer.writeUnsigned8(0xff);
        writer.writeUnsigned16(0xfffe);
        writer.writeUnsigned32(LARGE_UNSIGNED);
        writer.writeInt64(SIGNED_VALUE);
        writer.writeText(ACCENTED_TEXT, TEXT_LIMIT);
        writer.writeAddress(ipv4);
        writer.writeAddress(ipv6);
        writer.writeZeroBytes(3);

        byte[] bytes = writer.toByteArray();
        WireReader reader = new WireReader(bytes, 0, bytes.length);

        Assertions.assertEquals(0xff, reader.readUnsigned8());
        Assertions.assertEquals(0xfffe, reader.readUnsigned16());
        Assertions.assertEquals(LARGE_UNSIGNED, reader.readUnsigned32());
        Assertions.assertEquals(SIGNED_VALUE, reader.readInt64());
        Assertions.assertEquals(ACCENTED_TEXT, reader.readText(TEXT_LIMIT));
        Assertions.assertEquals(ipv4, reader.readAddress());
        Assertions.assertEquals(ipv6, reader.readAddress());
        Assertions.assertTrue(reader.readZeroBytes(3));
        Assertions.assertTrue(reader.isComplete());
    }

    @Test
    void read_past_the_end_fails_and_returns_neutral_values() {
        WireReader reader = new WireReader(new byte[]{1, 2, 3}, 1, 1);

        Assertions.assertEquals(0, reader.readUnsigned16());
        Assertions.assertTrue(reader.isFailed());
        Assertions.assertEquals(0, reader.readUnsigned8());
        Assertions.assertEquals(0, reader.readBytes(1).length);
        Assertions.assertFalse(reader.isComplete());
    }

    @Test
    void read_text_should_fail_for_invalid_utf8_and_over_limit_length() {
        WireReader invalid = new WireReader(new byte[]{2, (byte) 0xc3, (byte) 0x28}, 0, 3);
        WireReader overLimit = new WireReader(new byte[]{3, 'a', 'b', 'c'}, 0, 4);

        Assertions.assertEquals("", invalid.readText(TEXT_LIMIT));
        Assertions.assertTrue(invalid.isFailed());
        Assertions.assertEquals("", overLimit.readText(2));
        Assertions.assertTrue(overLimit.isFailed());
    }

    @Test
    void read_address_should_fail_for_an_unknown_family_and_a_cut_address() {
        WireReader unknown = new WireReader(new byte[]{5, 127, 0, 0, 1, 0, 1}, 0, 7);
        WireReader cut = new WireReader(new byte[]{4, 127, 0}, 0, 3);

        Assertions.assertNull(unknown.readAddress());
        Assertions.assertTrue(unknown.isFailed());
        Assertions.assertNull(cut.readAddress());
        Assertions.assertTrue(cut.isFailed());
    }

    @Test
    void read_zero_bytes_should_fail_for_a_non_zero_byte() {
        WireReader reader = new WireReader(new byte[]{0, 0, 1}, 0, 3);

        Assertions.assertFalse(reader.readZeroBytes(3));
        Assertions.assertTrue(reader.isFailed());
    }

    @Test
    void write_should_throw_for_values_out_of_range_and_overflow() {
        WireWriter writer = new WireWriter(2);

        Assertions.assertThrows(IllegalArgumentException.class, () -> writer.writeUnsigned8(0x100));
        Assertions.assertThrows(IllegalArgumentException.class, () -> writer.writeUnsigned32(-1L));
        Assertions.assertThrows(IllegalArgumentException.class, () -> writer.writeText("abc", 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> writer.writeInt64(1L));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> writer.writeAddress(InetSocketAddress.createUnresolved("node.example", PORT)));
    }
}
