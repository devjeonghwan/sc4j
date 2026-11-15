package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomAdvertisement;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Publishes or updates a room at a node. The sender must be the room's host and registered at the node.
 * <p>
 * Body: one {@link RoomAdvertisement} without the withdrawn flag.
 */
public final class PublishRoomMessage extends Message {
    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or the advertisement is a withdrawal
     */
    public static @Nullable PublishRoomMessage read(@NotNull WireReader reader) {
        RoomAdvertisement advertisement = RoomAdvertisement.read(reader);

        if (advertisement == null || advertisement.isWithdrawn()) {
            return null;
        }

        return new PublishRoomMessage(advertisement);
    }


    private final @NotNull RoomAdvertisement advertisement;

    /**
     * Creates a {@link PublishRoomMessage} instance.
     *
     * @param advertisement the room's advertisement
     * @throws IllegalArgumentException if the advertisement is a withdrawal
     */
    public PublishRoomMessage(@NotNull RoomAdvertisement advertisement) {
        if (advertisement.isWithdrawn()) {
            throw Exceptions.newIllegalArgumentException("The advertisement {0} withdraws its room.", advertisement);
        }

        this.advertisement = advertisement;
    }

    /**
     * @return {@link MessageKind#PUBLISH_ROOM}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.PUBLISH_ROOM;
    }

    /**
     * @return the room's advertisement
     */
    public @NotNull RoomAdvertisement getAdvertisement() {
        return this.advertisement;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        this.advertisement.write(writer);
    }
}
