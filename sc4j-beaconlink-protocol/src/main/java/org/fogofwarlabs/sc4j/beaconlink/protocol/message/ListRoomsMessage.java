package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lists the rooms of one mod topic at a node, one page at a time: the rooms of every version of the mod, which the client tells apart by their
 * fingerprints. The sender must be registered at the node; the node takes part in the topic for its clients.
 * <p>
 * Body: the topic key and the offset of the first room of the page (two bytes) in the node's stable room order.
 */
public final class ListRoomsMessage extends Message {
    /**
     * The largest page offset.
     */
    public static final int MAXIMUM_OFFSET = 0xffff;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed
     */
    public static @Nullable ListRoomsMessage read(@NotNull WireReader reader) {
        TopicKey topic = TopicKey.read(reader);
        int offset = reader.readUnsigned16();

        if (topic == null || reader.isFailed()) {
            return null;
        }

        return new ListRoomsMessage(topic, offset);
    }


    private final @NotNull TopicKey topic;
    private final int offset;

    /**
     * Creates a {@link ListRoomsMessage} instance.
     *
     * @param topic  the topic of the rooms to list
     * @param offset the offset of the first room of the page
     * @throws IllegalArgumentException if the offset is out of range
     */
    public ListRoomsMessage(@NotNull TopicKey topic, int offset) {
        if (offset < 0 || offset > MAXIMUM_OFFSET) {
            throw Exceptions.newIllegalArgumentException("The offset {0} is outside 0..{1}.", offset, MAXIMUM_OFFSET);
        }

        this.topic  = topic;
        this.offset = offset;
    }

    /**
     * @return {@link MessageKind#LIST_ROOMS}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.LIST_ROOMS;
    }

    /**
     * @return the topic of the rooms to list
     */
    public @NotNull TopicKey getTopic() {
        return this.topic;
    }

    /**
     * @return the offset of the first room of the page
     */
    public int getOffset() {
        return this.offset;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        this.topic.write(writer);
        writer.writeUnsigned16(this.offset);
    }
}
