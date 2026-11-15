package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomAdvertisement;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Withdraws a room at a node. The sender must be the room's host and registered at the node.
 * <p>
 * Body: one {@link RoomAdvertisement} with the withdrawn flag.
 */
public final class WithdrawRoomMessage extends Message {
    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or the advertisement is not a withdrawal
     */
    public static @Nullable WithdrawRoomMessage read(@NotNull WireReader reader) {
        RoomAdvertisement advertisement = RoomAdvertisement.read(reader);

        if (advertisement == null || !advertisement.isWithdrawn()) {
            return null;
        }

        return new WithdrawRoomMessage(advertisement);
    }


    private final @NotNull RoomAdvertisement advertisement;

    /**
     * Creates a {@link WithdrawRoomMessage} instance.
     *
     * @param advertisement the room's withdrawal
     * @throws IllegalArgumentException if the advertisement is not a withdrawal
     */
    public WithdrawRoomMessage(@NotNull RoomAdvertisement advertisement) {
        if (!advertisement.isWithdrawn()) {
            throw Exceptions.newIllegalArgumentException("The advertisement {0} does not withdraw its room.", advertisement);
        }

        this.advertisement = advertisement;
    }

    /**
     * @return {@link MessageKind#WITHDRAW_ROOM}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.WITHDRAW_ROOM;
    }

    /**
     * @return the room's withdrawal
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
