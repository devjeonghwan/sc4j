package org.fogofwarlabs.sc4j.beaconlink.peer.reachability;

import org.fogofwarlabs.sc4j.beaconlink.peer.MeshPeer;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.MessageKind;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.PeerNetworkScenario;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;

/**
 * Tests the reachability self-test of peers on the loopback NAT emulator: a peer behind a full-cone NAT passes the dial-back and becomes a
 * full peer, a peer behind any other NAT fails it and stays a leaf, a peer outside every NAT passes, and the very first peer, which has no
 * helper, is proved reachable by the first unrequested hello.
 */
final class ReachabilityCheckTest {
    private static final long ROUND_MILLIS = 1_000L;
    private static final long DEAD_NODE_MILLIS = 600_000L;
    private static final long PROBE_TIMEOUT_MILLIS = 3_000L;
    private static final long DIAL_BACK_RETRY_MILLIS = 2_000L;
    private static final long DIAL_BACK_INTERVAL_MILLIS = 600_000L;
    private static final int MAXIMUM_ROUNDS = 30;
    private static final int IDLE_ROUNDS = 5;

    private final @NotNull PeerConfiguration configuration;

    ReachabilityCheckTest() {
        this.configuration = new PeerConfiguration.Builder().setGossipIntervalMillis(ROUND_MILLIS)
                                                            .setDeadNodeMillis(DEAD_NODE_MILLIS)
                                                            .setProbeTimeoutMillis(PROBE_TIMEOUT_MILLIS)
                                                            .setDialBackRetryMillis(DIAL_BACK_RETRY_MILLIS)
                                                            .setDialBackIntervalMillis(DIAL_BACK_INTERVAL_MILLIS)
                                                            .build();
    }

    /**
     * Opens a helper and a third peer outside every NAT and lets them federate.
     */
    private @NotNull PeerSocket openHelper(@NotNull PeerNetworkScenario network) throws IOException {
        PeerSocket helper = network.openOutsidePeer(this.configuration);
        PeerSocket third = network.openOutsidePeer(this.configuration);

        third.getPeer().addSeed(helper.getLocalAddress());
        network.awaitRounds(() -> helper.getPeer().getLiveNodes().size() == 1 && third.getPeer().getLiveNodes().size() == 1, ROUND_MILLIS,
                            MAXIMUM_ROUNDS, "the helper and the third peer federate");

        return helper;
    }

    @ParameterizedTest
    @EnumSource(NatType.class)
    void each_nat_type_gets_its_role_from_the_dial_back(@NotNull NatType type) throws IOException {
        try (PeerNetworkScenario network = new PeerNetworkScenario()) {
            PeerSocket helper = this.openHelper(network);
            PeerNetworkScenario.NatPeer requester = network.openNatPeer(type, this.configuration);
            MeshPeer peer = requester.getPeer();
            boolean fullCone = type == NatType.FULL_CONE;

            peer.addSeed(helper.getLocalAddress());
            network.awaitRounds(() -> peer.getReachability() != Reachability.NOT_VERIFIED, ROUND_MILLIS, MAXIMUM_ROUNDS,
                                "the self-test of a peer behind " + type);

            Assertions.assertEquals(fullCone ? Reachability.REACHABLE : Reachability.NOT_REACHABLE, peer.getReachability());
            Assertions.assertEquals(fullCone, peer.getReachability().isFullPeer());
            Assertions.assertEquals(requester.getNat().getPublicAddresses().getFirst(), peer.getObservedAddress());

            if (fullCone) {
                Assertions.assertEquals(1L, peer.getStatistics().getHandledCount(MessageKind.PROBE), "the probe proved the reachability");
            } else {
                Assertions.assertEquals(0L, peer.getStatistics().getHandledCount(MessageKind.PROBE));
                Assertions.assertTrue(requester.getNat().getFilteredCount() > 0, "the NAT dropped the probe");
            }
        }
    }

    @Test
    void a_peer_outside_every_nat_is_a_full_peer() throws IOException {
        try (PeerNetworkScenario network = new PeerNetworkScenario()) {
            PeerSocket helper = this.openHelper(network);
            PeerSocket requester = network.openOutsidePeer(this.configuration);
            MeshPeer peer = requester.getPeer();

            peer.addSeed(helper.getLocalAddress());
            network.awaitRounds(() -> peer.getReachability() != Reachability.NOT_VERIFIED, ROUND_MILLIS, MAXIMUM_ROUNDS,
                                "the self-test of a peer outside every NAT");

            Assertions.assertEquals(Reachability.REACHABLE, peer.getReachability());
            Assertions.assertEquals(requester.getLocalAddress(), peer.getObservedAddress());
        }
    }

    @Test
    void the_first_unrequested_hello_proves_the_reachability_of_the_first_peer() throws IOException {
        try (PeerNetworkScenario network = new PeerNetworkScenario()) {
            PeerSocket first = network.openOutsidePeer(this.configuration);

            network.runRounds(ROUND_MILLIS, IDLE_ROUNDS);

            Assertions.assertEquals(Reachability.NOT_VERIFIED, first.getPeer().getReachability(), "nobody can test the first peer");

            PeerSocket newcomer = network.openOutsidePeer(this.configuration);

            newcomer.getPeer().addSeed(first.getLocalAddress());
            network.awaitRounds(() -> first.getPeer().getReachability() == Reachability.REACHABLE, ROUND_MILLIS, MAXIMUM_ROUNDS,
                                "the first peer is proved reachable");
            network.runRounds(ROUND_MILLIS, IDLE_ROUNDS);

            Assertions.assertNull(first.getPeer().getObservedAddress(), "no dial-back proved it");
            Assertions.assertEquals(Reachability.NOT_VERIFIED, newcomer.getPeer().getReachability(), "the first peer knows no third peer");
        }
    }
}
