package org.fogofwarlabs.sc4j.beaconlink.protocol.room;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HexFormat;

/**
 * Identifies what a room runs, as opaque bytes: the client writes the engine's mod identity in its wire form, and a room is listed only for
 * clients with equal bytes. Nodes compare fingerprints and never interpret them. On the wire: the length (one byte, {@code 1} to
 * {@value #MAXIMUM_LENGTH}) and the bytes.
 */
public final class ModFingerprint {
    /**
     * The largest fingerprint length.
     */
    public static final int MAXIMUM_LENGTH = 192;

    private static final int DISPLAY_BYTES = 8;

    /**
     * Creates a {@link ModFingerprint} instance.
     *
     * @param bytes the fingerprint bytes
     * @return the created {@link ModFingerprint} instance
     * @throws IllegalArgumentException if the bytes are empty or longer than {@value #MAXIMUM_LENGTH}
     */
    public static @NotNull ModFingerprint of(byte @NotNull [] bytes) {
        if (bytes.length == 0 || bytes.length > MAXIMUM_LENGTH) {
            throw Exceptions.newIllegalArgumentException("A mod fingerprint has 1..{0} bytes, not {1}.", MAXIMUM_LENGTH, bytes.length);
        }

        return new ModFingerprint(bytes.clone());
    }

    /**
     * Reads a fingerprint from the wire.
     *
     * @param reader the reader
     * @return the fingerprint, or {@code null} if the reader failed or the length is out of range
     */
    public static @Nullable ModFingerprint read(@NotNull WireReader reader) {
        int length = reader.readUnsigned8();

        if (length == 0 || length > MAXIMUM_LENGTH) {
            reader.fail();
        }

        byte[] bytes = reader.readBytes(length);

        return reader.isFailed() ? null : new ModFingerprint(bytes);
    }


    private final byte @NotNull [] bytes;

    private ModFingerprint(byte @NotNull [] bytes) {
        this.bytes = bytes;
    }

    /**
     * @return a copy of the fingerprint bytes
     */
    public byte @NotNull [] getBytes() {
        return this.bytes.clone();
    }

    /**
     * @return the length of the fingerprint's wire field: the length byte and the bytes
     */
    public int getEncodedLength() {
        return 1 + this.bytes.length;
    }


    /**
     * Writes the fingerprint.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        writer.writeUnsigned8(this.bytes.length);
        writer.writeBytes(this.bytes);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is a fingerprint with the same bytes, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof ModFingerprint fingerprint && Arrays.equals(this.bytes, fingerprint.bytes);
    }

    /**
     * @return the hash code of the fingerprint bytes
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    /**
     * @return the length and the first bytes in hexadecimal, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.bytes.length + ":" + HexFormat.of().formatHex(this.bytes, 0, Math.min(DISPLAY_BYTES, this.bytes.length));
    }
}
