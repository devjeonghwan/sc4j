package org.fogofwarlabs.sc4j.engine.command;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Collects the encoded payload of commands: little-endian values appended to a growing byte buffer. A {@link CommandType}
 * writes its command with it; {@link #toByteArray()} gives the bytes that replays, the network and recorders store.
 * <p>
 * An output is reusable: {@link #clear()} empties it and keeps the buffer.
 */
public final class CommandOutput {
    private static final int INITIAL_CAPACITY = 32;
    /**
     * The widest value {@link #writeUnsigned} writes, in bytes.
     */
    private static final int MAXIMUM_VALUE_WIDTH = Long.BYTES;

    private byte @NotNull [] buffer;
    private int length;

    /**
     * Creates an empty {@link CommandOutput} instance.
     */
    public CommandOutput() {
        this.buffer = new byte[INITIAL_CAPACITY];
        this.length = 0;
    }

    /**
     * @return the number of bytes written since the last {@link #clear()}
     */
    public int getLength() {
        return this.length;
    }

    /**
     * Appends a BYTE.
     *
     * @param value the value; only its low eight bits are written
     * @return this instance for chaining
     */
    public @NotNull CommandOutput writeByte(int value) {
        return this.writeUnsigned(value, Byte.BYTES);
    }

    /**
     * Appends a little-endian WORD.
     *
     * @param value the value; only its low sixteen bits are written
     * @return this instance for chaining
     */
    public @NotNull CommandOutput writeShort(int value) {
        return this.writeUnsigned(value, Short.BYTES);
    }

    /**
     * Appends a little-endian DWORD.
     *
     * @param value the value
     * @return this instance for chaining
     */
    public @NotNull CommandOutput writeInt(int value) {
        return this.writeUnsigned(value, Integer.BYTES);
    }

    /**
     * Appends the low bytes of a value, little-endian.
     *
     * @param value the value
     * @param width the number of bytes, {@code 0} to {@code 8}
     * @return this instance for chaining
     * @throws IllegalArgumentException if the width is outside the range
     */
    public @NotNull CommandOutput writeUnsigned(long value, int width) {
        if (width < 0 || width > MAXIMUM_VALUE_WIDTH) {
            throw Exceptions.newIllegalArgumentException("Invalid value width: {0}", width);
        }

        this.ensureCapacity(width);

        for (int index = 0; index < width; index++) {
            this.buffer[this.length + index] = (byte) (value >>> index * Byte.SIZE);
        }

        this.length += width;

        return this;
    }

    /**
     * Appends bytes.
     *
     * @param bytes the bytes
     * @return this instance for chaining
     */
    public @NotNull CommandOutput writeBytes(byte @NotNull [] bytes) {
        this.ensureCapacity(bytes.length);

        System.arraycopy(bytes, 0, this.buffer, this.length, bytes.length);
        this.length += bytes.length;

        return this;
    }

    /**
     * Copies the written bytes.
     *
     * @return a new array with the bytes written since the last {@link #clear()}
     */
    public byte @NotNull [] toByteArray() {
        return Arrays.copyOf(this.buffer, this.length);
    }

    /**
     * Empties this output and keeps its buffer.
     */
    public void clear() {
        this.length = 0;
    }

    private void ensureCapacity(int additional) {
        if (this.length + additional > this.buffer.length) {
            this.buffer = Arrays.copyOf(this.buffer, Math.max(this.buffer.length * 2, this.length + additional));
        }
    }
}
