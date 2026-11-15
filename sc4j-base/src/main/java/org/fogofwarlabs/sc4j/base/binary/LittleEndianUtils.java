package org.fogofwarlabs.sc4j.base.binary;

import org.jetbrains.annotations.NotNull;

/**
 * Reads and writes little-endian values in byte arrays, the byte order of most binary file formats.
 * <p>
 * The readers and writers do not check bounds beyond the array access itself; a writer stores its bytes in ascending order.
 */
public final class LittleEndianUtils {
    /**
     * The masks that read a byte as unsigned, as an {@code int} and as a {@code long}.
     */
    private static final int BYTE_MASK = 0xFF;
    private static final long LONG_BYTE_MASK = 0xFFL;
    /**
     * The mask that reads a DWORD as unsigned.
     */
    private static final long DWORD_MASK = 0xFFFFFFFFL;

    /**
     * Reads an unsigned little-endian WORD.
     *
     * @param data   the bytes
     * @param offset the offset of the WORD
     * @return the unsigned WORD at {@code offset}
     */
    public static int readUnsigned16(byte @NotNull [] data, int offset) {
        return (data[offset] & BYTE_MASK) | (data[offset + 1] & BYTE_MASK) << Byte.SIZE;
    }

    /**
     * Reads a little-endian DWORD.
     *
     * @param data   the bytes
     * @param offset the offset of the DWORD
     * @return the DWORD at {@code offset} as a signed {@code int}
     */
    public static int readInt32(byte @NotNull [] data, int offset) {
        return readUnsigned16(data, offset) | readUnsigned16(data, offset + Short.BYTES) << Short.SIZE;
    }

    /**
     * Reads an unsigned little-endian DWORD.
     *
     * @param data   the bytes
     * @param offset the offset of the DWORD
     * @return the unsigned DWORD at {@code offset}
     */
    public static long readUnsigned32(byte @NotNull [] data, int offset) {
        return readInt32(data, offset) & DWORD_MASK;
    }

    /**
     * Reads a little-endian value of up to four bytes.
     *
     * @param data   the bytes
     * @param offset the offset of the value
     * @param length the value width in bytes, {@code 0} through {@code 4}
     * @return the value of {@code length} bytes at {@code offset}; a {@code 4}-byte value is returned as a signed {@code int}
     */
    public static int readInt(byte @NotNull [] data, int offset, int length) {
        return (int) readUnsigned(data, offset, length);
    }

    /**
     * Reads an unsigned little-endian value of up to eight bytes.
     *
     * @param data   the bytes
     * @param offset the offset of the value
     * @param length the value width in bytes, {@code 0} through {@code 8}
     * @return the unsigned value of {@code length} bytes at {@code offset}
     */
    public static long readUnsigned(byte @NotNull [] data, int offset, int length) {
        long value = 0;

        for (int index = length - 1; index >= 0; index--) {
            value = value << Byte.SIZE | (data[offset + index] & LONG_BYTE_MASK);
        }

        return value;
    }

    /**
     * Writes a little-endian WORD.
     *
     * @param data   the destination
     * @param offset the offset of the WORD
     * @param value  the value in the low sixteen bits
     */
    public static void writeInt16(byte @NotNull [] data, int offset, int value) {
        data[offset]     = (byte) value;
        data[offset + 1] = (byte) (value >>> Byte.SIZE);
    }

    /**
     * Writes a little-endian DWORD.
     *
     * @param data   the destination
     * @param offset the offset of the DWORD
     * @param value  the value
     */
    public static void writeInt32(byte @NotNull [] data, int offset, int value) {
        for (int index = 0; index < Integer.BYTES; index++) {
            data[offset + index] = (byte) (value >>> (index * Byte.SIZE));
        }
    }

    /**
     * Writes a little-endian QWORD: the low DWORD, then the high DWORD.
     *
     * @param data   the destination
     * @param offset the offset of the QWORD
     * @param value  the value
     */
    public static void writeInt64(byte @NotNull [] data, int offset, long value) {
        writeInt32(data, offset, (int) value);
        writeInt32(data, offset + Integer.BYTES, (int) (value >>> Integer.SIZE));
    }

    /**
     * Replaces one byte of a little-endian DWORD.
     *
     * @param dword the DWORD
     * @param shift the bit position of the byte, {@link Byte#SIZE} times its index in the DWORD
     * @param value the byte value in the low eight bits
     * @return the DWORD holding the byte value at the bit position
     */
    public static int replaceByte(int dword, int shift, int value) {
        return dword & ~(BYTE_MASK << shift) | (value & BYTE_MASK) << shift;
    }

    private LittleEndianUtils() {
    }
}
