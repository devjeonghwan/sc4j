package org.fogofwarlabs.sc4j.engine.command;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Reads the encoded payload of one command: little-endian values from a range of a byte array, from the start of the
 * range on. A {@link CommandType} reads its command with it; the bytes are what replays, the network and recorders
 * store, so a read must take exactly the bytes the matching write produced.
 * <p>
 * An input is reusable: {@link #reset} points it at the next payload, and a read allocates nothing except where it
 * returns an array. A read past the end of the range fails and leaves the position unchanged.
 */
public final class CommandInput {
    private static final int BYTE_MASK = 0xFF;
    private static final long LONG_BYTE_MASK = 0xFFL;
    /**
     * The widest value {@link #readUnsigned} reads, in bytes.
     */
    private static final int MAXIMUM_VALUE_WIDTH = Long.BYTES;

    private static final byte @NotNull [] NO_BYTES = new byte[0];

    private byte @NotNull [] data;
    private int position;
    private int end;

    /**
     * Creates a {@link CommandInput} instance over no bytes.
     */
    public CommandInput() {
        this.data     = NO_BYTES;
        this.position = 0;
        this.end      = 0;
    }

    /**
     * Creates a {@link CommandInput} instance over a whole array.
     *
     * @param data the payload, not copied
     */
    public CommandInput(byte @NotNull [] data) {
        this();
        this.reset(data, 0, data.length);
    }

    /**
     * @return the offset of the next byte in the array
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * @return the number of bytes left in the range
     */
    public int getRemaining() {
        return this.end - this.position;
    }

    /**
     * Points this input at a range of an array.
     *
     * @param data   the bytes, not copied
     * @param offset the offset of the range
     * @param length the length of the range
     * @return this instance for chaining
     * @throws IndexOutOfBoundsException if the range lies outside the array
     */
    public @NotNull CommandInput reset(byte @NotNull [] data, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw Exceptions.newIndexOutOfBoundsException("The range of {0} bytes at {1} lies outside {2} bytes.", length, offset, data.length);
        }

        this.data     = data;
        this.position = offset;
        this.end      = offset + length;

        return this;
    }

    /**
     * Reads an unsigned BYTE.
     *
     * @return the value, {@code 0} to {@code 255}
     * @throws IOException if no byte is left
     */
    public int readUnsignedByte() throws IOException {
        this.require(Byte.BYTES);

        return this.data[this.position++] & BYTE_MASK;
    }

    /**
     * Reads an unsigned little-endian WORD.
     *
     * @return the value, {@code 0} to {@code 65535}
     * @throws IOException if fewer than two bytes are left
     */
    public int readUnsignedShort() throws IOException {
        return (int) this.readUnsigned(Short.BYTES);
    }

    /**
     * Reads a little-endian DWORD.
     *
     * @return the value as a signed {@code int}
     * @throws IOException if fewer than four bytes are left
     */
    public int readInt() throws IOException {
        return (int) this.readUnsigned(Integer.BYTES);
    }

    /**
     * Reads an unsigned little-endian value of a given width.
     *
     * @param width the width in bytes, {@code 0} to {@code 8}
     * @return the value
     * @throws IllegalArgumentException if the width is outside the range
     * @throws IOException              if fewer bytes than the width are left
     */
    public long readUnsigned(int width) throws IOException {
        if (width < 0 || width > MAXIMUM_VALUE_WIDTH) {
            throw Exceptions.newIllegalArgumentException("Invalid value width: {0}", width);
        }

        this.require(width);

        long value = 0;

        for (int index = width - 1; index >= 0; index--) {
            value = value << Byte.SIZE | (this.data[this.position + index] & LONG_BYTE_MASK);
        }

        this.position += width;

        return value;
    }

    /**
     * Reads bytes into a new array.
     *
     * @param length the number of bytes
     * @return the bytes
     * @throws IOException if fewer bytes than the length are left
     */
    public byte @NotNull [] readBytes(int length) throws IOException {
        this.require(length);

        byte[] bytes = new byte[length];

        System.arraycopy(this.data, this.position, bytes, 0, length);
        this.position += length;

        return bytes;
    }

    private void require(int length) throws IOException {
        if (length < 0 || length > this.end - this.position) {
            throw Exceptions.newIOException("The command payload ends after {0} bytes; {1} more are read.", this.end - this.position, length);
        }
    }
}
