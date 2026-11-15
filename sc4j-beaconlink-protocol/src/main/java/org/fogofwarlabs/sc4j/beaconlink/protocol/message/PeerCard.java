package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.CoordinateReport;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Says what a peer tells about itself in its {@link HelloMessage}, {@link FindPeersMessage} and {@link DialBackMessage}: its proof nonce (with
 * the envelope's sender key it gives the peer's {@link PeerId}), whether its reachability self-test proved that strangers reach it, and its
 * network coordinate with its error ({@link CoordinateReport}), with which the receiver of an answer updates its own coordinate. Only such
 * full peers enter routing tables and node lists; a leaf says it is not reachable.
 * <p>
 * On the wire: the proof nonce (8), the flags (1: bit 0 reachable) and the coordinate report ({@value CoordinateReport#LENGTH}).
 */
public final class PeerCard {
    /**
     * The length of the wire form.
     */
    public static final int LENGTH = Long.BYTES + 1 + CoordinateReport.LENGTH;

    private static final int REACHABLE_FLAG = 1;

    /**
     * Reads a card.
     *
     * @param reader the reader
     * @return the card, or {@code null} if the reader failed or an unknown flag is set
     */
    public static @Nullable PeerCard read(@NotNull WireReader reader) {
        long proofNonce = reader.readInt64();
        int flags = reader.readUnsigned8();
        CoordinateReport coordinate = CoordinateReport.read(reader);

        if ((flags & ~REACHABLE_FLAG) != 0) {
            reader.fail();
        }

        return coordinate == null || reader.isFailed() ? null : new PeerCard(proofNonce, (flags & REACHABLE_FLAG) != 0, coordinate);
    }


    private final long proofNonce;
    private final boolean reachable;
    private final @NotNull CoordinateReport coordinate;

    /**
     * Creates a {@link PeerCard} instance.
     *
     * @param proofNonce the peer's proof nonce
     * @param reachable  whether the peer's self-test proved that strangers reach it
     * @param coordinate the peer's network coordinate and its error
     */
    public PeerCard(long proofNonce, boolean reachable, @NotNull CoordinateReport coordinate) {
        this.proofNonce = proofNonce;
        this.reachable  = reachable;
        this.coordinate = coordinate;
    }

    /**
     * @return the peer's proof nonce
     */
    public long getProofNonce() {
        return this.proofNonce;
    }

    /**
     * @return {@code true} if the peer says that strangers reach it (a full peer), {@code false} otherwise (a leaf)
     */
    public boolean isReachable() {
        return this.reachable;
    }

    /**
     * @return the peer's network coordinate and its error
     */
    public @NotNull CoordinateReport getCoordinate() {
        return this.coordinate;
    }

    /**
     * Checks the proof of the card's peer.
     *
     * @param key        the peer's key, the envelope's sender
     * @param difficulty the proof difficulty of the network
     * @return {@code true} if the proof nonce is valid for the key, {@code false} otherwise
     */
    public boolean isProofValid(@NotNull IdentityKey key, int difficulty) {
        return PeerId.isProofValid(key, this.proofNonce, difficulty);
    }


    /**
     * Writes the card.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        writer.writeInt64(this.proofNonce);
        writer.writeUnsigned8(this.reachable ? REACHABLE_FLAG : 0);
        this.coordinate.write(writer);
    }

    /**
     * @return the proof nonce and the reachable flag, for logs
     */
    @Override
    public @NotNull String toString() {
        return "proof " + Long.toUnsignedString(this.proofNonce) + (this.reachable ? ", reachable" : ", leaf");
    }
}
