package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads, writes and measures the home peers a signed record names: the peers a player is registered at, through which other players reach
 * it (a room's host for a join, a player for a whisper). On the wire: the count (one byte, at most {@value #MAXIMUM_COUNT}) and the entries,
 * each a {@link NodeEndpoint}.
 */
public final class HomePeerListUtils {
    /**
     * The largest number of home peers of one record.
     */
    public static final int MAXIMUM_COUNT = 3;
    /**
     * The largest length of a list's wire form: the count and the longest entries (IPv6 addresses).
     */
    public static final int MAXIMUM_ENCODED_LENGTH = 1 + MAXIMUM_COUNT * NodeEndpoint.MAXIMUM_ENCODED_LENGTH;

    /**
     * Copies and checks a list of home peers.
     *
     * @param homePeers the home peers
     * @return an unmodifiable copy of the list
     * @throws IllegalArgumentException if the list has more than {@value #MAXIMUM_COUNT} entries
     */
    public static @NotNull List<@NotNull NodeEndpoint> copy(@NotNull List<@NotNull NodeEndpoint> homePeers) {
        if (homePeers.size() > MAXIMUM_COUNT) {
            throw Exceptions.newIllegalArgumentException("A record names {0} home peers, more than {1}.", homePeers.size(), MAXIMUM_COUNT);
        }

        return List.copyOf(homePeers);
    }

    /**
     * Reads a list: the count and the entries.
     *
     * @param reader the reader
     * @return the home peers, or {@code null} if the reader failed or the count is too large
     */
    public static @Nullable List<@NotNull NodeEndpoint> read(@NotNull WireReader reader) {
        int count = reader.readUnsigned8();

        if (count > MAXIMUM_COUNT) {
            reader.fail();

            return null;
        }

        List<NodeEndpoint> homePeers = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            NodeEndpoint homePeer = NodeEndpoint.read(reader);

            if (homePeer == null) {
                reader.fail();

                return null;
            }

            homePeers.add(homePeer);
        }

        return reader.isFailed() ? null : List.copyOf(homePeers);
    }

    /**
     * Writes a list: the count and the entries.
     *
     * @param writer    the writer
     * @param homePeers the home peers, at most {@value #MAXIMUM_COUNT}
     */
    public static void write(@NotNull WireWriter writer, @NotNull List<@NotNull NodeEndpoint> homePeers) {
        writer.writeUnsigned8(homePeers.size());

        for (NodeEndpoint homePeer : homePeers) {
            homePeer.write(writer);
        }
    }


    private HomePeerListUtils() {
    }
}
