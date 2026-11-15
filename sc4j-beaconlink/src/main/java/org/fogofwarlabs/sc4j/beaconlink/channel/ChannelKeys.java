package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Derives the two ChaCha20-Poly1305 keys of a channel, one per direction, from the X25519 secret of the two ephemeral keys.
 * <p>
 * The ephemeral public keys and the receive numbers travel in the offer and the answer, inside node signals signed by each side's identity key and
 * naming the other side as target; the receiver accepts them only with a valid signature of the expected participant. The derivation also binds both
 * participants: the transcript hash is SHA-256 over a domain text, the initiator's and the responder's identity keys and connection numbers, their
 * ephemeral public keys and their receive numbers, and it is the HKDF-SHA256 salt of the secret. The key of each direction is the first HKDF
 * expansion block of a direction label ({@code initiator to responder}, {@code responder to initiator}). A channel therefore has keys only between
 * the two participants whose signed signals carried its ephemeral keys, and a secret from another pair, another connection of the same account or
 * another channel gives other keys.
 */
public final class ChannelKeys {
    /**
     * The byte length of a ChaCha20-Poly1305 key.
     */
    public static final int KEY_LENGTH = 32;

    private static final byte @NotNull [] DOMAIN = "SC4J Beacon.Link channel 1".getBytes(StandardCharsets.US_ASCII);
    private static final byte @NotNull [] INITIATOR_LABEL = "initiator to responder".getBytes(StandardCharsets.US_ASCII);
    private static final byte @NotNull [] RESPONDER_LABEL = "responder to initiator".getBytes(StandardCharsets.US_ASCII);
    private static final @NotNull String DIGEST = "SHA-256";
    private static final @NotNull String MAC = "HmacSHA256";
    private static final int FIRST_BLOCK = 1;
    private static final int TRANSCRIPT_LENGTH = 2 * IdentityKey.LENGTH + 2 * Long.BYTES + 2 * EphemeralKeyPair.KEY_LENGTH + 2 * Long.BYTES;

    /**
     * Derives the keys of one side of a channel.
     *
     * @param own                    this side's ephemeral key pair
     * @param peerEphemeralKey       the other side's ephemeral public key, from its signed offer or answer
     * @param initiator              whether this side sent the offer
     * @param initiatorIdentity      the identity key of the side that sent the offer
     * @param initiatorConnection    the connection number of the side that sent the offer
     * @param responderIdentity      the identity key of the side that answered
     * @param responderConnection    the connection number of the side that answered
     * @param initiatorReceiveNumber the initiator's receive number
     * @param responderReceiveNumber the responder's receive number
     * @return the keys, or {@code null} if the other side's ephemeral key is invalid
     */
    public static @Nullable ChannelKeys derive(@NotNull EphemeralKeyPair own, byte @NotNull [] peerEphemeralKey, boolean initiator,
                                               @NotNull IdentityKey initiatorIdentity, long initiatorConnection,
                                               @NotNull IdentityKey responderIdentity, long responderConnection,
                                               long initiatorReceiveNumber, long responderReceiveNumber) {
        byte[] secret = own.agree(peerEphemeralKey);

        if (secret == null) {
            return null;
        }

        byte[] initiatorEphemeral = initiator ? own.getPublicKey() : peerEphemeralKey;
        byte[] responderEphemeral = initiator ? peerEphemeralKey : own.getPublicKey();
        WireWriter transcript = new WireWriter(TRANSCRIPT_LENGTH);

        initiatorIdentity.write(transcript);
        transcript.writeInt64(initiatorConnection);
        responderIdentity.write(transcript);
        transcript.writeInt64(responderConnection);
        transcript.writeBytes(initiatorEphemeral);
        transcript.writeBytes(responderEphemeral);
        transcript.writeInt64(initiatorReceiveNumber);
        transcript.writeInt64(responderReceiveNumber);

        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST);

            digest.update(DOMAIN);
            digest.update(transcript.toByteArray());

            byte[] pseudorandomKey = hmac(digest.digest(), secret);
            byte[] initiatorToResponder = expand(pseudorandomKey, INITIATOR_LABEL);
            byte[] responderToInitiator = expand(pseudorandomKey, RESPONDER_LABEL);

            Arrays.fill(secret, (byte) 0);
            Arrays.fill(pseudorandomKey, (byte) 0);

            return initiator ? new ChannelKeys(initiatorToResponder, responderToInitiator)
                             : new ChannelKeys(responderToInitiator, initiatorToResponder);
        } catch (GeneralSecurityException exception) {
            throw Exceptions.newIllegalStateException(exception, "The JDK cannot derive the channel keys with {0} and {1}.", DIGEST, MAC);
        }
    }

    private static byte @NotNull [] expand(byte @NotNull [] pseudorandomKey, byte @NotNull [] label) throws GeneralSecurityException {
        byte[] info = Arrays.copyOf(label, label.length + 1);

        info[label.length] = FIRST_BLOCK;

        return hmac(pseudorandomKey, info);
    }

    private static byte @NotNull [] hmac(byte @NotNull [] key, byte @NotNull [] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(MAC);

        mac.init(new SecretKeySpec(key, MAC));

        return mac.doFinal(data);
    }


    private final byte @NotNull [] sendKey;
    private final byte @NotNull [] receiveKey;

    private ChannelKeys(byte @NotNull [] sendKey, byte @NotNull [] receiveKey) {
        this.sendKey    = sendKey;
        this.receiveKey = receiveKey;
    }

    /**
     * @return the key of the datagrams this side sends
     */
    byte @NotNull [] getSendKey() {
        return this.sendKey;
    }

    /**
     * @return the key of the datagrams this side receives
     */
    byte @NotNull [] getReceiveKey() {
        return this.receiveKey;
    }
}
