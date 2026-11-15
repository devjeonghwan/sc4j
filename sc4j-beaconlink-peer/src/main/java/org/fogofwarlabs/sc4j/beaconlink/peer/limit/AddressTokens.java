package org.fogofwarlabs.sc4j.beaconlink.peer.limit;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Creates and checks the address tokens of registrations: an HMAC-SHA256 of the source address, the port, the sender key and the time bucket
 * under a secret of this node run, cut to {@value #TOKEN_LENGTH} bytes. Only a client that receives at the source address learns the token,
 * so a registration with a valid token proves the address. A token is valid in its time bucket and the next one.
 */
public final class AddressTokens {
    /**
     * The byte length of a token.
     */
    public static final int TOKEN_LENGTH = 16;
    /**
     * The byte length of the secret.
     */
    public static final int SECRET_LENGTH = 32;
    /**
     * The length of a time bucket in milliseconds.
     */
    public static final long BUCKET_MILLIS = 60_000L;

    private static final @NotNull String ALGORITHM = "HmacSHA256";


    private final @NotNull SecretKeySpec secret;

    /**
     * Creates an {@link AddressTokens} instance.
     *
     * @param secret the {@value #SECRET_LENGTH} random secret bytes of this node run
     * @throws IllegalArgumentException if the secret has the wrong length
     */
    public AddressTokens(byte @NotNull [] secret) {
        if (secret.length != SECRET_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The token secret has {0} bytes, not {1}.", secret.length, SECRET_LENGTH);
        }

        this.secret = new SecretKeySpec(secret.clone(), ALGORITHM);
    }


    /**
     * Creates the token of an address and a key.
     *
     * @param address   the observed source address
     * @param key       the sender key
     * @param nowMillis the current time
     * @return the {@value #TOKEN_LENGTH} token bytes
     */
    public byte @NotNull [] create(@NotNull InetSocketAddress address, @NotNull IdentityKey key, long nowMillis) {
        return this.compute(address, key, Math.floorDiv(nowMillis, BUCKET_MILLIS));
    }

    /**
     * Checks a token.
     *
     * @param token     the token the client repeated
     * @param address   the observed source address
     * @param key       the sender key
     * @param nowMillis the current time
     * @return {@code true} if the token was created for the address and the key in this time bucket or the previous one, {@code false} otherwise
     */
    public boolean isValid(byte @NotNull [] token, @NotNull InetSocketAddress address, @NotNull IdentityKey key, long nowMillis) {
        if (token.length != TOKEN_LENGTH) {
            return false;
        }

        long bucket = Math.floorDiv(nowMillis, BUCKET_MILLIS);

        return MessageDigest.isEqual(token, this.compute(address, key, bucket)) ||
               MessageDigest.isEqual(token, this.compute(address, key, bucket - 1));
    }

    private byte @NotNull [] compute(@NotNull InetSocketAddress address, @NotNull IdentityKey key, long bucket) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);

            mac.init(this.secret);
            mac.update(address.getAddress().getAddress());
            mac.update(ByteBuffer.allocate(Short.BYTES + Long.BYTES).putShort((short) address.getPort()).putLong(bucket).array());
            mac.update(key.getBytes());

            return Arrays.copyOf(mac.doFinal(), TOKEN_LENGTH);
        } catch (GeneralSecurityException exception) {
            throw Exceptions.newIllegalStateException(exception, "The JDK cannot compute {0}.", ALGORITHM);
        }
    }
}
