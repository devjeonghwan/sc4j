package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.RecordId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Asks the sender of an {@link IHaveMessage} for records of a topic by their {@link RecordId}; the records it still holds come back in
 * {@link GossipMessage}s. Never answered otherwise.
 * <p>
 * Body: the topic key and the IDs (count byte, {@code 1} to {@value #MAXIMUM_COUNT}, and the IDs).
 */
public final class IWantMessage extends Message {
    /**
     * The largest number of IDs.
     */
    public static final int MAXIMUM_COUNT = RecordIdListUtils.MAXIMUM_COUNT;

    private static final int MINIMUM_COUNT = 1;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or the count is out of range
     */
    public static @Nullable IWantMessage read(@NotNull WireReader reader) {
        TopicKey topic = TopicKey.read(reader);
        List<RecordId> ids = RecordIdListUtils.read(reader, MINIMUM_COUNT);

        return topic == null || ids == null ? null : new IWantMessage(topic, ids);
    }


    private final @NotNull TopicKey topic;
    private final @NotNull List<@NotNull RecordId> ids;

    /**
     * Creates an {@link IWantMessage} instance.
     *
     * @param topic the topic
     * @param ids   the IDs of the wanted records
     * @throws IllegalArgumentException if the list is empty or has more than {@value #MAXIMUM_COUNT} IDs
     */
    public IWantMessage(@NotNull TopicKey topic, @NotNull List<@NotNull RecordId> ids) {
        this.topic = topic;
        this.ids   = RecordIdListUtils.copy(ids, MINIMUM_COUNT);
    }

    /**
     * @return {@link MessageKind#IWANT}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.IWANT;
    }

    /**
     * @return the topic
     */
    public @NotNull TopicKey getTopic() {
        return this.topic;
    }

    /**
     * @return the IDs of the wanted records, unmodifiable
     */
    public @NotNull List<@NotNull RecordId> getIds() {
        return this.ids;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        this.topic.write(writer);
        RecordIdListUtils.write(writer, this.ids);
    }
}
