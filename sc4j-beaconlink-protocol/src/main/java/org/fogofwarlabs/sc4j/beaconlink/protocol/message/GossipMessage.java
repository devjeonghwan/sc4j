package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Carries records of a topic: pushed by a member to its mesh neighbours when they are new, sent to answer an {@link IWantMessage}, or pushed
 * by a home peer to its registered clients that subscribed to the topic ({@link RecentMessage}). The records are room advertisements,
 * presences, chat lines and sign-outs of the topic, each signed by its author; the receiver checks every one before it keeps or forwards it.
 * Never answered.
 * <p>
 * Body: the topic key and the records (count byte and records with their kind codes, at most {@value #MAXIMUM_RECORD_BYTES} bytes).
 */
public final class GossipMessage extends Message {
    /**
     * The largest total wire length of the records of one message, kind codes included.
     */
    public static final int MAXIMUM_RECORD_BYTES = Envelope.MAXIMUM_BODY_LENGTH - TopicKey.LENGTH - 1;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed
     */
    public static @Nullable GossipMessage read(@NotNull WireReader reader) {
        TopicKey topic = TopicKey.read(reader);
        List<TopicRecord> records = RecordListUtils.read(reader);

        if (topic == null || records == null || RecordListUtils.measure(records) > MAXIMUM_RECORD_BYTES) {
            return null;
        }

        return new GossipMessage(topic, records);
    }

    /**
     * Measures a record as a part of a message.
     *
     * @param record the record
     * @return the bytes the record takes in a message: its kind code and its wire form
     */
    public static int measure(@NotNull TopicRecord record) {
        return 1 + record.getEncodedLength();
    }


    private final @NotNull TopicKey topic;
    private final @NotNull List<@NotNull TopicRecord> records;

    /**
     * Creates a {@link GossipMessage} instance.
     *
     * @param topic   the topic
     * @param records the records
     * @throws IllegalArgumentException if the records do not fit in one datagram
     */
    public GossipMessage(@NotNull TopicKey topic, @NotNull List<? extends @NotNull TopicRecord> records) {
        this.topic   = topic;
        this.records = RecordListUtils.copy(records, MAXIMUM_RECORD_BYTES);
    }

    /**
     * @return {@link MessageKind#GOSSIP}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.GOSSIP;
    }

    /**
     * @return the topic
     */
    public @NotNull TopicKey getTopic() {
        return this.topic;
    }

    /**
     * @return the records, unmodifiable
     */
    public @NotNull List<@NotNull TopicRecord> getRecords() {
        return this.records;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        this.topic.write(writer);
        RecordListUtils.write(writer, this.records);
    }
}
