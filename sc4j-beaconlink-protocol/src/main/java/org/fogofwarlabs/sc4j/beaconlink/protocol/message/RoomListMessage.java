package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomAdvertisement;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Answers a room listing with one page of rooms.
 * <p>
 * Body: the number of listed rooms the node holds for the topic (two bytes), the offset of the page (two bytes), and the page's
 * advertisements (count byte and advertisements, at most {@value #MAXIMUM_ADVERTISEMENT_BYTES} bytes).
 */
public final class RoomListMessage extends Message {
    /**
     * The largest total wire length of the advertisements of one page.
     */
    public static final int MAXIMUM_ADVERTISEMENT_BYTES = Envelope.MAXIMUM_BODY_LENGTH - 2 * Short.BYTES - 1;

    private static final int MAXIMUM_COUNT_VALUE = 0xffff;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed
     */
    public static @Nullable RoomListMessage read(@NotNull WireReader reader) {
        int total = reader.readUnsigned16();
        int offset = reader.readUnsigned16();
        List<RoomAdvertisement> advertisements = AdvertisementListUtils.read(reader);

        if (advertisements == null || AdvertisementListUtils.measure(advertisements) > MAXIMUM_ADVERTISEMENT_BYTES) {
            return null;
        }

        return new RoomListMessage(total, offset, advertisements);
    }


    private final int total;
    private final int offset;
    private final @NotNull List<@NotNull RoomAdvertisement> advertisements;

    /**
     * Creates a {@link RoomListMessage} instance.
     *
     * @param total          the number of listed rooms the node holds for the topic
     * @param offset         the offset of the page
     * @param advertisements the page's advertisements
     * @throws IllegalArgumentException if a number is out of range or the page does not fit in one datagram
     */
    public RoomListMessage(int total, int offset, @NotNull List<@NotNull RoomAdvertisement> advertisements) {
        if (total < 0 || total > MAXIMUM_COUNT_VALUE || offset < 0 || offset > MAXIMUM_COUNT_VALUE) {
            throw Exceptions.newIllegalArgumentException("The total {0} or the offset {1} is out of range.", total, offset);
        }

        this.total          = total;
        this.offset         = offset;
        this.advertisements = AdvertisementListUtils.copy(advertisements, MAXIMUM_ADVERTISEMENT_BYTES);
    }

    /**
     * @return {@link MessageKind#ROOM_LIST}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.ROOM_LIST;
    }

    /**
     * @return the number of listed rooms the node holds for the topic
     */
    public int getTotal() {
        return this.total;
    }

    /**
     * @return the offset of the page
     */
    public int getOffset() {
        return this.offset;
    }

    /**
     * @return the page's advertisements, unmodifiable
     */
    public @NotNull List<@NotNull RoomAdvertisement> getAdvertisements() {
        return this.advertisements;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        writer.writeUnsigned16(this.total);
        writer.writeUnsigned16(this.offset);
        AdvertisementListUtils.write(writer, this.advertisements);
    }
}
