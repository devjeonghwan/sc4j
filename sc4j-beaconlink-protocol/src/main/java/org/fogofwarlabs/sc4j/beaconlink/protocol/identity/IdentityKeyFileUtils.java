package org.fogofwarlabs.sc4j.beaconlink.protocol.identity;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.io.AtomicFileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Reads and writes the key file of an install ({@code userdata/beaconlink/}) or a node (its state folder): the magic {@code BLKF}, the format
 * version {@value #VERSION}, the {@value IdentityKeyPair#PRIVATE_LENGTH} private key bytes and the {@value IdentityKey#LENGTH} public key
 * bytes. The file is the identity of its owner; it is written once and replaced only atomically.
 */
public final class IdentityKeyFileUtils {
    /**
     * The format version of the key file.
     */
    public static final int VERSION = 1;

    private static final byte @NotNull [] MAGIC = {'B', 'L', 'K', 'F'};
    private static final int LENGTH = MAGIC.length + 1 + IdentityKeyPair.PRIVATE_LENGTH + IdentityKey.LENGTH;

    /**
     * Reads the key pair of a key file, or creates a new key pair and writes it when the file does not exist.
     *
     * @param file   the key file
     * @param random the source of a new private key
     * @return the key pair of the file
     * @throws IOException if the file cannot be read or written, or it is not a valid key file
     */
    public static @NotNull IdentityKeyPair readOrCreate(@NotNull Path file, @NotNull SecureRandom random) throws IOException {
        if (Files.exists(file)) {
            return read(file);
        }

        IdentityKeyPair pair = IdentityKeyPair.generate(random);

        write(file, pair);

        return pair;
    }

    /**
     * Reads a key file.
     *
     * @param file the key file
     * @return the key pair of the file
     * @throws IOException if the file cannot be read or it is not a valid key file
     */
    public static @NotNull IdentityKeyPair read(@NotNull Path file) throws IOException {
        byte[] contents = Files.readAllBytes(file);

        try {
            return decode(contents, file.toString());
        } finally {
            Arrays.fill(contents, (byte) 0);
        }
    }

    /**
     * Checks whether bytes start like a key file: the magic and the version, as a reader of several file kinds tells them apart.
     *
     * @param contents the bytes
     * @return {@code true} if the bytes start with the key file magic and version, {@code false} otherwise
     */
    public static boolean isKeyFile(byte @NotNull [] contents) {
        return contents.length > MAGIC.length && Arrays.equals(contents, 0, MAGIC.length, MAGIC, 0, MAGIC.length) &&
               contents[MAGIC.length] == VERSION;
    }

    /**
     * Reads the key pair of a key file's contents.
     *
     * @param contents the contents of a key file
     * @param source   the name of the contents' source, for the failure message
     * @return the key pair
     * @throws IOException if the contents are not a valid key file
     */
    public static @NotNull IdentityKeyPair decode(byte @NotNull [] contents, @NotNull String source) throws IOException {
        if (contents.length != LENGTH || !isKeyFile(contents)) {
            throw Exceptions.newIOException("The file {0} is not a version {1} Beacon.Link key file.", source, VERSION);
        }

        int privateOffset = MAGIC.length + 1;
        int publicOffset = privateOffset + IdentityKeyPair.PRIVATE_LENGTH;
        byte[] privateBytes = Arrays.copyOfRange(contents, privateOffset, publicOffset);
        IdentityKey publicKey = IdentityKey.of(Arrays.copyOfRange(contents, publicOffset, LENGTH));

        try {
            return IdentityKeyPair.of(privateBytes, publicKey);
        } catch (IllegalArgumentException exception) {
            throw Exceptions.newIOException(exception, "The key file {0} holds keys that do not belong together.", source);
        } finally {
            Arrays.fill(privateBytes, (byte) 0);
        }
    }

    /**
     * Writes a key file atomically.
     *
     * @param file the key file
     * @param pair the key pair
     * @throws IOException if the file cannot be written
     */
    public static void write(@NotNull Path file, @NotNull IdentityKeyPair pair) throws IOException {
        byte[] contents = encode(pair);

        try {
            AtomicFileUtils.write(file, contents);
        } finally {
            Arrays.fill(contents, (byte) 0);
        }
    }

    /**
     * Creates the contents of a key file; the caller clears them after use.
     *
     * @param pair the key pair
     * @return the key file bytes
     */
    public static byte @NotNull [] encode(@NotNull IdentityKeyPair pair) {
        byte[] contents = new byte[LENGTH];
        byte[] privateBytes = pair.getPrivateBytes();
        int privateOffset = MAGIC.length + 1;

        System.arraycopy(MAGIC, 0, contents, 0, MAGIC.length);
        contents[MAGIC.length] = VERSION;
        System.arraycopy(privateBytes, 0, contents, privateOffset, IdentityKeyPair.PRIVATE_LENGTH);
        System.arraycopy(pair.getPublicKey().getBytes(), 0, contents, privateOffset + IdentityKeyPair.PRIVATE_LENGTH, IdentityKey.LENGTH);
        Arrays.fill(privateBytes, (byte) 0);

        return contents;
    }


    private IdentityKeyFileUtils() {
    }
}
