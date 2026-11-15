package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Subscribes a registered client to a topic at its home peer, or renews the subscription (answered with {@link ResultMessage}, its value the
 * granted lifetime in seconds): the peer takes part in the topic for the client and pushes it every new record of the topic
 * ({@link GossipMessage}) and the IDs of the last ones every round ({@link IHaveMessage}) until the subscription lapses. With the backlog flag,
 * the peer first pushes what it holds: the presences and the last chat lines of the topic.
 * <p>
 * Body: the topic key and the flags (one byte, bit 0 backlog, the other bits zero).
 */
public final class RecentMessage extends Message {
    private static final int BACKLOG_FLAG = 1;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or an unknown flag is set
     */
    public static @Nullable RecentMessage read(@NotNull WireReader reader) {
        TopicKey topic = TopicKey.read(reader);
        int flags = reader.readUnsigned8();

        if (topic == null || (flags & ~BACKLOG_FLAG) != 0 || reader.isFailed()) {
            return null;
        }

        return new RecentMessage(topic, (flags & BACKLOG_FLAG) != 0);
    }


    private final @NotNull TopicKey topic;
    private final boolean backlog;

    /**
     * Creates a {@link RecentMessage} instance.
     *
     * @param topic   the topic
     * @param backlog whether the peer pushes what it holds of the topic first
     */
    public RecentMessage(@NotNull TopicKey topic, boolean backlog) {
        this.topic   = topic;
        this.backlog = backlog;
    }

    /**
     * @return {@link MessageKind#RECENT}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.RECENT;
    }

    /**
     * @return the topic
     */
    public @NotNull TopicKey getTopic() {
        return this.topic;
    }

    /**
     * @return {@code true} if the peer pushes what it holds of the topic first, {@code false} otherwise (a renewal)
     */
    public boolean isBacklog() {
        return this.backlog;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        this.topic.write(writer);
        writer.writeUnsigned8(this.backlog ? BACKLOG_FLAG : 0);
    }
}
