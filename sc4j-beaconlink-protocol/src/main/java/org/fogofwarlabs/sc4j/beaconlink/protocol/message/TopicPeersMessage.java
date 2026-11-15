package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Answers a {@link GetTopicMessage}: an address token for the asker, whether the answering peer is a member of the topic itself, and at most
 * {@value #MAXIMUM_COUNT} other members. Every entry carries its proof nonce; the receiver checks it before it uses the entry.
 * <p>
 * Body: the topic key, the token (length byte, at most {@value RegisterMessage#MAXIMUM_TOKEN_LENGTH} bytes), the member flag (one byte,
 * {@code 0} or {@code 1}), the count (one byte) and the {@link TopicMember} entries.
 */
public final class TopicPeersMessage extends Message {
    /**
     * The largest number of entries.
     */
    public static final int MAXIMUM_COUNT = 16;
    /**
     * The longest datagram of the kind: the longest token and {@value #MAXIMUM_COUNT} entries with IPv6 addresses. A {@link GetTopicMessage} is
     * padded to it.
     */
    public static final int MAXIMUM_DATAGRAM_LENGTH = Envelope.MINIMUM_DATAGRAM_LENGTH + TopicKey.LENGTH + 1 + RegisterMessage.MAXIMUM_TOKEN_LENGTH +
                                                      1 + 1 + MAXIMUM_COUNT * TopicMember.MAXIMUM_ENCODED_LENGTH;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or a field is out of range
     */
    public static @Nullable TopicPeersMessage read(@NotNull WireReader reader) {
        TopicKey topic = TopicKey.read(reader);
        int tokenLength = reader.readUnsigned8();

        if (tokenLength > RegisterMessage.MAXIMUM_TOKEN_LENGTH) {
            reader.fail();
        }

        byte[] token = reader.readBytes(tokenLength);
        int memberFlag = reader.readUnsigned8();
        int count = reader.readUnsigned8();

        if (topic == null || memberFlag > 1 || count > MAXIMUM_COUNT || reader.isFailed()) {
            return null;
        }

        List<TopicMember> members = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            TopicMember member = TopicMember.read(reader);

            if (member == null) {
                return null;
            }

            members.add(member);
        }

        return new TopicPeersMessage(topic, token, memberFlag == 1, members);
    }


    private final @NotNull TopicKey topic;
    private final byte @NotNull [] token;
    private final boolean member;
    private final @NotNull List<@NotNull TopicMember> members;

    /**
     * Creates a {@link TopicPeersMessage} instance.
     *
     * @param topic   the topic
     * @param token   the address token of the asker
     * @param member  whether the answering peer is a member of the topic
     * @param members the other members
     * @throws IllegalArgumentException if the token is too long or there are more than {@value #MAXIMUM_COUNT} entries
     */
    public TopicPeersMessage(@NotNull TopicKey topic, byte @NotNull [] token, boolean member, @NotNull List<@NotNull TopicMember> members) {
        if (token.length > RegisterMessage.MAXIMUM_TOKEN_LENGTH || members.size() > MAXIMUM_COUNT) {
            throw Exceptions.newIllegalArgumentException("The token length {0} or the {1} member entries are out of range.",
                                                         token.length, members.size());
        }

        this.topic   = topic;
        this.token   = token.clone();
        this.member  = member;
        this.members = List.copyOf(members);
    }

    /**
     * @return {@link MessageKind#TOPIC_PEERS}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.TOPIC_PEERS;
    }

    /**
     * @return the topic
     */
    public @NotNull TopicKey getTopic() {
        return this.topic;
    }

    /**
     * @return a copy of the address token of the asker
     */
    public byte @NotNull [] getToken() {
        return this.token.clone();
    }

    /**
     * @return {@code true} if the answering peer is a member of the topic, {@code false} otherwise
     */
    public boolean isMember() {
        return this.member;
    }

    /**
     * @return the other members, unmodifiable
     */
    public @NotNull List<@NotNull TopicMember> getMembers() {
        return this.members;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        this.topic.write(writer);
        writer.writeUnsigned8(this.token.length);
        writer.writeBytes(this.token);
        writer.writeUnsigned8(this.member ? 1 : 0);
        writer.writeUnsigned8(this.members.size());

        for (TopicMember entry : this.members) {
            entry.write(writer);
        }
    }
}
