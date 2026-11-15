package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.RecordId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads, writes and checks the record ID lists of {@link IHaveMessage} and {@link IWantMessage}: the count (one byte, at most
 * {@value #MAXIMUM_COUNT}) and the IDs. An {@code IHAVE} may name no ID (it keeps a mesh link alive); an {@code IWANT} names at least one.
 */
final class RecordIdListUtils {
    /**
     * The largest number of IDs in one list.
     */
    static final int MAXIMUM_COUNT = 32;

    /**
     * Copies and checks a list of IDs.
     *
     * @param ids          the IDs
     * @param minimumCount the smallest number of IDs
     * @return an unmodifiable copy of the list
     * @throws IllegalArgumentException if the list has fewer than {@code minimumCount} or more than {@value #MAXIMUM_COUNT} IDs
     */
    static @NotNull List<@NotNull RecordId> copy(@NotNull List<@NotNull RecordId> ids, int minimumCount) {
        if (ids.size() < minimumCount || ids.size() > MAXIMUM_COUNT) {
            throw Exceptions.newIllegalArgumentException("A record ID list has {0}..{1} IDs, not {2}.", minimumCount, MAXIMUM_COUNT, ids.size());
        }

        return List.copyOf(ids);
    }

    /**
     * Reads a list: the count and the IDs.
     *
     * @param reader       the reader
     * @param minimumCount the smallest number of IDs
     * @return the IDs, or {@code null} if the reader failed or the count is out of range
     */
    static @Nullable List<@NotNull RecordId> read(@NotNull WireReader reader, int minimumCount) {
        int count = reader.readUnsigned8();

        if (count < minimumCount || count > MAXIMUM_COUNT) {
            reader.fail();

            return null;
        }

        List<RecordId> ids = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            RecordId id = RecordId.read(reader);

            if (id == null) {
                return null;
            }

            ids.add(id);
        }

        return List.copyOf(ids);
    }

    /**
     * Writes a list: the count and the IDs.
     *
     * @param writer the writer
     * @param ids    the IDs
     */
    static void write(@NotNull WireWriter writer, @NotNull List<@NotNull RecordId> ids) {
        writer.writeUnsigned8(ids.size());

        for (RecordId id : ids) {
            id.write(writer);
        }
    }


    private RecordIdListUtils() {
    }
}
