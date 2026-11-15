package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Asks a peer for the full peers of its routing table closest to a target ID (answered by {@link PeersMessage}): one step of an iterative
 * lookup, or a client asking its node for other nodes. The request is padded to {@link PeersMessage#MAXIMUM_DATAGRAM_LENGTH}, so that the
 * answer to an address that has not proved itself is never longer than the request.
 * <p>
 * A peer sends its {@link PeerCard}, so that the receiver may verify it and take it into its table; a client sends none.
 * <p>
 * Body: the target (32), the card flag (1: {@code 0} none, {@code 1} a card follows) and the card.
 */
public final class FindPeersMessage extends Message {
    private static final int NO_CARD = 0;
    private static final int WITH_CARD = 1;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed
     */
    public static @Nullable FindPeersMessage read(@NotNull WireReader reader) {
        PeerId target = PeerId.read(reader);
        int flag = reader.readUnsigned8();
        PeerCard card = null;

        if (flag == WITH_CARD) {
            card = PeerCard.read(reader);

            if (card == null) {
                return null;
            }
        } else if (flag != NO_CARD) {
            reader.fail();
        }

        return target == null || reader.isFailed() ? null : new FindPeersMessage(target, card);
    }


    private final @NotNull PeerId target;
    private final @Nullable PeerCard card;

    /**
     * Creates a {@link FindPeersMessage} instance.
     *
     * @param target the target ID
     * @param card   the sending peer's card, or {@code null} for a client
     */
    public FindPeersMessage(@NotNull PeerId target, @Nullable PeerCard card) {
        this.target = target;
        this.card   = card;
    }

    /**
     * @return {@link MessageKind#FIND_PEERS}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.FIND_PEERS;
    }

    /**
     * @return the target ID
     */
    public @NotNull PeerId getTarget() {
        return this.target;
    }

    /**
     * @return the sending peer's card, or {@code null} if a client sent the request
     */
    public @Nullable PeerCard getCard() {
        return this.card;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        this.target.write(writer);

        if (this.card == null) {
            writer.writeUnsigned8(NO_CARD);
        } else {
            writer.writeUnsigned8(WITH_CARD);
            this.card.write(writer);
        }
    }
}
