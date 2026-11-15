package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.RecordId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Names records of a topic that the sender holds, by their {@link RecordId}: the lazy half of the gossip. A member sends it to its mesh
 * neighbours and a few other members for the records it took in lately, and to a new mesh neighbour for all its records; the receiver asks
 * for the ones it has not seen with an {@link IWantMessage}. Without IDs it keeps a quiet mesh link and its NAT mappings alive. Never
 * answered.
 * <p>
 * Body: the topic key and the IDs (count byte, {@code 0} to {@value #MAXIMUM_COUNT}, and the IDs).
 */
public final class IHaveMessage extends Message {
    /**
     * The largest number of IDs.
     */
    public static final int MAXIMUM_COUNT = RecordIdListUtils.MAXIMUM_COUNT;

    private static final int MINIMUM_COUNT = 0;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or the count is out of range
     */
    public static @Nullable IHaveMessage read(@NotNull WireReader reader) {
        TopicKey topic = TopicKey.read(reader);
        List<RecordId> ids = RecordIdListUtils.read(reader, MINIMUM_COUNT);

        return topic == null || ids == null ? null : new IHaveMessage(topic, ids);
    }


    private final @NotNull TopicKey topic;
    private final @NotNull List<@NotNull RecordId> ids;

    /**
     * Creates an {@link IHaveMessage} instance.
     *
     * @param topic the topic
     * @param ids   the IDs of the records
     * @throws IllegalArgumentException if the list has more than {@value #MAXIMUM_COUNT} IDs
     */
    public IHaveMessage(@NotNull TopicKey topic, @NotNull List<@NotNull RecordId> ids) {
        this.topic = topic;
        this.ids   = RecordIdListUtils.copy(ids, MINIMUM_COUNT);
    }

    /**
     * @return {@link MessageKind#IHAVE}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.IHAVE;
    }

    /**
     * @return the topic
     */
    public @NotNull TopicKey getTopic() {
        return this.topic;
    }

    /**
     * @return the IDs of the records, unmodifiable
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
