package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

/**
 * Proves the password of a private room without sending it. The host of a private room publishes the public half of a join key pair it made
 * for the room; a joiner agrees an X25519 secret between its offer's ephemeral key and that join key, and sends in its offer HMAC-SHA256 with
 * that secret as key over a domain text, the host key, the room number (four bytes), the joiner key and the password's UTF-8 bytes. The host
 * agrees the same secret from the join key's private half and the offer's ephemeral key and compares. Nodes that relay the offer know neither
 * secret nor password, so they cannot test passwords against the proof; every offer has its own ephemeral key, so a proof fits one offer only.
 */
public final class JoinProofUtils {
    /**
     * The length of a proof.
     */
    public static final int PROOF_LENGTH = 32;

    private static final byte @NotNull [] DOMAIN = "SC4J Beacon.Link room password 1".getBytes(StandardCharsets.US_ASCII);
    private static final @NotNull String MAC = "HmacSHA256";

    /**
     * Creates a joiner's proof of a password.
     *
     * @param ephemeral  the joiner's ephemeral key pair of the offer
     * @param joinKey    the room's join key from its advertisement
     * @param host       the host's key
     * @param roomNumber the room number
     * @param joiner     the joiner's key
     * @param password   the password
     * @return the proof, or {@code null} if the join key is not a valid X25519 key
     */
    public static byte @Nullable [] create(@NotNull EphemeralKeyPair ephemeral, byte @NotNull [] joinKey, @NotNull IdentityKey host, long roomNumber,
                                           @NotNull IdentityKey joiner, @NotNull String password) {
        byte[] secret = ephemeral.agree(joinKey);

        return secret == null ? null : compute(secret, host, roomNumber, joiner, password);
    }

    /**
     * Checks a joiner's proof of a password.
     *
     * @param joinKeyPair  the room's join key pair
     * @param ephemeralKey the ephemeral key of the joiner's offer
     * @param host         the host's key
     * @param roomNumber   the room number
     * @param joiner       the joiner's key
     * @param password     the room's password
     * @param proof        the proof of the offer, or {@code null} if it carries none
     * @return {@code true} if the proof is the password's, {@code false} otherwise
     */
    public static boolean verify(@NotNull EphemeralKeyPair joinKeyPair, byte @NotNull [] ephemeralKey, @NotNull IdentityKey host, long roomNumber,
                                 @NotNull IdentityKey joiner, @NotNull String password, byte @Nullable [] proof) {
        byte[] secret = proof == null ? null : joinKeyPair.agree(ephemeralKey);

        return secret != null && MessageDigest.isEqual(compute(secret, host, roomNumber, joiner, password), proof);
    }

    private static byte @NotNull [] compute(byte @NotNull [] secret, @NotNull IdentityKey host, long roomNumber, @NotNull IdentityKey joiner,
                                            @NotNull String password) {
        try {
            Mac mac = Mac.getInstance(MAC);

            mac.init(new SecretKeySpec(secret, MAC));
            mac.update(DOMAIN);
            mac.update(host.getBytes());
            mac.update(ByteBuffer.allocate(Integer.BYTES).putInt((int) roomNumber).array());
            mac.update(joiner.getBytes());

            return mac.doFinal(password.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw Exceptions.newIllegalStateException(exception, "The platform has no {0}: {1}", MAC, exception.getMessage());
        }
    }


    private JoinProofUtils() {
    }
}
