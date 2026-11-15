package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Asks a member of a topic to take the sender into its mesh of the topic (answered by {@link ResultMessage}): mesh neighbours push new
 * records to each other. The token of the member's last {@link TopicPeersMessage} proves that the sender receives at its source address, so
 * that nobody can make a member push records to another host.
 * <p>
 * Body: the topic key, the sender's {@link PeerCard} and the token (length byte, at most {@value RegisterMessage#MAXIMUM_TOKEN_LENGTH} bytes).
 */
public final class GraftMessage extends Message {
    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or the token is too long
     */
    public static @Nullable GraftMessage read(@NotNull WireReader reader) {
        TopicKey topic = TopicKey.read(reader);
        PeerCard card = PeerCard.read(reader);
        int tokenLength = reader.readUnsigned8();

        if (tokenLength > RegisterMessage.MAXIMUM_TOKEN_LENGTH) {
            reader.fail();
        }

        byte[] token = reader.readBytes(tokenLength);

        return topic == null || card == null || reader.isFailed() ? null : new GraftMessage(topic, card, token);
    }


    private final @NotNull TopicKey topic;
    private final @NotNull PeerCard card;
    private final byte @NotNull [] token;

    /**
     * Creates a {@link GraftMessage} instance.
     *
     * @param topic the topic
     * @param card  the sender's card
     * @param token the address token of the member's last answer
     * @throws IllegalArgumentException if the token is too long
     */
    public GraftMessage(@NotNull TopicKey topic, @NotNull PeerCard card, byte @NotNull [] token) {
        if (token.length > RegisterMessage.MAXIMUM_TOKEN_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The token length {0} is more than {1}.", token.length, RegisterMessage.MAXIMUM_TOKEN_LENGTH);
        }

        this.topic = topic;
        this.card  = card;
        this.token = token.clone();
    }

    /**
     * @return {@link MessageKind#GRAFT}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.GRAFT;
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
