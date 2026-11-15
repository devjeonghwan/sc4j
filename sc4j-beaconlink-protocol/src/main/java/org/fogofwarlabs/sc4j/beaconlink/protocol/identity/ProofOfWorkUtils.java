package org.fogofwarlabs.sc4j.beaconlink.protocol.identity;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.DoublePredicate;

/**
 * Computes the proofs of work of Beacon.Link keys: the SHA-256 hash of a domain text, a subject (the bytes the proof is for) and a 64-bit
 * nonce, and the search for the smallest nonce whose hash starts with a number of zero bits. A peer's ID proof ({@link PeerId}, the subject is
 * the peer key) and a player's stamp ({@link PlayerStamp}, the subject is the player key and the name) use it with their own domain texts.
 */
final class ProofOfWorkUtils {
    /**
     * The byte length of a hash.
     */
    static final int HASH_LENGTH = 32;
    /**
     * The bit length of a hash: the largest difficulty.
     */
    static final int HASH_BIT_LENGTH = HASH_LENGTH * Byte.SIZE;
    /**
     * The result of a search that its progress callback stopped.
     */
    static final long CANCELLED = -1L;

    /**
     * The number of nonces between two progress reports; a power of two.
     */
    private static final long PROGRESS_INTERVAL = 1L << 16;
    /**
     * The largest share a progress report gives: the search may need more than the expected hashes.
     */
    private static final double MAXIMUM_SHARE = 0.99;

    private static final @NotNull String DIGEST_ALGORITHM = "SHA-256";
    private static final int NONCE_LENGTH = Long.BYTES;
    private static final int BYTE_MASK = 0xff;

    /**
     * Hashes a domain text, a subject and a nonce.
     *
     * @param domain  the domain text
     * @param subject the bytes the proof is for
     * @param nonce   the nonce
     * @return the {@value #HASH_LENGTH} hash bytes
     */
    static byte @NotNull [] hash(byte @NotNull [] domain, byte @NotNull [] subject, long nonce) {
        MessageDigest digest = createDigest();
        byte[] nonceBytes = new byte[NONCE_LENGTH];

        writeNonce(nonceBytes, nonce);
        digest.update(domain);
        digest.update(subject);
        digest.update(nonceBytes);

        return digest.digest();
    }

    /**
     * Checks a nonce: whether the hash of the domain, the subject and the nonce starts with enough zero bits.
     *
     * @param domain     the domain text
     * @param subject    the bytes the proof is for
     * @param nonce      the nonce
     * @param difficulty the number of leading zero bits the hash needs
     * @return {@code true} if the hash starts with at least {@code difficulty} zero bits, {@code false} otherwise
     */
    static boolean isValid(byte @NotNull [] domain, byte @NotNull [] subject, long nonce, int difficulty) {
        return countLeadingZeroBits(hash(domain, subject, nonce)) >= difficulty;
    }

    /**
     * Finds the smallest non-negative nonce of a subject whose hash starts with enough zero bits: about two to the power of the difficulty
     * hashes. The result is the same for the same domain, subject and difficulty.
     *
     * @param domain     the domain text
     * @param subject    the bytes the proof is for
     * @param difficulty the number of leading zero bits the hash needs, at most {@value #HASH_BIT_LENGTH}
     * @return the nonce
     * @throws IllegalArgumentException if the difficulty is outside {@code 0..}{@value #HASH_BIT_LENGTH}
     */
    static long solve(byte @NotNull [] domain, byte @NotNull [] subject, int difficulty) {
        return solve(domain, subject, difficulty, share -> true);
    }

    /**
     * Finds the smallest non-negative nonce of a subject whose hash starts with enough zero bits, as {@link #solve(byte[], byte[], int)},
     * reporting the progress every 65536 nonces on the calling thread.
     *
     * @param domain     the domain text
     * @param subject    the bytes the proof is for
     * @param difficulty the number of leading zero bits the hash needs, at most {@value #HASH_BIT_LENGTH}
     * @param progress   receives the share of the expected hashes done so far (below {@code 1}) and returns {@code false} to stop the search
     * @return the nonce, or {@value #CANCELLED} if the progress callback stopped the search
     * @throws IllegalArgumentException if the difficulty is outside {@code 0..}{@value #HASH_BIT_LENGTH}
     */
    static long solve(byte @NotNull [] domain, byte @NotNull [] subject, int difficulty, @NotNull DoublePredicate progress) {
        if (difficulty < 0 || difficulty > HASH_BIT_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The proof difficulty {0} is outside 0..{1}.", difficulty, HASH_BIT_LENGTH);
        }

        MessageDigest prefix = createDigest();
        byte[] nonceBytes = new byte[NONCE_LENGTH];
        byte[] hash = new byte[HASH_LENGTH];
        double expectedHashes = Math.scalb(1.0, difficulty);

        prefix.update(domain);
        prefix.update(subject);

        for (long nonce = 0L; nonce >= 0L; nonce++) {
            if (nonce > 0L && (nonce & (PROGRESS_INTERVAL - 1)) == 0L &&
                !progress.test(Math.min(nonce / expectedHashes, MAXIMUM_SHARE))) {
                return CANCELLED;
            }

            MessageDigest digest = cloneDigest(prefix);

            writeNonce(nonceBytes, nonce);
            digest.update(nonceBytes);

            try {
                digest.digest(hash, 0, HASH_LENGTH);
            } catch (DigestException exception) {
                throw Exceptions.newIllegalStateException("The SHA-256 digest failed: {0}", exception.getMessage());
            }

            if (countLeadingZeroBits(hash) >= difficulty) {
                return nonce;
            }
        }

        throw Exceptions.newIllegalStateException("No proof nonce of difficulty {0} exists for {1}.",
                                                  difficulty, HexFormat.of().formatHex(subject));
    }

    private static void writeNonce(byte @NotNull [] target, long nonce) {
        for (int index = 0; index < NONCE_LENGTH; index++) {
            target[index] = (byte) (nonce >>> (Byte.SIZE * (NONCE_LENGTH - 1 - index)));
        }
    }

    private static int countLeadingZeroBits(byte @NotNull [] bytes) {
        int count = 0;

        for (byte value : bytes) {
            int unsigned = value & BYTE_MASK;

            if (unsigned != 0) {
                return count + Integer.numberOfLeadingZeros(unsigned) - (Integer.SIZE - Byte.SIZE);
            }

            count += Byte.SIZE;
        }

        return count;
    }

    private static @NotNull MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw Exceptions.newIllegalStateException("The JDK has no {0} digest.", DIGEST_ALGORITHM);
        }
    }

    private static @NotNull MessageDigest cloneDigest(@NotNull MessageDigest digest) {
        try {
            return (MessageDigest) digest.clone();
        } catch (CloneNotSupportedException exception) {
            throw Exceptions.newIllegalStateException("The {0} digest cannot be copied.", DIGEST_ALGORITHM);
        }
    }


    private ProofOfWorkUtils() {
    }
}
