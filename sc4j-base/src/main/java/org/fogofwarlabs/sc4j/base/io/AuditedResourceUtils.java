package org.fogofwarlabs.sc4j.base.io;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Checks resource contents against the SHA-256 digests of their audited assets.
 */
public final class AuditedResourceUtils {
    private static final @NotNull String DIGEST_ALGORITHM = "SHA-256";

    /**
     * Computes the SHA-256 digest of the resource bytes.
     *
     * @param contents the resource bytes
     * @return the lowercase hexadecimal SHA-256 digest of {@code contents}
     * @throws AssertionError if the SHA-256 algorithm is unavailable
     */
    public static @NotNull String computeSha256(byte @NotNull [] contents) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(DIGEST_ALGORITHM).digest(contents));
        } catch (NoSuchAlgorithmException exception) {
            throw Exceptions.newAssertionError(exception, "Required SHA-256 algorithm is unavailable");
        }
    }

    /**
     * Returns the resource if it is the audited asset.
     *
     * @param contents      the resource bytes
     * @param digest        the lowercase hexadecimal SHA-256 digest of the audited asset
     * @param messagePrefix the exception message, followed by {@code ": "} and the actual digest
     * @return {@code contents}
     * @throws UnsupportedOperationException if the digest differs
     */
    public static byte @NotNull [] require(byte @NotNull [] contents, @NotNull String digest, @NotNull String messagePrefix) {
        String actual = computeSha256(contents);

        if (!actual.equals(digest)) {
            throw Exceptions.newUnsupportedOperationException("{0}: {1}", messagePrefix, actual);
        }

        return contents;
    }

    private AuditedResourceUtils() {
    }
}
