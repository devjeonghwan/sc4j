package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Holds one decoded Beacon.Link datagram: the signed envelope around a {@link Message} body. {@link #decode} checks the structure and every
 * field without throwing; the signature ({@link #isSignatureValid()}) and the replay window ({@link ReplayGuard}) are checked separately so
 * that a receiver can drop cheap failures before the signature work.
 * <p>
 * Layout (big-endian): the magic {@code BLNK}, the protocol version {@value #PROTOCOL_VERSION} (one byte), the kind code (one byte), the
 * flags (one byte, bit 0 reply), the request number (unsigned 32-bit; a reply repeats it), the timestamp (sender wall clock, epoch
 * milliseconds, 64-bit), the nonce (64-bit), the sender key, the body length (two bytes), the body, the padding length (two bytes), the zero
 * padding, and the Ed25519 signature of the sender over every byte before it (the magic separates it from other signatures of the key). At
 * most {@value #MAXIMUM_DATAGRAM_LENGTH} bytes.
 */
public final class Envelope {
    /**
     * The protocol version.
     */
    public static final int PROTOCOL_VERSION = 1;
    /**
     * The largest datagram length: below the IPv6 minimum MTU with its headers.
     */
    public static final int MAXIMUM_DATAGRAM_LENGTH = 1200;
    /**
     * The length of the fields before the body.
     */
    public static final int HEADER_LENGTH = 4 + 1 + 1 + 1 + Integer.BYTES + Long.BYTES + Long.BYTES + IdentityKey.LENGTH + Short.BYTES;
    /**
     * The length of the fields after the body without padding bytes: the padding length and the signature.
     */
    public static final int TRAILER_LENGTH = Short.BYTES + IdentityKey.SIGNATURE_LENGTH;
    /**
     * The shortest datagram: an envelope with an empty body and no padding.
     */
    public static final int MINIMUM_DATAGRAM_LENGTH = HEADER_LENGTH + TRAILER_LENGTH;
    /**
     * The largest body length.
     */
    public static final int MAXIMUM_BODY_LENGTH = MAXIMUM_DATAGRAM_LENGTH - MINIMUM_DATAGRAM_LENGTH;
    /**
     * The flag bit of a reply.
     */
    static final int REPLY_FLAG = 1;
    /**
     * The magic at the start of every datagram.
     */
    static final byte @NotNull [] MAGIC = {'B', 'L', 'N', 'K'};

    /**
     * Decodes a datagram. Never throws for malformed input.
     *
     * @param datagram the received bytes
     * @param length   the datagram length, from the start of the array
     * @return the decoded envelope, or why the datagram was rejected
     */
    public static @NotNull DecodeResult decode(byte @NotNull [] datagram, int length) {
        if (length < MINIMUM_DATAGRAM_LENGTH) {
            return DecodeResult.rejected(DecodeFailure.TOO_SHORT);
        }

        if (length > MAXIMUM_DATAGRAM_LENGTH || length > datagram.length) {
            return DecodeResult.rejected(DecodeFailure.TOO_LONG);
        }

        if (!Arrays.equals(datagram, 0, MAGIC.length, MAGIC, 0, MAGIC.length)) {
            return DecodeResult.rejected(DecodeFailure.BAD_MAGIC);
        }

        WireReader reader = new WireReader(datagram, MAGIC.length, length - MAGIC.length);

        if (reader.readUnsigned8() != PROTOCOL_VERSION) {
            return DecodeResult.rejected(DecodeFailure.UNSUPPORTED_VERSION);
        }

        MessageKind kind = MessageKind.find(reader.readUnsigned8());

        if (kind == null) {
            return DecodeResult.rejected(DecodeFailure.UNKNOWN_KIND);
        }

        int flags = reader.readUnsigned8();
        boolean reply = (flags & REPLY_FLAG) != 0;

        if ((flags & ~REPLY_FLAG) != 0 || (reply ? !kind.isReply() : !kind.isRequest())) {
            return DecodeResult.rejected(DecodeFailure.BAD_FLAGS);
        }

        long requestNumber = reader.readUnsigned32();
        long timestampMillis = reader.readInt64();
        long nonce = reader.readInt64();
        IdentityKey sender = IdentityKey.read(reader);
        int bodyLength = reader.readUnsigned16();

        if (sender == null || bodyLength > reader.getRemaining() - TRAILER_LENGTH) {
            return DecodeResult.rejected(DecodeFailure.MALFORMED_BODY);
        }

        WireReader bodyReader = new WireReader(datagram, reader.getPosition(), bodyLength);
        Message body = readBody(kind, bodyReader);

        if (body == null || !bodyReader.isComplete()) {
            return DecodeResult.rejected(DecodeFailure.MALFORMED_BODY);
        }

        reader.skip(bodyLength);

        int paddingLength = reader.readUnsigned16();

        if (paddingLength > reader.getRemaining() - IdentityKey.SIGNATURE_LENGTH || !reader.readZeroBytes(paddingLength)) {
            return DecodeResult.rejected(DecodeFailure.BAD_PADDING);
        }

        int signatureOffset = reader.getPosition();

        reader.skip(IdentityKey.SIGNATURE_LENGTH);

        if (!reader.isComplete()) {
            return DecodeResult.rejected(DecodeFailure.BAD_SIGNATURE_FIELD);
        }

        return DecodeResult.decoded(new Envelope(reply, requestNumber, timestampMillis, nonce, sender, body,
                                                 Arrays.copyOf(datagram, length), signatureOffset));
    }

    private static @Nullable Message readBody(@NotNull MessageKind kind, @NotNull WireReader reader) {
        return switch (kind) {
            case REGISTER -> RegisterMessage.read(reader);
            case OBSERVED_ADDRESS -> ObservedAddressMessage.read(reader);
            case PUBLISH_ROOM -> PublishRoomMessage.read(reader);
            case LIST_ROOMS -> ListRoomsMessage.read(reader);
            case ROOM_LIST -> RoomListMessage.read(reader);
            case WITHDRAW_ROOM -> WithdrawRoomMessage.read(reader);
            case SIGNAL -> SignalMessage.read(reader);
            case PING -> PingMessage.read(reader);
            case HELLO -> HelloMessage.read(reader);
            case FIND_PEERS -> FindPeersMessage.read(reader);
            case RESULT -> ResultMessage.read(reader);
            case PONG -> PongMessage.read(reader);
            case DIAL_BACK -> DialBackMessage.read(reader);
            case PROBE -> ProbeMessage.read(reader);
            case PEERS -> PeersMessage.read(reader);
            case GET_TOPIC -> GetTopicMessage.read(reader);
            case TOPIC_PEERS -> TopicPeersMessage.read(reader);
            case ANNOUNCE -> AnnounceMessage.read(reader);
            case GRAFT -> GraftMessage.read(reader);
            case PRUNE -> PruneMessage.read(reader);
            case GOSSIP -> GossipMessage.read(reader);
            case IHAVE -> IHaveMessage.read(reader);
            case IWANT -> IWantMessage.read(reader);
            case PUBLISH_RECORD -> PublishRecordMessage.read(reader);
            case RECENT -> RecentMessage.read(reader);
            case SEALED -> SealedMessage.read(reader);
        };
    }


    private final boolean reply;
    private final long requestNumber;
    private final long timestampMillis;
    private final long nonce;
    private final @NotNull IdentityKey sender;
    private final @NotNull Message body;

    private final byte @NotNull [] datagram;
    private final int signatureOffset;

    private Envelope(boolean reply, long requestNumber, long timestampMillis, long nonce, @NotNull IdentityKey sender, @NotNull Message body,
                     byte @NotNull [] datagram, int signatureOffset) {
        this.reply           = reply;
        this.requestNumber   = requestNumber;
        this.timestampMillis = timestampMillis;
        this.nonce           = nonce;
        this.sender          = sender;
        this.body            = body;

        this.datagram        = datagram;
        this.signatureOffset = signatureOffset;
    }

    /**
     * @return the kind of the body
     */
    public @NotNull MessageKind getKind() {
        return this.body.getKind();
    }

    /**
     * @return {@code true} if the message is a reply, {@code false} otherwise (it is a request)
     */
    public boolean isReply() {
        return this.reply;
    }

    /**
     * @return the request number: chosen by the requester, repeated by the reply
     */
    public long getRequestNumber() {
        return this.requestNumber;
    }

    /**
     * @return the sender's wall clock time when it sent the message, in epoch milliseconds
     */
    public long getTimestampMillis() {
        return this.timestampMillis;
    }

    /**
     * @return the sender's random nonce of the message
     */
    public long getNonce() {
        return this.nonce;
    }

    /**
     * @return the sender's key
     */
    public @NotNull IdentityKey getSender() {
        return this.sender;
    }

    /**
     * @return the message body
     */
    public @NotNull Message getBody() {
        return this.body;
    }

    /**
     * @return a copy of the whole datagram, as received (a peer relays a signal unchanged)
     */
    public byte @NotNull [] getDatagram() {
        return this.datagram.clone();
    }

    /**
     * @return the datagram length
     */
    public int getLength() {
        return this.datagram.length;
    }

    /**
     * @return {@code true} if the sender's signature is valid for the datagram, {@code false} otherwise
     */
    public boolean isSignatureValid() {
        return this.sender.verify(this.datagram, 0, this.signatureOffset, this.datagram, this.signatureOffset);
    }

    /**
     * @return the kind, the request number and the sender, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.getKind() + (this.reply ? " reply " : " request ") + this.requestNumber + " from " + this.sender;
    }
}
