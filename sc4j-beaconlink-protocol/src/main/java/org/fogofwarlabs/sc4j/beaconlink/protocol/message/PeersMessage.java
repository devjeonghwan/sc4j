package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.CoordinateReport;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Answers a {@link FindPeersMessage}: the full peers of the answering peer's routing table that are closest to the target, at most
 * {@value #MAXIMUM_COUNT}. Every entry carries its proof nonce; the receiver checks it before it uses the entry. The answer also carries the
 * answering peer's own network coordinate with its error, with which the asking peer or client updates its coordinate from the round trip
 * time of the request.
 * <p>
 * Body: the answering peer's {@link CoordinateReport}, the count (one byte) and the {@link PeerContact} entries.
 */
public final class PeersMessage extends Message {
    /**
     * The largest number of entries: the bucket size of a routing table.
     */
    public static final int MAXIMUM_COUNT = 8;
    /**
     * The longest datagram of the kind: {@value #MAXIMUM_COUNT} entries with IPv6 addresses. A {@link FindPeersMessage} is padded to it.
     */
    public static final int MAXIMUM_DATAGRAM_LENGTH = Envelope.MINIMUM_DATAGRAM_LENGTH + CoordinateReport.LENGTH + 1 +
                                                      MAXIMUM_COUNT * PeerContact.MAXIMUM_ENCODED_LENGTH;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or there are too many entries
     */
    public static @Nullable PeersMessage read(@NotNull WireReader reader) {
        CoordinateReport coordinate = CoordinateReport.read(reader);
        int count = reader.readUnsigned8();

        if (coordinate == null || count > MAXIMUM_COUNT) {
            return null;
        }

        List<PeerContact> peers = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            PeerContact peer = PeerContact.read(reader);

            if (peer == null) {
                return null;
            }

            peers.add(peer);
        }

        return new PeersMessage(coordinate, peers);
    }


    private final @NotNull CoordinateReport coordinate;
    private final @NotNull List<@NotNull PeerContact> peers;

    /**
     * Creates a {@link PeersMessage} instance.
     *
     * @param coordinate the answering peer's network coordinate and its error
     * @param peers      the entries, closest first
     * @throws IllegalArgumentException if there are more than {@value #MAXIMUM_COUNT} entries
     */
    public PeersMessage(@NotNull CoordinateReport coordinate, @NotNull List<@NotNull PeerContact> peers) {
        if (peers.size() > MAXIMUM_COUNT) {
            throw Exceptions.newIllegalArgumentException("{0} peer entries are more than {1}.", peers.size(), MAXIMUM_COUNT);
        }

        this.coordinate = coordinate;
        this.peers      = List.copyOf(peers);
    }

    /**
     * @return {@link MessageKind#PEERS}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.PEERS;
    }

    /**
     * @return the answering peer's network coordinate and its error
     */
    public @NotNull CoordinateReport getCoordinate() {
        return this.coordinate;
    }

    /**
     * @return the entries, unmodifiable
     */
    public @NotNull List<@NotNull PeerContact> getPeers() {
        return this.peers;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        this.coordinate.write(writer);
        writer.writeUnsigned8(this.peers.size());

        for (PeerContact peer : this.peers) {
            peer.write(writer);
        }
    }
}
