package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tells a storing peer of a topic that the sender is a member of it (answered by {@link ResultMessage}). The sender's address is the source
 * address the storing peer observes; the token of the storing peer's last {@link TopicPeersMessage} proves that the sender receives there,
 * so nobody can announce another host's address. The storing peer keeps the announcement for a limited time; members repeat it before it
 * expires.
 * <p>
 * Body: the topic key, the sender's {@link PeerCard} and the token (length byte, at most {@value RegisterMessage#MAXIMUM_TOKEN_LENGTH} bytes).
 */
public final class AnnounceMessage extends Message {
    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or the token is too long
     */
    public static @Nullable AnnounceMessage read(@NotNull WireReader reader) {
        TopicKey topic = TopicKey.read(reader);
        PeerCard card = PeerCard.read(reader);
        int tokenLength = reader.readUnsigned8();

        if (tokenLength > RegisterMessage.MAXIMUM_TOKEN_LENGTH) {
            reader.fail();
        }

        byte[] token = reader.readBytes(tokenLength);

        return topic == null || card == null || reader.isFailed() ? null : new AnnounceMessage(topic, card, token);
    }


    private final @NotNull TopicKey topic;
    private final @NotNull PeerCard card;
    private final byte @NotNull [] token;

    /**
     * Creates an {@link AnnounceMessage} instance.
     *
     * @param topic the topic
     * @param card  the sender's card
     * @param token the address token of the storing peer's last answer
     * @throws IllegalArgumentException if the token is too long
     */
    public AnnounceMessage(@NotNull TopicKey topic, @NotNull PeerCard card, byte @NotNull [] token) {
        if (token.length > RegisterMessage.MAXIMUM_TOKEN_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The token length {0} is more than {1}.", token.length, RegisterMessage.MAXIMUM_TOKEN_LENGTH);
        }

        this.topic = topic;
        this.card  = card;
        this.token = token.clone();
    }

    /**
     * @return {@link MessageKind#ANNOUNCE}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.ANNOUNCE;
    }

    /**
     * @return the topic
     */
    public @NotNull TopicKey getTopic() {
        return this.topic;
    }

    /**
     * @return the sender's card
     */
    public @NotNull PeerCard getCard() {
        return this.card;
    }

    /**
     * @return a copy of the address token
     */
    public byte @NotNull [] getToken() {
        return this.token.clone();
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        this.topic.write(writer);
        this.card.write(writer);
        writer.writeUnsigned8(this.token.length);
        writer.writeBytes(this.token);
    }
}
