package org.fogofwarlabs.sc4j.beaconlink.protocol.identity;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Identifies a Beacon.Link participant or node: its Ed25519 public key, {@value #LENGTH} bytes on the wire. Two keys are equal when their
 * bytes are equal. The key checks the signatures of its owner; bytes that are not a valid Ed25519 point verify nothing.
 * <p>
 * A key is used by one thread at a time (the node loop or a client's network thread).
 */
public final class IdentityKey implements Comparable<IdentityKey> {
    /**
     * The byte length of an Ed25519 public key.
     */
    public static final int LENGTH = 32;
    /**
     * The byte length of an Ed25519 signature.
     */
    public static final int SIGNATURE_LENGTH = 64;
    /**
     * The JDK name of the signature algorithm.
     */
    static final @NotNull String ALGORITHM = "Ed25519";
    /**
     * The X.509 prefix of an Ed25519 public key: the encoded key is this prefix followed by the {@value #LENGTH} key bytes.
     */
    static final byte @NotNull [] X509_PREFIX = HexFormat.of().parseHex("302a300506032b6570032100");

    private static final int DISPLAY_BYTES = 8;
    private static final int TAG_BYTES = 3;
    private static final @NotNull String TAG_DIGEST = "SHA-256";

    /**
     * Creates an {@link IdentityKey} instance from its wire bytes.
     *
     * @param bytes the {@value #LENGTH} key bytes
     * @return the created {@link IdentityKey} instance
     * @throws IllegalArgumentException if the length is not {@value #LENGTH}
     */
    public static @NotNull IdentityKey of(byte @NotNull [] bytes) {
        if (bytes.length != LENGTH) {
            throw Exceptions.newIllegalArgumentException("An identity key has {0} bytes, not {1}.", LENGTH, bytes.length);
        }

        return new IdentityKey(bytes.clone());
    }

    /**
     * Reads a key from the wire.
     *
     * @param reader the reader
     * @return the key, or {@code null} if the reader failed
     */
    public static @Nullable IdentityKey read(@NotNull WireReader reader) {
        byte[] bytes = reader.readBytes(LENGTH);

        return reader.isFailed() ? null : new IdentityKey(bytes);
    }


    private final byte @NotNull [] bytes;

    private @Nullable PublicKey publicKey;
    private boolean invalid;

    private IdentityKey(byte @NotNull [] bytes) {
        this.bytes = bytes;

        this.publicKey = null;
        this.invalid   = false;
    }

    /**
     * @return a copy of the {@value #LENGTH} key bytes
     */
    public byte @NotNull [] getBytes() {
        return this.bytes.clone();
    }

    /**
     * @return the key's tag: the first six hexadecimal digits of the SHA-256 hash of the key, shown after a player's name ({@code Name#abcd12})
     */
    public @NotNull String getTag() {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(TAG_DIGEST).digest(this.bytes), 0, TAG_BYTES);
        } catch (NoSuchAlgorithmException exception) {
            throw Exceptions.newIllegalStateException(exception, "The JDK has no {0} digest.", TAG_DIGEST);
        }
    }


    /**
     * Writes the key bytes.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        writer.writeBytes(this.bytes);
    }

    /**
     * Checks a signature of this key's owner.
     *
     * @param data            the signed bytes
     * @param offset          the first signed byte
     * @param length          the number of signed bytes
     * @param signature       the array holding the signature
     * @param signatureOffset the first byte of the {@value #SIGNATURE_LENGTH} signature bytes
     * @return {@code true} if the signature is valid for the bytes, {@code false} otherwise (also for an invalid key)
     */
    public boolean verify(byte @NotNull [] data, int offset, int length, byte @NotNull [] signature, int signatureOffset) {
        PublicKey key = this.getPublicKey();

        if (key == null) {
            return false;
        }

        try {
            Signature verifier = Signature.getInstance(ALGORITHM);

            verifier.initVerify(key);
            verifier.update(data, offset, length);

            return verifier.verify(signature, signatureOffset, SIGNATURE_LENGTH);
        } catch (GeneralSecurityException exception) {
            return false;
        }
    }

    private @Nullable PublicKey getPublicKey() {
        if (this.publicKey == null && !this.invalid) {
            byte[] encoded = Arrays.copyOf(X509_PREFIX, X509_PREFIX.length + LENGTH);

            System.arraycopy(this.bytes, 0, encoded, X509_PREFIX.length, LENGTH);

            try {
                this.publicKey = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(encoded));
            } catch (GeneralSecurityException exception) {
                this.invalid = true;
            }
        }

        return this.publicKey;
    }

    /**
     * Orders keys by their unsigned bytes, a stable order for listings.
     *
     * @param other the other key
     * @return a negative value, zero or a positive value if this key orders before, equal to or after the other key
     */
    @Override
    public int compareTo(@NotNull IdentityKey other) {
        return Arrays.compareUnsigned(this.bytes, other.bytes);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is a key with the same bytes, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof IdentityKey key && Arrays.equals(this.bytes, key.bytes);
    }

    /**
     * @return the hash code of the key bytes
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    /**
     * @return the first key bytes in hexadecimal, for logs
     */
    @Override
    public @NotNull String toString() {
        return HexFormat.of().formatHex(this.bytes, 0, DISPLAY_BYTES);
    }
}
