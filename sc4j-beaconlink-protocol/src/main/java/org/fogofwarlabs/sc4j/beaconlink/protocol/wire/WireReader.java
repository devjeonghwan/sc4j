package org.fogofwarlabs.sc4j.beaconlink.protocol.wire;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Reads big-endian protocol fields from a byte range without ever throwing for malformed input: a read past the end, a length above its
 * limit, invalid UTF-8 or an unknown address family marks the reader failed and returns a neutral value ({@code 0}, an empty array or text,
 * {@code null}). A decoder reads every field, then checks {@link #isComplete()}: the input was well formed and fully consumed.
 * <p>
 * Addresses are written as their family ({@value #IPV4_FAMILY} or {@value #IPV6_FAMILY}), the address bytes and the port; texts as their
 * UTF-8 length (one byte) and the UTF-8 bytes.
 */
public final class WireReader {
    /**
     * The family code of an IPv4 address.
     */
    public static final int IPV4_FAMILY = 4;
    /**
     * The family code of an IPv6 address.
     */
    public static final int IPV6_FAMILY = 6;
    /**
     * The byte length of an IPv4 address.
     */
    public static final int IPV4_LENGTH = 4;
    /**
     * The byte length of an IPv6 address.
     */
    public static final int IPV6_LENGTH = 16;

    private static final int BYTE_MASK = 0xff;
    private static final int BYTE_BITS = 8;
    private static final int SHORT_LENGTH = 2;
    private static final int INT_LENGTH = 4;
    private static final int LONG_LENGTH = 8;
    private static final long UNSIGNED_INT_MASK = 0xffffffffL;
    private static final byte @NotNull [] EMPTY = new byte[0];


    private final byte @NotNull [] data;
    private final int limit;

    private int position;
    private boolean failed;

    /**
     * Creates a {@link WireReader} instance over a byte range; the range is read in place, not copied.
     *
     * @param data   the bytes
     * @param offset the first byte of the range
     * @param length the byte length of the range
     * @throws IndexOutOfBoundsException if the range is not inside the array
     */
    public WireReader(byte @NotNull [] data, int offset, int length) {
        if (offset < 0 || length < 0 || offset > data.length - length) {
            throw Exceptions.newIndexOutOfBoundsException("The range {0}+{1} is outside the {2} bytes.", offset, length, data.length);
        }

        this.data  = data;
        this.limit = offset + length;

        this.position = offset;
        this.failed   = false;
    }

    /**
     * @return the index of the next byte to read in the array
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * @return the number of bytes left to read
     */
    public int getRemaining() {
        return this.limit - this.position;
    }

    /**
     * @return {@code true} if a read failed or {@link #fail()} was called, {@code false} otherwise
     */
    public boolean isFailed() {
        return this.failed;
    }

    /**
     * @return {@code true} if no read failed and every byte was read, {@code false} otherwise
     */
    public boolean isComplete() {
        return !this.failed && this.position == this.limit;
    }


    /**
     * Marks the reader failed: a decoder found a field value outside its range.
     */
    public void fail() {
        this.failed = true;
    }

    /**
     * Reads one unsigned byte.
     *
     * @return the value, or {@code 0} if the reader failed
     */
    public int readUnsigned8() {
        if (!this.require(1)) {
            return 0;
        }

        return this.data[this.position++] & BYTE_MASK;
    }

    /**
     * Reads one unsigned 16-bit value.
     *
     * @return the value, or {@code 0} if the reader failed
     */
    public int readUnsigned16() {
        if (!this.require(SHORT_LENGTH)) {
            return 0;
        }

        int value = (this.data[this.position] & BYTE_MASK) << BYTE_BITS | this.data[this.position + 1] & BYTE_MASK;

        this.position += SHORT_LENGTH;

        return value;
    }

    /**
     * Reads one unsigned 32-bit value.
     *
     * @return the value, or {@code 0} if the reader failed
     */
    public long readUnsigned32() {
        if (!this.require(INT_LENGTH)) {
            return 0L;
        }

        long value = 0L;

        for (int index = 0; index < INT_LENGTH; index++) {
            value = value << BYTE_BITS | this.data[this.position + index] & BYTE_MASK;
        }

        this.position += INT_LENGTH;

        return value & UNSIGNED_INT_MASK;
    }

    /**
     * Reads one signed 64-bit value.
     *
     * @return the value, or {@code 0} if the reader failed
     */
    public long readInt64() {
        if (!this.require(LONG_LENGTH)) {
            return 0L;
        }

        long value = 0L;

        for (int index = 0; index < LONG_LENGTH; index++) {
            value = value << BYTE_BITS | this.data[this.position + index] & BYTE_MASK;
        }

        this.position += LONG_LENGTH;

        return value;
    }

    /**
     * Reads a copy of the next bytes.
     *
     * @param length the number of bytes
     * @return the bytes, or an empty array if the reader failed
     */
    public byte @NotNull [] readBytes(int length) {
        if (length < 0) {
            this.failed = true;
        }

        if (!this.require(length)) {
            return EMPTY;
        }

        byte[] bytes = new byte[length];

        System.arraycopy(this.data, this.position, bytes, 0, length);
        this.position += length;

        return bytes;
    }

    /**
     * Skips bytes that another reader decodes.
     *
     * @param length the number of bytes
     */
    public void skip(int length) {
        if (length < 0) {
            this.failed = true;
        }

        if (this.require(length)) {
            this.position += length;
        }
    }

    /**
     * Reads bytes that must all be zero (padding).
     *
     * @param length the number of bytes
     * @return {@code true} if the bytes were read and all zero, {@code false} otherwise (the reader failed)
     */
    public boolean readZeroBytes(int length) {
        if (!this.require(length)) {
            return false;
        }

        for (int index = 0; index < length; index++) {
            if (this.data[this.position + index] != 0) {
                this.failed = true;

                return false;
            }
        }

        this.position += length;

        return true;
    }

    /**
     * Reads a text: its UTF-8 length (one byte) and its UTF-8 bytes, which must be well formed.
     *
     * @param maximumLength the largest UTF-8 length the field allows
     * @return the text, or an empty text if the reader failed
     */
    public @NotNull String readText(int maximumLength) {
        int length = this.readUnsigned8();

        if (length > maximumLength) {
            this.failed = true;
        }

        if (!this.require(length)) {
            return "";
        }

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                                                       .onMalformedInput(CodingErrorAction.REPORT)
                                                       .onUnmappableCharacter(CodingErrorAction.REPORT);
        String text;

        try {
            text = decoder.decode(ByteBuffer.wrap(this.data, this.position, length)).toString();
        } catch (CharacterCodingException exception) {
            this.failed = true;

            return "";
        }

        this.position += length;

        return text;
    }

    /**
     * Reads a socket address: its family, its address bytes and its port. No name lookup happens.
     *
     * @return the address, or {@code null} if the reader failed or the family is unknown
     */
    public @Nullable InetSocketAddress readAddress() {
        int family = this.readUnsigned8();
        int length;

        if (family == IPV4_FAMILY) {
            length = IPV4_LENGTH;
        } else if (family == IPV6_FAMILY) {
            length = IPV6_LENGTH;
        } else {
            this.failed = true;

            return null;
        }

        byte[] address = this.readBytes(length);
        int port = this.readUnsigned16();

        if (this.failed) {
            return null;
        }

        try {
            return new InetSocketAddress(InetAddress.getByAddress(address), port);
        } catch (UnknownHostException exception) {
            this.failed = true;

            return null;
        }
    }

    private boolean require(int length) {
        if (this.failed || length > this.limit - this.position) {
            this.failed = true;

            return false;
        }

        return true;
    }
}
