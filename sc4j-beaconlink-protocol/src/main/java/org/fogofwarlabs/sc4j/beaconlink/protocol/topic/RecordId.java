package org.fogofwarlabs.sc4j.beaconlink.protocol.topic;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Identifies one signed record of a topic (a room advertisement, a presence, a chat line): the first {@value #LENGTH} bytes of the SHA-256
 * hash of a domain text and the record's whole wire form, signature included. Members of a topic name records by their ID when they
 * announce them lazily ({@code IHAVE}) and ask for them ({@code IWANT}), and drop a record whose ID they have seen.
 */
public final class RecordId {
    /**
     * The length of an ID.
     */
    public static final int LENGTH = 16;

    private static final @NotNull String DIGEST_ALGORITHM = "SHA-256";
    private static final byte @NotNull [] DOMAIN = "BLNK record 1\0".getBytes(StandardCharsets.US_ASCII);

    /**
     * Creates a {@link RecordId} instance: the ID of a record's wire form.
     *
     * @param encoded the record's whole wire form
     * @return the created {@link RecordId} instance
     */
    public static @NotNull RecordId of(byte @NotNull [] encoded) {
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw Exceptions.newIllegalStateException(exception, "The JDK has no {0} digest.", DIGEST_ALGORITHM);
        }

        digest.update(DOMAIN);

        return new RecordId(Arrays.copyOf(digest.digest(encoded), LENGTH));
    }

    /**
     * Reads an ID from the wire.
     *
     * @param reader the reader
     * @return the ID, or {@code null} if the reader failed
     */
    public static @Nullable RecordId read(@NotNull WireReader reader) {
        byte[] bytes = reader.readBytes(LENGTH);

        return reader.isFailed() ? null : new RecordId(bytes);
    }


    private final byte @NotNull [] bytes;

    private RecordId(byte @NotNull [] bytes) {
        this.bytes = bytes;
    }


    /**
     * Writes the ID.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        writer.writeBytes(this.bytes);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is an ID with the same bytes, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof RecordId id && Arrays.equals(this.bytes, id.bytes);
    }

    /**
     * @return the hash code of the ID bytes
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    /**
     * @return the ID in hexadecimal, for logs
     */
    @Override
    public @NotNull String toString() {
        return HexFormat.of().formatHex(this.bytes);
    }
}
