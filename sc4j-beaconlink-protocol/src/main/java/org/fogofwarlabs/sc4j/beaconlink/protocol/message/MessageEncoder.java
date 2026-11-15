package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;

import java.util.random.RandomGenerator;

/**
 * Writes signed Beacon.Link datagrams of one sender ({@link Envelope} describes the layout): the timestamp comes from the protocol clock,
 * the nonce from the random generator, and the sender's key pair signs every byte before the signature.
 * <p>
 * A request that a peer answers before the sender has proved its address (a ping, a registration, a hello, a dial-back) is padded with zero
 * bytes to {@value #UNVALIDATED_REQUEST_LENGTH} bytes, so that the answer is never longer than the request; a request with a longer answer
 * (a {@link FindPeersMessage}, a {@link GetTopicMessage}) is padded to the length of its longest answer.
 */
public final class MessageEncoder {
    /**
     * The length an unvalidated request is padded to: at least the length of every answer to it.
     */
    public static final int UNVALIDATED_REQUEST_LENGTH = 256;

    private static final long MAXIMUM_REQUEST_NUMBER = 0xffffffffL;


    private final @NotNull IdentityKeyPair sender;
    private final @NotNull ProtocolClock clock;
    private final @NotNull RandomGenerator random;

    /**
     * Creates a {@link MessageEncoder} instance.
     *
     * @param sender the sender's key pair
     * @param clock  the clock of the timestamps
     * @param random the source of the nonces
     */
    public MessageEncoder(@NotNull IdentityKeyPair sender, @NotNull ProtocolClock clock, @NotNull RandomGenerator random) {
        this.sender = sender;
        this.clock  = clock;
        this.random = random;
    }

    /**
     * @return the sender's key
     */
    public @NotNull IdentityKey getSenderKey() {
        return this.sender.getPublicKey();
    }


    /**
     * Encodes a request.
     *
     * @param message       the body
     * @param requestNumber the request number the reply repeats, an unsigned 32-bit value
     * @return the signed datagram
     * @throws IllegalArgumentException if the kind is not sent as a request or the request number is out of range
     */
    public byte @NotNull [] encodeRequest(@NotNull Message message, long requestNumber) {
        return this.encode(message, false, requestNumber, 0);
    }

    /**
     * Encodes a request padded to the length of its longest answer: {@link PeersMessage#MAXIMUM_DATAGRAM_LENGTH} bytes for a
     * {@link FindPeersMessage}, {@link TopicPeersMessage#MAXIMUM_DATAGRAM_LENGTH} bytes for a {@link GetTopicMessage},
     * {@value #UNVALIDATED_REQUEST_LENGTH} bytes for the others (a ping, a registration, a hello, a dial-back).
     *
     * @param message       the body
     * @param requestNumber the request number the reply repeats, an unsigned 32-bit value
     * @return the signed datagram
     * @throws IllegalArgumentException if the kind is not sent as a request or the request number is out of range
     */
    public byte @NotNull [] encodePaddedRequest(@NotNull Message message, long requestNumber) {
        int length = switch (message.getKind()) {
            case FIND_PEERS -> PeersMessage.MAXIMUM_DATAGRAM_LENGTH;
            case GET_TOPIC -> TopicPeersMessage.MAXIMUM_DATAGRAM_LENGTH;
            default -> UNVALIDATED_REQUEST_LENGTH;
        };

        return this.encode(message, false, requestNumber, length);
    }

    /**
     * Encodes a reply.
     *
     * @param message       the body
     * @param requestNumber the request number of the answered request
     * @return the signed datagram
     * @throws IllegalArgumentException if the kind is not sent as a reply or the request number is out of range
     */
    public byte @NotNull [] encodeReply(@NotNull Message message, long requestNumber) {
        return this.encode(message, true, requestNumber, 0);
    }

    private byte @NotNull [] encode(@NotNull Message message, boolean reply, long requestNumber, int minimumLength) {
        MessageKind kind = message.getKind();

        if (reply ? !kind.isReply() : !kind.isRequest()) {
            throw Exceptions.newIllegalArgumentException("A {0} is not sent as a {1}.", kind, reply ? "reply" : "request");
        }

        if (requestNumber < 0L || requestNumber > MAXIMUM_REQUEST_NUMBER) {
            throw Exceptions.newIllegalArgumentException("The request number {0} is outside 0..{1}.", requestNumber, MAXIMUM_REQUEST_NUMBER);
        }

        WireWriter writer = new WireWriter(Envelope.MAXIMUM_DATAGRAM_LENGTH);

        writer.writeBytes(Envelope.MAGIC);
        writer.writeUnsigned8(Envelope.PROTOCOL_VERSION);
        writer.writeUnsigned8(kind.getCode());
        writer.writeUnsigned8(reply ? Envelope.REPLY_FLAG : 0);
        writer.writeUnsigned32(requestNumber);
        writer.writeInt64(this.clock.getEpochMillis());
        writer.writeInt64(this.random.nextLong());
        this.sender.getPublicKey().write(writer);

        int bodyLengthPosition = writer.getPosition();

        writer.writeUnsigned16(0);
        message.writeBody(writer);
        writer.setUnsigned16(bodyLengthPosition, writer.getPosition() - bodyLengthPosition - Short.BYTES);

        int paddingLength = Math.max(0, minimumLength - writer.getPosition() - Envelope.TRAILER_LENGTH);

        writer.writeUnsigned16(paddingLength);
        writer.writeZeroBytes(paddingLength);

        byte[] unsigned = writer.toByteArray();

        writer.writeBytes(this.sender.sign(unsigned, 0, unsigned.length));

        return writer.toByteArray();
    }
}
