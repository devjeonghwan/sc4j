package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.SealedMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Seals and opens whispers end to end: the sender agrees a secret between a fresh X25519 key pair and the target's whisper key (from the
 * target's signed presence), derives a one-time key with HKDF-SHA256 (the salt is the hash of a domain text, both player keys and both X25519
 * public keys) and seals the text with ChaCha20-Poly1305, the two player keys as associated data. The home peers that relay a whisper see
 * only the two player keys and the size; only the holder of the target's whisper private key opens it, and a whisper cannot be passed off as
 * another sender's or another target's.
 */
public final class WhisperSealUtils {
    /**
     * The length of the authentication tag a sealed whisper adds.
     */
    public static final int TAG_LENGTH = 16;

    private static final byte @NotNull [] DOMAIN = "SC4J Beacon.Link whisper 1".getBytes(StandardCharsets.US_ASCII);
    private static final byte @NotNull [] KEY_LABEL = "whisper key".getBytes(StandardCharsets.US_ASCII);
    private static final @NotNull String ALGORITHM = "ChaCha20-Poly1305";
    private static final @NotNull String KEY_ALGORITHM = "ChaCha20";
    private static final @NotNull String DIGEST = "SHA-256";
    private static final @NotNull String MAC = "HmacSHA256";
    private static final int NONCE_LENGTH = 12;
    private static final int FIRST_BLOCK = 1;

    /**
     * Seals a whisper for one connection of a target.
     *
     * @param random           the source of the fresh X25519 key pair
     * @param sender           the sender's player key
     * @param target           the target's player key
     * @param targetConnection the number of the target's connection, whose home peers relay the whisper to it alone
     * @param whisperKey       the X25519 whisper key of the target's connection
     * @param plaintext        the whisper's plain bytes
     * @return the message to the connection's home peer, or {@code null} if the whisper key is invalid
     * @throws IllegalArgumentException if the sealed whisper is longer than {@value SealedMessage#MAXIMUM_SEALED_LENGTH} bytes
     */
    public static @Nullable SealedMessage seal(@NotNull SecureRandom random, @NotNull IdentityKey sender, @NotNull IdentityKey target,
                                               long targetConnection, byte @NotNull [] whisperKey, byte @NotNull [] plaintext) {
        EphemeralKeyPair ephemeral = EphemeralKeyPair.generate(random);
        byte[] secret = ephemeral.agree(whisperKey);

        if (secret == null) {
            return null;
        }

        byte[] ephemeralKey = ephemeral.getPublicKey();

        try {
            Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, secret, sender, target, ephemeralKey, whisperKey);

            return new SealedMessage(target, targetConnection, ephemeralKey, cipher.doFinal(plaintext));
        } catch (GeneralSecurityException exception) {
            throw Exceptions.newIllegalStateException(exception, "The JDK cannot seal a whisper with {0}.", ALGORITHM);
        } finally {
            Arrays.fill(secret, (byte) 0);
        }
    }

    /**
     * Opens a whisper sealed for this player.
     *
     * @param whisperKeys the X25519 key pair whose public key this player's presence carries
     * @param sender      the sender's player key, from the signed envelope
     * @param target      this player's key
     * @param sealed      the message
     * @return the whisper's plain bytes, or {@code null} if the whisper was not sealed for this key pair, the two players or was changed
     */
    public static byte @Nullable [] open(@NotNull EphemeralKeyPair whisperKeys, @NotNull IdentityKey sender, @NotNull IdentityKey target,
                                         @NotNull SealedMessage sealed) {
        byte[] ephemeralKey = sealed.getEphemeralKey();
        byte[] secret = whisperKeys.agree(ephemeralKey);

        if (secret == null) {
            return null;
        }

        try {
            Cipher cipher = createCipher(Cipher.DECRYPT_MODE, secret, sender, target, ephemeralKey, whisperKeys.getPublicKey());

            return cipher.doFinal(sealed.getSealed());
        } catch (AEADBadTagException exception) {
            return null;
        } catch (GeneralSecurityException exception) {
            throw Exceptions.newIllegalStateException(exception, "The JDK cannot open a whisper with {0}.", ALGORITHM);
        } finally {
            Arrays.fill(secret, (byte) 0);
        }
    }

    private static @NotNull Cipher createCipher(int mode, byte @NotNull [] secret, @NotNull IdentityKey sender, @NotNull IdentityKey target,
                                                byte @NotNull [] ephemeralKey, byte @NotNull [] whisperKey) throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance(DIGEST);

        digest.update(DOMAIN);
        digest.update(sender.getBytes());
        digest.update(target.getBytes());
        digest.update(ephemeralKey);
        digest.update(whisperKey);

        byte[] pseudorandomKey = hmac(digest.digest(), secret);
        byte[] info = Arrays.copyOf(KEY_LABEL, KEY_LABEL.length + 1);

        info[KEY_LABEL.length] = FIRST_BLOCK;

        byte[] key = hmac(pseudorandomKey, info);
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        // The key is new for every whisper (a fresh X25519 key pair), so the zero nonce is never used twice with one key.
        cipher.init(mode, new SecretKeySpec(key, KEY_ALGORITHM), new IvParameterSpec(new byte[NONCE_LENGTH]));
        cipher.updateAAD(sender.getBytes());
        cipher.updateAAD(target.getBytes());
        Arrays.fill(pseudorandomKey, (byte) 0);
        Arrays.fill(key, (byte) 0);

        return cipher;
    }

    private static byte @NotNull [] hmac(byte @NotNull [] key, byte @NotNull [] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(MAC);

        mac.init(new SecretKeySpec(key, MAC));

        return mac.doFinal(data);
    }


    private WhisperSealUtils() {
    }
}
