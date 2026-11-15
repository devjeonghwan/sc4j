package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Asks a peer for members of a topic (answered by {@link TopicPeersMessage}): a storing peer of the topic answers from the members that
 * announced themselves to it, a member also from its swarm (peer exchange). The answer carries an address token for the asker, which its
 * {@link AnnounceMessage} and {@link GraftMessage} to that peer must repeat. The request is padded to
 * {@link TopicPeersMessage#MAXIMUM_DATAGRAM_LENGTH}, so that the answer to an address that has not proved itself is never longer than the
 * request.
 * <p>
 * Body: the topic key.
 */
public final class GetTopicMessage extends Message {
    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed
     */
    public static @Nullable GetTopicMessage read(@NotNull WireReader reader) {
        TopicKey topic = TopicKey.read(reader);

        return topic == null ? null : new GetTopicMessage(topic);
    }


    private final @NotNull TopicKey topic;

    /**
     * Creates a {@link GetTopicMessage} instance.
     *
     * @param topic the topic
     */
    public GetTopicMessage(@NotNull TopicKey topic) {
        this.topic = topic;
    }

    /**
     * @return {@link MessageKind#GET_TOPIC}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.GET_TOPIC;
    }

    /**
     * @return the topic
     */
    public @NotNull TopicKey getTopic() {
        return this.topic;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        this.topic.write(writer);
    }
}
