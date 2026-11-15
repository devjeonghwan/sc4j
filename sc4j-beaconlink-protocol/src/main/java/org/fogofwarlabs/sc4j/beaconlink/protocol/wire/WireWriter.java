package org.fogofwarlabs.sc4j.beaconlink.protocol.wire;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Writes big-endian protocol fields into a buffer of fixed capacity, in the layout {@link WireReader} reads. The values come from the
 * protocol's own objects, which validate their ranges when they are created, so a value outside its field or a write past the capacity is a
 * programming error and throws.
 */
public final class WireWriter {
    /**
     * The largest length of an address field: an IPv6 address with its family and port.
     */
    public static final int MAXIMUM_ADDRESS_LENGTH = 1 + 2 + WireReader.IPV6_LENGTH;

    private static final int BYTE_MASK = 0xff;
    private static final int BYTE_BITS = 8;
    private static final int SHORT_LENGTH = 2;
    private static final int INT_LENGTH = 4;
    private static final int LONG_LENGTH = 8;
    private static final int MAXIMUM_UNSIGNED_8 = 0xff;
    private static final int MAXIMUM_UNSIGNED_16 = 0xffff;
    private static final long MAXIMUM_UNSIGNED_32 = 0xffffffffL;
    private static final int ADDRESS_HEADER_LENGTH = 1 + SHORT_LENGTH;

    /**
     * Measures a text field.
     *
     * @param text the text
     * @return the UTF-8 length of the text without its length byte
     */
    public static int getTextLength(@NotNull String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Measures an address field.
     *
     * @param address the address
     * @return the length of the field: family, address bytes and port
     */
    public static int getAddressLength(@NotNull InetSocketAddress address) {
        InetAddress host = address.getAddress();

        return ADDRESS_HEADER_LENGTH + (host instanceof Inet6Address ? WireReader.IPV6_LENGTH : WireReader.IPV4_LENGTH);
    }

    private static void checkRange(int value, int maximum) {
        if (value < 0 || value > maximum) {
            throw Exceptions.newIllegalArgumentException("The value {0} is outside 0..{1}.", value, maximum);
        }
    }


    private final byte @NotNull [] buffer;

    private int position;

    /**
     * Creates a {@link WireWriter} instance.
     *
     * @param capacity the largest number of bytes the writer holds
     */
    public WireWriter(int capacity) {
        this.buffer = new byte[capacity];

        this.position = 0;
    }

    /**
     * @return the number of bytes written
     */
    public int getPosition() {
        return this.position;
    }


    /**
     * Writes one unsigned byte.
     *
     * @param value the value, {@code 0} to {@code 255}
     * @throws IllegalArgumentException if the value is out of range or the buffer is full
     */
    public void writeUnsigned8(int value) {
        checkRange(value, MAXIMUM_UNSIGNED_8);
        this.reserve(1);
        this.buffer[this.position++] = (byte) value;
    }

    /**
     * Writes one unsigned 16-bit value.
     *
     * @param value the value, {@code 0} to {@code 65535}
     * @throws IllegalArgumentException if the value is out of range or the buffer is full
     */
    public void writeUnsigned16(int value) {
        checkRange(value, MAXIMUM_UNSIGNED_16);
        this.reserve(SHORT_LENGTH);
        this.buffer[this.position]     = (byte) (value >>> BYTE_BITS);
        this.buffer[this.position + 1] = (byte) value;
        this.position += SHORT_LENGTH;
    }

    /**
     * Replaces an unsigned 16-bit value written before (a length field written ahead of its content).
     *
     * @param position the index of the value's first byte
     * @param value    the value, {@code 0} to {@code 65535}
     * @throws IllegalArgumentException if the value is out of range or the position is not inside the written bytes
     */
    public void setUnsigned16(int position, int value) {
        checkRange(value, MAXIMUM_UNSIGNED_16);

        if (position < 0 || position > this.position - SHORT_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The position {0} is not inside the {1} written bytes.", position, this.position);
        }

        this.buffer[position]     = (byte) (value >>> BYTE_BITS);
        this.buffer[position + 1] = (byte) value;
    }

    /**
     * Writes one unsigned 32-bit value.
     *
     * @param value the value, {@code 0} to {@code 4294967295}
     * @throws IllegalArgumentException if the value is out of range or the buffer is full
     */
    public void writeUnsigned32(long value) {
        if (value < 0L || value > MAXIMUM_UNSIGNED_32) {
            throw Exceptions.newIllegalArgumentException("The value {0} is outside 0..{1}.", value, MAXIMUM_UNSIGNED_32);
        }

        this.writeLong(value, INT_LENGTH);
    }

    /**
     * Writes one signed 64-bit value.
     *
     * @param value the value
     * @throws IllegalArgumentException if the buffer is full
     */
    public void writeInt64(long value) {
        this.writeLong(value, LONG_LENGTH);
    }

    /**
     * Writes bytes.
     *
     * @param bytes the bytes
     * @throws IllegalArgumentException if the buffer is full
     */
    public void writeBytes(byte @NotNull [] bytes) {
        this.writeBytes(bytes, 0, bytes.length);
    }

    /**
     * Writes a byte range.
     *
     * @param bytes  the bytes
     * @param offset the first byte to write
     * @param length the number of bytes
     * @throws IllegalArgumentException if the buffer is full
     */
    public void writeBytes(byte @NotNull [] bytes, int offset, int length) {
        this.reserve(length);
        System.arraycopy(bytes, offset, this.buffer, this.position, length);
        this.position += length;
    }

    /**
     * Writes zero bytes (padding).
     *
     * @param length the number of bytes
     * @throws IllegalArgumentException if the buffer is full
     */
    public void writeZeroBytes(int length) {
        this.reserve(length);
        Arrays.fill(this.buffer, this.position, this.position + length, (byte) 0);
        this.position += length;
    }

    /**
     * Writes a text: its UTF-8 length (one byte) and its UTF-8 bytes.
     *
     * @param text          the text
     * @param maximumLength the largest UTF-8 length the field allows
     * @throws IllegalArgumentException if the text is longer than the field allows or the buffer is full
     */
    public void writeText(@NotNull String text, int maximumLength) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        if (bytes.length > maximumLength) {
            throw Exceptions.newIllegalArgumentException("The text has {0} UTF-8 bytes, more than {1}.", bytes.length, maximumLength);
        }

        this.writeUnsigned8(bytes.length);
        this.writeBytes(bytes);
    }

    /**
     * Writes a socket address: its family, its address bytes and its port.
     *
     * @param address the address, which must be resolved
     * @throws IllegalArgumentException if the address is not resolved or the buffer is full
     */
    public void writeAddress(@NotNull InetSocketAddress address) {
        InetAddress host = address.getAddress();

        if (host instanceof Inet4Address) {
            this.writeUnsigned8(WireReader.IPV4_FAMILY);
        } else if (host instanceof Inet6Address) {
            this.writeUnsigned8(WireReader.IPV6_FAMILY);
        } else {
            throw Exceptions.newIllegalArgumentException("The address {0} is not resolved.", address);
        }

        this.writeBytes(host.getAddress());
        this.writeUnsigned16(address.getPort());
    }

    /**
     * Copies the written bytes.
     *
     * @return a copy of the written bytes
     */
    public byte @NotNull [] toByteArray() {
        return Arrays.copyOf(this.buffer, this.position);
    }

    private void writeLong(long value, int length) {
        this.reserve(length);

        for (int index = 0; index < length; index++) {
            this.buffer[this.position + index] = (byte) (value >>> (length - 1 - index) * BYTE_BITS & BYTE_MASK);
        }

        this.position += length;
    }

    private void reserve(int length) {
        if (length < 0 || length > this.buffer.length - this.position) {
            throw Exceptions.newIllegalArgumentException("The message needs {0} more bytes than its {1} byte limit.",
                                                         length - (this.buffer.length - this.position), this.buffer.length);
        }
    }
}
