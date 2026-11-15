package org.fogofwarlabs.sc4j.base.binary;

import org.jetbrains.annotations.NotNull;

/**
 * Reads big-endian values from byte arrays, the byte order of network protocols and of formats such as TrueType fonts.
 * <p>
 * The readers do not check bounds beyond the array access itself.
 */
public final class BigEndianUtils {
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
     * Reads an unsigned big-endian WORD.
     *
     * @param data   the bytes
     * @param offset the offset of the WORD
     * @return the unsigned WORD at {@code offset}
     */
    public static int readUnsigned16(byte @NotNull [] data, int offset) {
        return (data[offset] & BYTE_MASK) << Byte.SIZE | (data[offset + 1] & BYTE_MASK);
    }

    /**
     * Reads a big-endian DWORD.
     *
     * @param data   the bytes
     * @param offset the offset of the DWORD
     * @return the DWORD at {@code offset} as a signed {@code int}
     */
    public static int readInt32(byte @NotNull [] data, int offset) {
        return readUnsigned16(data, offset) << Short.SIZE | readUnsigned16(data, offset + Short.BYTES);
    }

    /**
     * Reads an unsigned big-endian DWORD.
     *
     * @param data   the bytes
     * @param offset the offset of the DWORD
     * @return the unsigned DWORD at {@code offset}
     */
    public static long readUnsigned32(byte @NotNull [] data, int offset) {
        return readInt32(data, offset) & DWORD_MASK;
    }

    /**
     * Reads an unsigned big-endian value of up to eight bytes.
     *
     * @param data   the bytes
     * @param offset the offset of the value
     * @param length the value width in bytes, {@code 0} through {@code 8}
     * @return the unsigned value of {@code length} bytes at {@code offset}
     */
    public static long readUnsigned(byte @NotNull [] data, int offset, int length) {
        long value = 0;

        for (int index = 0; index < length; index++) {
            value = value << Byte.SIZE | (data[offset + index] & LONG_BYTE_MASK);
        }

        return value;
    }

    private BigEndianUtils() {
    }
}
