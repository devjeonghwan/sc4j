package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.RecordKind;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Publishes a client's own record at its home peer, which takes it into the record's topic (answered with {@link ResultMessage}): a presence,
 * a chat line or a sign-out. The sender must be registered at the peer and be the record's author; rooms have their own
 * {@link PublishRoomMessage}.
 * <p>
 * Body: one record with its kind code ({@link RecordKind#PRESENCE}, {@link RecordKind#CHAT_LINE} or {@link RecordKind#SIGN_OUT}).
 */
public final class PublishRecordMessage extends Message {
    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or the record is a room advertisement
     */
    public static @Nullable PublishRecordMessage read(@NotNull WireReader reader) {
        TopicRecord record = RecordListUtils.readRecord(reader);

        if (record == null || record.getRecordKind() == RecordKind.ROOM) {
            return null;
        }

        return new PublishRecordMessage(record);
    }


    private final @NotNull TopicRecord record;

    /**
     * Creates a {@link PublishRecordMessage} instance.
     *
     * @param record the record: a presence, a chat line or a sign-out
     * @throws IllegalArgumentException if the record is a room advertisement
     */
    public PublishRecordMessage(@NotNull TopicRecord record) {
        if (record.getRecordKind() == RecordKind.ROOM) {
            throw Exceptions.newIllegalArgumentException("The room advertisement {0} is published with a room message.", record);
        }

        this.record = record;
    }

    /**
     * @return {@link MessageKind#PUBLISH_RECORD}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.PUBLISH_RECORD;
    }

    /**
     * @return the record
     */
    public @NotNull TopicRecord getRecord() {
        return this.record;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        RecordListUtils.writeRecord(writer, this.record);
    }
}
