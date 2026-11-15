package org.fogofwarlabs.sc4j.beaconlink.protocol.identity;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.random.RandomGenerator;

/**
 * Places a peer in the Beacon.Link routing space: a {@value #BIT_LENGTH}-bit identifier that peers compare by the XOR distance (Kademlia).
 * <p>
 * A peer's ID is the SHA-256 hash of a domain text, the peer's key and its proof nonce. The proof nonce is valid when the SHA-256 hash of
 * another domain text, the key and the nonce starts with a number of zero bits (the difficulty, {@value #DEFAULT_PROOF_DIFFICULTY} on the
 * network): finding one costs about two to the power of the difficulty hashes, once per peer key. So a peer cannot choose where its ID lies,
 * and every ID costs work. A receiver checks the proof of every peer it learns ({@link #isProofValid}) before it uses the peer's ID.
 * <p>
 * A lookup target is any ID ({@link #createRandom}, or read from a request).
 */
public final class PeerId implements Comparable<PeerId> {
    /**
     * The byte length of an ID.
     */
    public static final int LENGTH = 32;
    /**
     * The bit length of an ID.
     */
    public static final int BIT_LENGTH = LENGTH * Byte.SIZE;
    /**
     * The proof difficulty of the network: the number of leading zero bits of a valid proof hash.
     */
    public static final int DEFAULT_PROOF_DIFFICULTY = 20;

    private static final byte @NotNull [] ID_DOMAIN = "BLNK peer id 1".getBytes(StandardCharsets.US_ASCII);
    private static final byte @NotNull [] PROOF_DOMAIN = "BLNK peer proof 1".getBytes(StandardCharsets.US_ASCII);
    private static final int BYTE_MASK = 0xff;
    private static final int DISPLAY_BYTES = 6;

    /**
     * Creates a {@link PeerId} instance: the ID of a peer key with its proof nonce. The proof is not checked.
     *
     * @param key        the peer's key
     * @param proofNonce the peer's proof nonce
     * @return the created {@link PeerId} instance
     */
    public static @NotNull PeerId of(@NotNull IdentityKey key, long proofNonce) {
        return new PeerId(ProofOfWorkUtils.hash(ID_DOMAIN, key.getBytes(), proofNonce));
    }

    /**
     * Creates a {@link PeerId} instance from its bytes.
     *
     * @param bytes the {@value #LENGTH} ID bytes
     * @return the created {@link PeerId} instance
     * @throws IllegalArgumentException if the length is not {@value #LENGTH}
     */
    public static @NotNull PeerId of(byte @NotNull [] bytes) {
        if (bytes.length != LENGTH) {
            throw Exceptions.newIllegalArgumentException("A peer ID has {0} bytes, not {1}.", LENGTH, bytes.length);
        }

        return new PeerId(bytes.clone());
    }

    /**
     * Creates a {@link PeerId} instance at a random place, for example the target of a lookup that refreshes a table.
     *
     * @param random the source of the bytes
     * @return the created {@link PeerId} instance
     */
    public static @NotNull PeerId createRandom(@NotNull RandomGenerator random) {
        byte[] bytes = new byte[LENGTH];

        random.nextBytes(bytes);

        return new PeerId(bytes);
    }

    /**
     * Reads an ID from the wire.
     *
     * @param reader the reader
     * @return the ID, or {@code null} if the reader failed
     */
    public static @Nullable PeerId read(@NotNull WireReader reader) {
        byte[] bytes = reader.readBytes(LENGTH);

        return reader.isFailed() ? null : new PeerId(bytes);
    }

    /**
     * Checks a peer's proof nonce.
     *
     * @param key        the peer's key
     * @param proofNonce the peer's proof nonce
     * @param difficulty the number of leading zero bits the proof hash needs
     * @return {@code true} if the proof hash starts with at least {@code difficulty} zero bits, {@code false} otherwise
     */
    public static boolean isProofValid(@NotNull IdentityKey key, long proofNonce, int difficulty) {
        return ProofOfWorkUtils.isValid(PROOF_DOMAIN, key.getBytes(), proofNonce, difficulty);
    }

    /**
     * Finds the smallest non-negative proof nonce of a key: about two to the power of the difficulty hashes. The result is the same for the
     * same key and difficulty, so that a peer finds its nonce again at every start.
     *
     * @param key        the peer's key
     * @param difficulty the number of leading zero bits the proof hash needs, at most {@value #BIT_LENGTH}
     * @return the proof nonce
     * @throws IllegalArgumentException if the difficulty is outside {@code 0..}{@value #BIT_LENGTH}
     */
    public static long solveProof(@NotNull IdentityKey key, int difficulty) {
        return ProofOfWorkUtils.solve(PROOF_DOMAIN, key.getBytes(), difficulty);
    }


    private final byte @NotNull [] bytes;

    private PeerId(byte @NotNull [] bytes) {
        this.bytes = bytes;
    }

    /**
     * @return a copy of the {@value #LENGTH} ID bytes
     */
    public byte @NotNull [] getBytes() {
        return this.bytes.clone();
    }

    /**
     * Counts the leading bits this ID shares with another: the index of the k-bucket that holds the other ID in this ID's table.
     *
     * @param other the other ID
     * @return the number of equal leading bits, {@value #BIT_LENGTH} for an equal ID
     */
    public int getCommonPrefixLength(@NotNull PeerId other) {
        for (int index = 0; index < LENGTH; index++) {
            int difference = (this.bytes[index] ^ other.bytes[index]) & BYTE_MASK;

            if (difference != 0) {
                return index * Byte.SIZE + Integer.numberOfLeadingZeros(difference) - (Integer.SIZE - Byte.SIZE);
            }
        }

        return BIT_LENGTH;
    }

    /**
     * Compares the XOR distances of two IDs to this ID.
     *
     * @param first  the first ID
     * @param second the second ID
     * @return a negative value if the first ID is closer to this ID, a positive value if the second is, {@code 0} if both are equal
     */
    public int compareDistance(@NotNull PeerId first, @NotNull PeerId second) {
        for (int index = 0; index < LENGTH; index++) {
            int firstDistance = (this.bytes[index] ^ first.bytes[index]) & BYTE_MASK;
            int secondDistance = (this.bytes[index] ^ second.bytes[index]) & BYTE_MASK;

            if (firstDistance != secondDistance) {
                return Integer.compare(firstDistance, secondDistance);
            }
        }

        return 0;
    }

    /**
     * Creates a {@link PeerId} instance at a random place of the k-bucket with the given common prefix length: the first bits are this ID's,
     * the next bit differs, the rest is random.
     *
     * @param prefixLength the common prefix length, below {@value #BIT_LENGTH}
     * @param random       the source of the random bits
     * @return the created {@link PeerId} instance
     * @throws IllegalArgumentException if the prefix length is outside {@code 0..}{@value #BIT_LENGTH}{@code -1}
     */
    public @NotNull PeerId createRandomInBucket(int prefixLength, @NotNull RandomGenerator random) {
        if (prefixLength < 0 || prefixLength >= BIT_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The prefix length {0} is outside 0..{1}.", prefixLength, BIT_LENGTH - 1);
        }

        byte[] result = new byte[LENGTH];
        int byteIndex = prefixLength / Byte.SIZE;
        int bitMask = 1 << (Byte.SIZE - 1 - prefixLength % Byte.SIZE);
        int keptMask = ~((bitMask << 1) - 1) & BYTE_MASK;

        random.nextBytes(result);
        System.arraycopy(this.bytes, 0, result, 0, byteIndex);
        result[byteIndex] = (byte) ((this.bytes[byteIndex] & keptMask) | (~this.bytes[byteIndex] & bitMask) |
                                    (result[byteIndex] & (bitMask - 1)));

        return new PeerId(result);
    }


    /**
     * Writes the ID bytes.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        writer.writeBytes(this.bytes);
    }

    /**
     * Orders IDs by their bytes as unsigned numbers.
     *
     * @param other the other ID
     * @return a negative value, zero or a positive value as this ID is below, equal to or above the other
     */
    @Override
    public int compareTo(@NotNull PeerId other) {
        return Arrays.compareUnsigned(this.bytes, other.bytes);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is an ID with the same bytes, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof PeerId id && Arrays.equals(this.bytes, id.bytes);
    }

    /**
     * @return the hash code of the bytes
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    /**
     * @return the first ID bytes in hexadecimal, for logs
     */
    @Override
    public @NotNull String toString() {
        return HexFormat.of().formatHex(this.bytes, 0, DISPLAY_BYTES);
    }
}
