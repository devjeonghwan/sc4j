package org.fogofwarlabs.sc4j.beaconlink.peer;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.peer.reachability.Reachability;
import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.CoordinateReport;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.*;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.ModFingerprint;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomAdvertisement;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomDescription;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKind;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

/**
 * Tests the peer layer of the protocol on 127.0.0.1: peers prove each other with hellos, peers that join through one seed test their reachability
 * through each other and fill their routing tables with lookups, rooms published at one peer are listed (and withdrawn) at the others through the
 * swarm of their mod topic, lookup requests are answered with the full peers only when padded, hellos without a valid proof are dropped, and a peer
 * that stops answering is dropped.
 */
final class PeerRoutingTest {
    private static final @NotNull ModFingerprint FINGERPRINT = ModFingerprint.of(new byte[]{3, 1, 4, 1, 5});
    private static final @NotNull TopicKey TOPIC = TopicKey.of(TopicKind.MOD, "pi");
    private static final int PROOF_DIFFICULTY = 8;
    private static final long ROUND_MILLIS = 1_000L;
    private static final long DEAD_NODE_MILLIS = 4_000L;
    private static final long CONTACT_MEMORY_MILLIS = 2_000L;
    private static final int MAXIMUM_ROUNDS = 60;
    private static final long ROOM_LIFETIME_MILLIS = 300_000L;
    private static final int CAPACITY = 2;
    private static final int STRANGERS = 6;
    private static final int POLLS_PER_ROUND = 10;
    private static final long POLL_WAIT_MILLIS = 2L;
    private static final @NotNull String SOFTWARE = "stranger";

    private static @NotNull PeerConfiguration.Builder createConfiguration() {
        return PeerScenario.createNodeBuilder().setProofDifficulty(PROOF_DIFFICULTY)
                           .setGossipIntervalMillis(ROUND_MILLIS)
                           .setDeadNodeMillis(DEAD_NODE_MILLIS)
                           .setDialBackRetryMillis(ROUND_MILLIS)
                           .setDialBackIntervalMillis(ROUND_MILLIS)
                           .setContactMemoryMillis(CONTACT_MEMORY_MILLIS)
                           .setLookupRequestTimeoutMillis(ROUND_MILLIS);
    }

    private static boolean knows(@NotNull PeerSocket node, @NotNull PeerSocket other) {
        for (NodeEndpoint endpoint : node.getPeer().getLiveNodes()) {
            if (endpoint.getKey().equals(other.getPeer().getKey())) {
                return true;
            }
        }

        return false;
    }

    private static @NotNull Set<@NotNull IdentityKey> getFullPeerKeys(@NotNull PeerSocket node) {
        return node.getPeer().getFullPeers().stream().map(PeerContact::getKey).collect(Collectors.toSet());
    }

    private static long findInvalidNonce(@NotNull IdentityKey key) {
        long nonce = 0L;

        while (PeerId.isProofValid(key, nonce, PROOF_DIFFICULTY)) {
            nonce++;
        }

        return nonce;
    }

    private static int countRooms(@NotNull PeerScenario.Client client, @NotNull PeerSocket node) {
        try {
            return ((RoomListMessage) client.request(new ListRoomsMessage(TOPIC, 0), node, false).getBody()).getTotal();
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }
    }

    private final @NotNull PeerScenario scenario;
    private final @NotNull PeerConfiguration configuration;

    PeerRoutingTest() {
        this.scenario      = new PeerScenario();
        this.configuration = createConfiguration().build();
    }

    @AfterEach
    void closeScenario() {
        this.scenario.close();
    }

    private @NotNull RoomAdvertisement publish(@NotNull PeerScenario.Client host, @NotNull PeerSocket node) throws IOException {
        RoomDescription description = new RoomDescription(FINGERPRINT, "Mesh room", "Ashen Ford", 1, CAPACITY);
        RoomAdvertisement room = RoomAdvertisement.signPublication(host.getKeyPair(), PeerScenario.DEFAULT_CONNECTION, TOPIC, 1L, 1L,
                                                                   this.scenario.getClock().getEpochMillis() + ROOM_LIFETIME_MILLIS, description,
                                                                   List.of(), null);

        host.register(node, FINGERPRINT);
        Assertions.assertEquals(ResultStatus.ACCEPTED,
                                ((ResultMessage) host.request(new PublishRoomMessage(room), node, false).getBody()).getStatus());

        return room;
    }

    @Test
    void two_peers_prove_each_other_and_share_rooms_through_their_topic() throws IOException {
        PeerSocket first = this.scenario.openNode(this.configuration);
        PeerSocket second = this.scenario.openNode(this.configuration);
        PeerScenario.Client host = this.scenario.openClient();
        PeerScenario.Client player = this.scenario.openClient();

        second.getPeer().addSeed(first.getLocalAddress());
        this.scenario.awaitRounds(() -> knows(first, second) && knows(second, first), ROUND_MILLIS, MAXIMUM_ROUNDS, "both peers proved");

        Assertions.assertEquals(Reachability.REACHABLE, first.getPeer().getReachability(), "the second peer's hello proves the seed");
        Assertions.assertEquals(Set.of(first.getPeer().getKey()), getFullPeerKeys(second));
        Assertions.assertEquals(Set.of(), getFullPeerKeys(first), "nobody could test the second peer: it stays out of the tables");

        RoomAdvertisement room = this.publish(host, first);

        player.register(second, FINGERPRINT);
        this.scenario.awaitRounds(() -> countRooms(player, second) == 1, ROUND_MILLIS, MAXIMUM_ROUNDS, "the room reaches the second peer");

        RoomListMessage listed = (RoomListMessage) player.request(new ListRoomsMessage(TOPIC, 0), second, false).getBody();

        Assertions.assertEquals(List.of(room), listed.getAdvertisements());

        RoomAdvertisement withdrawal = RoomAdvertisement.signWithdrawal(host.getKeyPair(), PeerScenario.DEFAULT_CONNECTION, TOPIC, 1L, 2L,
                                                                        room.getExpiryMillis(), room.getDescription(), List.of());

        host.request(new WithdrawRoomMessage(withdrawal), first, false);
        this.scenario.awaitRounds(() -> countRooms(player, second) == 0, ROUND_MILLIS, MAXIMUM_ROUNDS, "the withdrawal reaches the second peer");
    }

    @Test
    void peers_joining_through_one_seed_prove_their_reachability_and_fill_their_tables() throws IOException {
        PeerSocket seed = this.scenario.openNode(this.configuration);
        PeerSocket second = this.scenario.openNode(this.configuration);
        PeerSocket third = this.scenario.openNode(this.configuration);
        PeerScenario.Client host = this.scenario.openClient();
        PeerScenario.Client player = this.scenario.openClient();
        List<PeerSocket> peers = List.of(seed, second, third);

        // A joiner that probed another one for its test cannot take that one's probe as a proof until its contact memory forgot the probe;
        // the short memory lets the three peers test each other.
        second.getPeer().addSeed(seed.getLocalAddress());
        third.getPeer().addSeed(seed.getLocalAddress());
        this.scenario.awaitRounds(() -> peers.stream().allMatch(peer -> getFullPeerKeys(peer).size() == 2), ROUND_MILLIS, MAXIMUM_ROUNDS,
                                  "every peer has the two others in its routing table");

        for (PeerSocket peer : peers) {
            Assertions.assertEquals(Reachability.REACHABLE, peer.getPeer().getReachability());
            Assertions.assertEquals(0L, peer.getPeer().getStatistics().getDroppedCount(DropReason.FORGED));
            Assertions.assertEquals(0L, peer.getPeer().getStatistics().getDroppedCount(DropReason.INVALID_PROOF));
        }

        this.publish(host, third);
        player.register(second, FINGERPRINT);
        this.scenario.awaitRounds(() -> countRooms(player, second) == 1, ROUND_MILLIS, MAXIMUM_ROUNDS, "the room reaches the second peer");
    }

    @Test
    void find_peers_is_answered_with_the_full_peers_only_when_padded() throws IOException {
        PeerSocket first = this.scenario.openNode(this.configuration);
        PeerSocket second = this.scenario.openNode(this.configuration);
        PeerScenario.Client client = this.scenario.openClient();
        FindPeersMessage find = new FindPeersMessage(PeerId.createRandom(new SplittableRandom(1L)), null);

        second.getPeer().addSeed(first.getLocalAddress());
        this.scenario.awaitRounds(() -> getFullPeerKeys(second).size() == 1, ROUND_MILLIS, MAXIMUM_ROUNDS, "the seed enters the table");

        PeersMessage sample = (PeersMessage) client.request(find, second, true).getBody();
        PeerContact seed = sample.getPeers().getFirst();

        Assertions.assertEquals(1, sample.getPeers().size());
        Assertions.assertEquals(first.getPeer().getKey(), seed.getKey());
        Assertions.assertEquals(first.getLocalAddress(), seed.getAddress());
        Assertions.assertEquals(first.getPeer().getId(), seed.getId());
        Assertions.assertTrue(seed.isProofValid(PROOF_DIFFICULTY));
        Assertions.assertTrue(((PeersMessage) client.request(find, first, true).getBody()).getPeers().isEmpty(),
                              "the second peer is a leaf to the seed");

        client.send(client.getEncoder().encodeRequest(find, client.takeRequestNumber()), second);
        this.scenario.await(() -> second.getPeer().getStatistics().getDroppedCount(DropReason.UNPADDED) == 1L, "the unpadded request is dropped");

        Assertions.assertEquals(List.of(), client.drain());
    }

    @Test
    void hello_is_answered_only_when_padded_and_with_a_valid_proof() throws IOException {
        PeerSocket node = this.scenario.openNode(this.configuration);
        PeerScenario.Client stranger = this.scenario.openClient();
        IdentityKey key = stranger.getKey();
        HelloMessage hello = new HelloMessage(new PeerCard(PeerId.solveProof(key, PROOF_DIFFICULTY), false, CoordinateReport.UNKNOWN), 0, SOFTWARE);
        HelloMessage forged = new HelloMessage(new PeerCard(findInvalidNonce(key), true, CoordinateReport.UNKNOWN), 0, SOFTWARE);

        stranger.send(stranger.getEncoder().encodeRequest(hello, stranger.takeRequestNumber()), node);
        this.scenario.await(() -> node.getPeer().getStatistics().getDroppedCount(DropReason.UNPADDED) == 1L, "the unpadded hello is dropped");
        stranger.send(stranger.getEncoder().encodePaddedRequest(forged, stranger.takeRequestNumber()), node);
        this.scenario.await(() -> node.getPeer().getStatistics().getDroppedCount(DropReason.INVALID_PROOF) == 1L, "the forged proof is dropped");
        Assertions.assertTrue(stranger.drain().isEmpty());

        HelloMessage answer = (HelloMessage) stranger.request(hello, node, true).getBody();

        Assertions.assertTrue(answer.getCard().isProofValid(node.getPeer().getKey(), PROOF_DIFFICULTY));
        Assertions.assertEquals(HelloMessage.ROUTING_CAPABILITY | HelloMessage.TOPIC_CAPABILITY, answer.getCapabilities());
        Assertions.assertEquals(MeshPeer.SOFTWARE, answer.getSoftware());
    }

    @Test
    void hellos_of_unknown_keys_do_not_keep_a_seed_out() throws IOException {
        PeerConfiguration small = createConfiguration().setMaximumLeafContacts(CAPACITY).setMaximumCandidates(CAPACITY).build();
        PeerSocket first = this.scenario.openNode(small);
        PeerSocket second = this.scenario.openNode(this.configuration);
        PeerScenario.Client stranger = this.scenario.openClient();

        first.getPeer().addSeed(second.getLocalAddress());

        // Hellos of fresh keys arrive between every hello of the first peer and the seed's answer, so a table shared with the candidates would
        // always be full of them when the answer comes.
        for (int round = 0; round < MAXIMUM_ROUNDS && !knows(first, second); round++) {
            this.scenario.getClock().advance(ROUND_MILLIS);

            for (int poll = 0; poll < POLLS_PER_ROUND; poll++) {
                first.poll(0L);
                this.sendStrangerHellos(stranger, first);
                second.poll(POLL_WAIT_MILLIS);
            }
        }

        Assertions.assertTrue(knows(first, second), "the seed is proved");
    }

    private void sendStrangerHellos(@NotNull PeerScenario.Client stranger, @NotNull PeerSocket node) {
        try {
            for (int count = 0; count < STRANGERS; count++) {
                IdentityKeyPair keyPair = IdentityKeyPair.generate(new SecureRandom());
                MessageEncoder encoder = new MessageEncoder(keyPair, this.scenario.getClock(), new SplittableRandom(count));
                PeerCard card = new PeerCard(PeerId.solveProof(keyPair.getPublicKey(), PROOF_DIFFICULTY), true, CoordinateReport.UNKNOWN);

                stranger.send(encoder.encodePaddedRequest(new HelloMessage(card, 0, SOFTWARE), stranger.takeRequestNumber()), node);
            }
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }
    }

    @Test
    void a_peer_that_stops_answering_is_dropped() throws IOException {
        PeerSocket first = this.scenario.openNode(this.configuration);
        PeerSocket second = this.scenario.openNode(this.configuration);
        IdentityKey secondKey = second.getPeer().getKey();

        second.getPeer().addSeed(first.getLocalAddress());
        this.scenario.awaitRounds(() -> knows(first, second) && knows(second, first), ROUND_MILLIS, MAXIMUM_ROUNDS, "both peers proved");
        this.scenario.closeNode(second);
        this.scenario.awaitRounds(() -> first.getPeer().getLiveNodes().isEmpty(), ROUND_MILLIS, MAXIMUM_ROUNDS, "the closed peer is dropped");

        Assertions.assertTrue(first.getPeer().getLiveNodes().stream().noneMatch(endpoint -> endpoint.getKey().equals(secondKey)));
    }
}
