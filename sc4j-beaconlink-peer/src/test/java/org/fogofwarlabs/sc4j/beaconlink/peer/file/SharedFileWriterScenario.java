package org.fogofwarlabs.sc4j.beaconlink.peer.file;

import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.PeerStore;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a process that writes a shared peers file for {@link SharedFileTest}: {@code merge <file> <network>} merges
 * {@value #PEERS_PER_ROUND} new peers of the network {@code 10.<network>.0.0/16} in each of {@value #ROUNDS} rounds and ends;
 * {@code rewrite <file> <network>} records the same {@value #REWRITTEN_PEERS} peers with a new time and merges them again until it is killed.
 */
final class SharedFileWriterScenario {
    /**
     * The mode that merges new peers in rounds.
     */
    static final @NotNull String MERGE = "merge";
    /**
     * The mode that rewrites the file until the process is killed.
     */
    static final @NotNull String REWRITE = "rewrite";
    /**
     * The most peers a file keeps in these processes: more than both merging processes add together.
     */
    static final int MAXIMUM_PEERS = 1000;
    /**
     * The rounds of the merge mode.
     */
    static final int ROUNDS = 50;
    /**
     * The new peers of each round of the merge mode.
     */
    static final int PEERS_PER_ROUND = 6;
    /**
     * The peers of the rewrite mode.
     */
    static final int REWRITTEN_PEERS = 256;
    /**
     * The time of the first record, in epoch milliseconds.
     */
    static final long START_MILLIS = 1_800_000_000_000L;

    private static final int PORT = 7455;
    private static final int HOSTS_PER_SUBNET = 250;

    public static void main(@NotNull String @NotNull [] arguments) {
        Path file = Path.of(arguments[1]);
        int network = Integer.parseInt(arguments[2]);
        SecureRandom random = new SecureRandom();

        if (MERGE.equals(arguments[0])) {
            PeerStore store = PeerStore.open(file, MAXIMUM_PEERS);

            for (int round = 0; round < ROUNDS; round++) {
                List<NodeEndpoint> peers = new ArrayList<>(PEERS_PER_ROUND);

                for (int host = 1; host <= PEERS_PER_ROUND; host++) {
                    peers.add(createPeer(random, network, round, host));
                }

                store.recordSuccess(peers, START_MILLIS + round);
                store.merge(START_MILLIS + round);
            }
        } else {
            PeerStore store = PeerStore.open(file, MAXIMUM_PEERS);
            List<NodeEndpoint> peers = new ArrayList<>(REWRITTEN_PEERS);

            for (int index = 0; index < REWRITTEN_PEERS; index++) {
                peers.add(createPeer(random, network, index / HOSTS_PER_SUBNET, index % HOSTS_PER_SUBNET + 1));
            }

            for (long nowMillis = START_MILLIS; ; nowMillis++) {
                store.recordSuccess(peers, nowMillis);
                store.merge(nowMillis);
            }
        }
    }

    private static @NotNull NodeEndpoint createPeer(@NotNull SecureRandom random, int network, int subnet, int host) {
        InetAddress address = InetAddress.ofLiteral("10." + network + "." + subnet + "." + host);

        return new NodeEndpoint(IdentityKeyPair.generate(random).getPublicKey(), new InetSocketAddress(address, PORT));
    }


    private SharedFileWriterScenario() {
    }
}
