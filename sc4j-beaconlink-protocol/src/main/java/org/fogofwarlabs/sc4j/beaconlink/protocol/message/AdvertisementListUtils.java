package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomAdvertisement;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads, writes and measures the advertisement lists of {@link RoomListMessage}: the count (one byte) and the
 * advertisements, within the byte budget of one datagram.
 */
final class AdvertisementListUtils {
    /**
     * The largest number of advertisements in one list.
     */
    static final int MAXIMUM_COUNT = 0xff;

    /**
     * Copies and checks a list of advertisements.
     *
     * @param advertisements the advertisements
     * @param maximumBytes   the largest total wire length of the advertisements
     * @return an unmodifiable copy of the list
     * @throws IllegalArgumentException if the list has too many advertisements or too many bytes
     */
    static @NotNull List<@NotNull RoomAdvertisement> copy(@NotNull List<@NotNull RoomAdvertisement> advertisements, int maximumBytes) {
        int bytes = measure(advertisements);

        if (advertisements.size() > MAXIMUM_COUNT || bytes > maximumBytes) {
            throw Exceptions.newIllegalArgumentException("{0} advertisements of {1} bytes do not fit in {2} bytes.",
                                                         advertisements.size(), bytes, maximumBytes);
        }

        return List.copyOf(advertisements);
    }

    /**
     * Measures a list of advertisements.
     *
     * @param advertisements the advertisements
     * @return the total wire length of the advertisements, without the count byte
     */
    static int measure(@NotNull List<@NotNull RoomAdvertisement> advertisements) {
        int bytes = 0;

        for (RoomAdvertisement advertisement : advertisements) {
            bytes += advertisement.getEncodedLength();
        }

        return bytes;
    }

    /**
     * Reads a list: the count and the advertisements.
     *
     * @param reader the reader
     * @return the advertisements, or {@code null} if the reader failed
     */
    static @Nullable List<@NotNull RoomAdvertisement> read(@NotNull WireReader reader) {
        int count = reader.readUnsigned8();
        List<RoomAdvertisement> advertisements = new ArrayList<>(count);

        for (int index = 0; index < count && !reader.isFailed(); index++) {
            RoomAdvertisement advertisement = RoomAdvertisement.read(reader);

            if (advertisement != null) {
                advertisements.add(advertisement);
            }
        }

        return reader.isFailed() ? null : List.copyOf(advertisements);
    }

    /**
     * Writes a list: the count and the advertisements.
     *
     * @param writer         the writer
     * @param advertisements the advertisements
     */
    static void write(@NotNull WireWriter writer, @NotNull List<@NotNull RoomAdvertisement> advertisements) {
        writer.writeUnsigned8(advertisements.size());

        for (RoomAdvertisement advertisement : advertisements) {
            advertisement.write(writer);
        }
    }


    private AdvertisementListUtils() {
    }
}
