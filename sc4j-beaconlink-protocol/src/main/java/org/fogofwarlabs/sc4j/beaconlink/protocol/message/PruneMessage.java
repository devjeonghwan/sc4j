package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tells a mesh neighbour that the sender removed it from its mesh of a topic: the sender left the topic or holds more neighbours than it
 * keeps. Never answered.
 * <p>
 * Body: the topic key.
 */
public final class PruneMessage extends Message {
    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed
     */
    public static @Nullable PruneMessage read(@NotNull WireReader reader) {
        TopicKey topic = TopicKey.read(reader);

        return topic == null ? null : new PruneMessage(topic);
    }


    private final @NotNull TopicKey topic;

    /**
     * Creates a {@link PruneMessage} instance.
     *
     * @param topic the topic
     */
    public PruneMessage(@NotNull TopicKey topic) {
        this.topic = topic;
    }

    /**
     * @return {@link MessageKind#PRUNE}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.PRUNE;
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
