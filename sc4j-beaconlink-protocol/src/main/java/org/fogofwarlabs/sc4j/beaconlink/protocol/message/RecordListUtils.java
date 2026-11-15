package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.community.ChatLineRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.community.PresenceRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.community.SignOutRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomAdvertisement;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.RecordKind;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads, writes and measures the records of a topic where records of several kinds travel together ({@link GossipMessage},
 * {@link PublishRecordMessage}): each record is its kind code (one byte, {@link RecordKind}) and its wire form. A list is the count (one byte)
 * and the records, within the byte budget of one datagram.
 */
final class RecordListUtils {
    /**
     * The largest number of records in one list.
     */
    static final int MAXIMUM_COUNT = 0xff;

    /**
     * Copies and checks a list of records.
     *
     * @param records      the records
     * @param maximumBytes the largest total wire length of the records, kind codes included
     * @return an unmodifiable copy of the list
     * @throws IllegalArgumentException if the list has too many records or too many bytes
     */
    static @NotNull List<@NotNull TopicRecord> copy(@NotNull List<? extends @NotNull TopicRecord> records, int maximumBytes) {
        int bytes = measure(records);

        if (records.size() > MAXIMUM_COUNT || bytes > maximumBytes) {
            throw Exceptions.newIllegalArgumentException("{0} records of {1} bytes do not fit in {2} bytes.", records.size(), bytes, maximumBytes);
        }

        return List.copyOf(records);
    }

    /**
     * Measures a list of records.
     *
     * @param records the records
     * @return the total wire length of the records with their kind codes, without the count byte
     */
    static int measure(@NotNull List<? extends @NotNull TopicRecord> records) {
        int bytes = 0;

        for (TopicRecord record : records) {
            bytes += 1 + record.getEncodedLength();
        }

        return bytes;
    }

    /**
     * Reads one record: its kind code and its wire form.
     *
     * @param reader the reader
     * @return the record, or {@code null} if the reader failed or the kind is unknown
     */
    static @Nullable TopicRecord readRecord(@NotNull WireReader reader) {
        RecordKind kind = RecordKind.find(reader.readUnsigned8());

        if (kind == null) {
            reader.fail();

            return null;
        }

        return switch (kind) {
            case ROOM -> RoomAdvertisement.read(reader);
            case PRESENCE -> PresenceRecord.read(reader);
            case CHAT_LINE -> ChatLineRecord.read(reader);
            case SIGN_OUT -> SignOutRecord.read(reader);
        };
    }

    /**
     * Writes one record: its kind code and its wire form.
     *
     * @param writer the writer
     * @param record the record
     */
    static void writeRecord(@NotNull WireWriter writer, @NotNull TopicRecord record) {
        writer.writeUnsigned8(record.getRecordKind().getCode());
        record.write(writer);
    }

    /**
     * Reads a list: the count and the records.
     *
     * @param reader the reader
     * @return the records, or {@code null} if the reader failed or a record is unknown or malformed
     */
    static @Nullable List<@NotNull TopicRecord> read(@NotNull WireReader reader) {
        int count = reader.readUnsigned8();
        List<TopicRecord> records = new ArrayList<>(count);

        for (int index = 0; index < count && !reader.isFailed(); index++) {
            TopicRecord record = readRecord(reader);

            if (record != null) {
                records.add(record);
            }
        }

        return reader.isFailed() ? null : List.copyOf(records);
    }

    /**
     * Writes a list: the count and the records.
     *
     * @param writer  the writer
     * @param records the records
     */
    static void write(@NotNull WireWriter writer, @NotNull List<@NotNull TopicRecord> records) {
        writer.writeUnsigned8(records.size());

        for (TopicRecord record : records) {
            writeRecord(writer, record);
        }
    }


    private RecordListUtils() {
    }
}
