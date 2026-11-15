package org.fogofwarlabs.sc4j.engine.mod.identity;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Writes and checks the frame the engine file formats (the engine replay and save formats) share around their contents: the magic of the
 * format (four bytes), the format version (two bytes), the identity of the writing mod ({@link ModIdentity#write}), the format's body, and the
 * CRC-32 of everything before it (four bytes), big-endian ({@link DataOutputStream}). Reading checks the frame in this order: length and
 * magic, format version, checksum ("truncated or damaged"), then the identity, which another mod identity fails with a
 * {@link ModIdentityException}.
 */
public final class IdentityFileFrame {
    /**
     * Writes the body of a framed file, after the writer's identity.
     */
    @FunctionalInterface
    public interface BodyWriter {
        /**
         * Writes the body.
         *
         * @param output the output of the file, after the identity
         * @throws IOException if the output cannot be written
         */
        void write(@NotNull DataOutputStream output) throws IOException;
    }

    /**
     * The bytes of the magic, the format version and the checksum: the shortest file that can be framed.
     */
    private static final int MINIMUM_LENGTH = Integer.BYTES + Short.BYTES + Integer.BYTES;
    /**
     * The offset of the body after the magic and the format version, where the identity starts.
     */
    private static final int BODY_START = Integer.BYTES + Short.BYTES;

    private static long computeChecksum(byte @NotNull [] contents, int length) {
        CRC32 checksum = new CRC32();

        checksum.update(contents, 0, length);

        return checksum.getValue();
    }


    private final int magic;
    private final int formatVersion;
    private final @NotNull String formatName;
    private final @NotNull String fileKind;

    /**
     * Creates an {@link IdentityFileFrame} instance of one format.
     *
     * @param magic         the first four bytes of the format's files
     * @param formatVersion the format version this engine writes and reads
     * @param formatName    the name of the format in the messages, such as {@code engine replay}
     * @param fileKind      the kind of the file for people, which the identity refusal names, such as {@code replay}
     */
    public IdentityFileFrame(int magic, int formatVersion, @NotNull String formatName, @NotNull String fileKind) {
        this.magic         = magic;
        this.formatVersion = formatVersion;
        this.formatName    = formatName;
        this.fileKind      = fileKind;
    }

    /**
     * Tests whether file contents start like a file of the format: long enough for the frame and with its magic; the contents may still be
     * damaged.
     *
     * @param contents the file contents
     * @return {@code true} if the contents start like a file of the format, {@code false} otherwise
     */
    public boolean matches(byte @NotNull [] contents) {
        return contents.length >= MINIMUM_LENGTH && ByteBuffer.wrap(contents).getInt(0) == this.magic;
    }

    /**
     * Writes a framed file: the magic, the format version, the writer's identity, the body and the checksum.
     *
     * @param identity the identity of the writing mod
     * @param body     the writer of the body
     * @return the file contents
     */
    public byte @NotNull [] write(@NotNull ModIdentity identity, @NotNull BodyWriter body) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);

        try {
            output.writeInt(this.magic);
            output.writeShort(this.formatVersion);
            identity.write(output);
            body.write(output);
            output.flush();
            output.writeInt((int) computeChecksum(bytes.toByteArray(), bytes.size()));
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }

        return bytes.toByteArray();
    }

    /**
     * Checks the frame of a file (length, magic, format version, checksum) and the writer's identity against the loading one, and opens the
     * body after the identity, up to the checksum.
     *
     * @param contents the file contents
     * @param local    the identity of the loading mod
     * @return the input of the body
     * @throws ModIdentityException if the file was written by another mod identity
     * @throws IOException          if the contents are no file of the format, have another format version, or are truncated or damaged
     */
    public @NotNull DataInputStream openBody(byte @NotNull [] contents, @NotNull ModIdentity local) throws IOException {
        if (!this.matches(contents)) {
            throw Exceptions.newIOException("The file is not an {0}.", this.formatName);
        }

        ByteBuffer frame = ByteBuffer.wrap(contents);
        int version = Short.toUnsignedInt(frame.getShort(Integer.BYTES));
        int bodyEnd = contents.length - Integer.BYTES;

        if (version != this.formatVersion) {
            throw Exceptions.newIOException("The {0} has the format version {1}; this engine reads version {2}.",
                                            this.formatName, version, this.formatVersion);
        }

        long stored = Integer.toUnsignedLong(frame.getInt(bodyEnd));
        long computed = computeChecksum(contents, bodyEnd);

        if (stored != computed) {
            throw Exceptions.newIOException("The {0} is truncated or damaged: its checksum is {1}, its contents give {2}.",
                                            this.formatName, Long.toHexString(stored), Long.toHexString(computed));
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(contents, BODY_START, bodyEnd - BODY_START));
        ModIdentity written = ModIdentity.read(input);
        ModIdentityMismatch mismatch = local.findMismatch(written);

        if (mismatch != null) {
            throw ModIdentityException.create(this.fileKind, mismatch, written, local);
        }

        return input;
    }
}
