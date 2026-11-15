package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.fogofwarlabs.sc4j.engine.net.lobby.JoinRefusal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Refuses an offer to join a room: the host does not hold the room, or its join check failed with a {@link JoinRefusal} (another engine
 * version, another mod identity, a full room). It repeats the initiator's receive number, so that the initiator knows which offer is refused.
 * The refusal goes back through the node the offer came through.
 * <p>
 * Fields after the sender's connection: the initiator's receive number (eight bytes) and the reason (one byte): {@code 0} for a room the host does
 * not hold, otherwise the code of the {@link JoinRefusal}.
 */
public final class ChannelRefusal extends ChannelSignal {
    private static final int ROOM_NOT_FOUND_CODE = 0;

    /**
     * Reads the fields after the kind and the sender's connection.
     *
     * @param reader           the reader
     * @param senderConnection the connection number of the sender's session
     * @return the refusal, or {@code null} if the reader failed or the reason is unknown
     */
    static @Nullable ChannelRefusal read(@NotNull WireReader reader, long senderConnection) {
        long offerNumber = reader.readInt64();
        int code = reader.readUnsigned8();
        JoinRefusal refusal = JoinRefusal.find(code);

        if (reader.isFailed() || code != ROOM_NOT_FOUND_CODE && refusal == null) {
            return null;
        }

        return new ChannelRefusal(senderConnection, offerNumber, refusal);
    }


    private final long offerNumber;
    private final @Nullable JoinRefusal refusal;

    /**
     * Creates a {@link ChannelRefusal} instance.
     *
     * @param senderConnection the connection number of the host's session
     * @param offerNumber      the initiator's receive number from the refused offer
     * @param refusal          the reason of the failed join check, or {@code null} if the host does not hold the room
     */
    public ChannelRefusal(long senderConnection, long offerNumber, @Nullable JoinRefusal refusal) {
        super(senderConnection);

        this.offerNumber = offerNumber;
        this.refusal     = refusal;
    }

    /**
     * @return {@link ChannelSignalKind#REFUSAL}
     */
    @Override
    public @NotNull ChannelSignalKind getKind() {
        return ChannelSignalKind.REFUSAL;
    }

    /**
     * @return the initiator's receive number from the refused offer
     */
    public long getOfferNumber() {
        return this.offerNumber;
    }

    /**
     * @return the reason of the failed join check, or {@code null} if the host does not hold the room
     */
    public @Nullable JoinRefusal getJoinRefusal() {
        return this.refusal;
    }


    /**
     * Writes the kind's fields.
     *
     * @param writer the writer
     */
    @Override
    protected void writeFields(@NotNull WireWriter writer) {
        JoinRefusal reason = this.refusal;

        writer.writeInt64(this.offerNumber);
        writer.writeUnsigned8(reason == null ? ROOM_NOT_FOUND_CODE : reason.getCode());
    }
}
