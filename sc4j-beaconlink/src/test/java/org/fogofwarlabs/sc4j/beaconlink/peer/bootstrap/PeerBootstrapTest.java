package org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap;

import org.fogofwarlabs.sc4j.beaconlink.peer.MeshPeer;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.PeerNetworkScenario;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tests how peers behind emulated NATs join the network: a later start connects through the learned peers while the seed is down, and a
 * first start without any peer, or with a seed that is down, tells why it is not connected.
 */
final class PeerBootstrapTest {
    private static final long ROUND_MILLIS = 1_000L;
    private static final long DEAD_NODE_MILLIS = 600_000L;
    private static final long CONNECT_TIMEOUT_MILLIS = 5_000L;
    private static final int MAXIMUM_ROUNDS = 60;
    private static final long PROBE_TIMEOUT_MILLIS = 3_000L;
    private static final long CONTACT_MEMORY_MILLIS = 5_000L;
    private static final int IDLE_ROUNDS = 10;
    private static final @NotNull String LOOPBACK_HOST = "127.0.0.1";

    private static @NotNull ConfiguredNode toSeed(@NotNull InetSocketAddress address) {
        return ConfiguredNode.of(LOOPBACK_HOST, address.getPort(), null);
    }

    private static @NotNull NodeEndpoint toEndpoint(@NotNull PeerSocket socket) throws IOException {
        return new NodeEndpoint(socket.getPeer().getKey(), socket.getLocalAddress());
    }

    private static boolean isInState(@NotNull PeerBootstrap bootstrap, @NotNull PeerNetworkScenario network, @NotNull BootstrapState state) {
        bootstrap.update(network.getClock().getEpochMillis());

        return bootstrap.getState() == state;
    }

    private final @NotNull PeerConfiguration configuration;

    PeerBootstrapTest() {
        this.configuration = new PeerConfiguration.Builder().setGossipIntervalMillis(ROUND_MILLIS)
                                                            .setDeadNodeMillis(DEAD_NODE_MILLIS)
                                                            .setDialBackRetryMillis(ROUND_MILLIS)
                                                            .setProbeTimeoutMillis(PROBE_TIMEOUT_MILLIS)
                                                            .setContactMemoryMillis(CONTACT_MEMORY_MILLIS)
                                                            .build();
    }

    @Test
    void a_later_start_connects_through_learned_peers_while_the_seed_is_down(@TempDir @NotNull Path directory) throws IOException {
        Path file = directory.resolve(PeerStore.FILE_NAME);

        try (PeerNetworkScenario network = new PeerNetworkScenario()) {
            PeerSocket seed = network.openOutsidePeer(this.configuration);
            PeerSocket other = network.openOutsidePeer(this.configuration);
            ConfiguredNode seedEntry = toSeed(seed.getLocalAddress());
            NodeEndpoint seedEndpoint = toEndpoint(seed);
            NodeEndpoint otherEndpoint = toEndpoint(other);

            other.getPeer().addSeed(seed.getLocalAddress());
            network.awaitRounds(() -> seed.getPeer().getLiveNodes().size() == 1 && other.getPeer().getLiveNodes().size() == 1, ROUND_MILLIS,
                                MAXIMUM_ROUNDS, "the seed and the other peer prove each other");

            PeerNetworkScenario.NatPeer first = network.openNatPeer(NatType.PORT_RESTRICTED, this.configuration);
            MeshPeer firstPeer = first.getPeer();
            PeerBootstrap firstStart = new PeerBootstrap(firstPeer, KnownNodes.open(List.of(seedEntry), file), CONNECT_TIMEOUT_MILLIS);

            firstStart.start(network.getClock().getEpochMillis());
            network.awaitRounds(() -> isInState(firstStart, network, BootstrapState.CONNECTED) && firstPeer.getLiveNodes().contains(otherEndpoint),
                                ROUND_MILLIS, MAXIMUM_ROUNDS, "the first start connects and learns the other peer from the seed");
            firstStart.close(network.getClock().getEpochMillis());
            network.stopNatPeer(first);
            network.stopOutsidePeer(seed);

            Assertions.assertEquals(Set.of(seedEndpoint, otherEndpoint),
                                    PeerStore.open(file).getPeers().stream().map(LearnedPeer::getEndpoint).collect(Collectors.toSet()),
                                    "the seed's first answer and the other peer are learned");

            PeerNetworkScenario.NatPeer second = network.openNatPeer(NatType.PORT_RESTRICTED, this.configuration);
            MeshPeer secondPeer = second.getPeer();
            PeerBootstrap secondStart = new PeerBootstrap(secondPeer, KnownNodes.open(List.of(seedEntry), file), CONNECT_TIMEOUT_MILLIS);

            secondStart.start(network.getClock().getEpochMillis());

            Assertions.assertEquals(BootstrapState.CONNECTING, secondStart.getState());

            network.awaitRounds(() -> isInState(secondStart, network, BootstrapState.CONNECTED), ROUND_MILLIS, MAXIMUM_ROUNDS,
                                "the second start connects without the seed");
            secondStart.close(network.getClock().getEpochMillis());

            Assertions.assertEquals(List.of(otherEndpoint), secondPeer.getLiveNodes());

            List<LearnedPeer> learned = PeerStore.open(file).getPeers();

            Assertions.assertEquals(1, learned.stream().filter(peer -> peer.getEndpoint().equals(seedEndpoint)).findFirst().orElseThrow()
                                              .getFailedStarts(), "the seed that is down counts one failed start");
            Assertions.assertEquals(0, learned.stream().filter(peer -> peer.getEndpoint().equals(otherEndpoint)).findFirst().orElseThrow()
                                              .getFailedStarts());
        }
    }

    @Test
    void a_first_start_without_a_reachable_peer_tells_why(@TempDir @NotNull Path directory) throws IOException {
        try (PeerNetworkScenario network = new PeerNetworkScenario()) {
            PeerNetworkScenario.NatPeer lonely = network.openNatPeer(NatType.FULL_CONE, this.configuration);
            PeerBootstrap noSeed = new PeerBootstrap(lonely.getPeer(), KnownNodes.open(List.of(), directory.resolve("lonely.json")),
                                                     CONNECT_TIMEOUT_MILLIS);

            noSeed.start(network.getClock().getEpochMillis());
            network.runRounds(ROUND_MILLIS, IDLE_ROUNDS);
            noSeed.update(network.getClock().getEpochMillis());

            Assertions.assertEquals(BootstrapState.NO_PEERS_KNOWN, noSeed.getState());
            Assertions.assertEquals(0, noSeed.getKnownCount());

            PeerSocket gone = network.openOutsidePeer(this.configuration);
            ConfiguredNode goneEntry = toSeed(gone.getLocalAddress());

            network.stopOutsidePeer(gone);

            PeerNetworkScenario.NatPeer hopeful = network.openNatPeer(NatType.FULL_CONE, this.configuration);
            PeerBootstrap seedDown = new PeerBootstrap(hopeful.getPeer(), KnownNodes.open(List.of(goneEntry), directory.resolve("hopeful.json")),
                                                       CONNECT_TIMEOUT_MILLIS);

            seedDown.start(network.getClock().getEpochMillis());

            Assertions.assertEquals(BootstrapState.CONNECTING, seedDown.getState());
            Assertions.assertEquals(1, seedDown.getKnownCount());

            network.awaitRounds(() -> isInState(seedDown, network, BootstrapState.NO_PEER_REACHABLE), ROUND_MILLIS, MAXIMUM_ROUNDS,
                                "the start reports that no peer is reachable");

            Assertions.assertTrue(hopeful.getPeer().getLiveNodes().isEmpty());
        }
    }
}
