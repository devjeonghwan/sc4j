package org.fogofwarlabs.sc4j.beaconlink.peer.topic;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.beaconlink.peer.DropReason;
import org.fogofwarlabs.sc4j.beaconlink.peer.MeshPeer;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.peer.store.RecordOffer;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.MessageKind;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.ModFingerprint;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomAdvertisement;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomDescription;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.test.ManualProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKind;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.LargeSimulation;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.SimulatedNetwork;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.SimulatedPeer;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.TopicFlooder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Tests the topic layer on the in-memory network: a room published through one peer is listed by at least 99 of 100 searches of a thousand
 * peers within ten seconds (searches that ran before the room and searches that start after it), its withdrawal hides it as fast, an expired
 * room is listed nowhere, and a member that floods a mesh with records over their author's rate and forged records is graylisted while honest
 * rooms still spread.
 */
final class TopicSwarmsSimulationTest {
    private static final @NotNull Logger LOGGER = Logger.of(TopicSwarmsSimulationTest.class);
    private static final long RANDOM_SEED = 20261110L;
    private static final int REGIONS = 8;
    private static final double LOSS_RATE = 0.01;
    private static final int PROOF_DIFFICULTY = 8;
    private static final long HOST_CONNECTION = 1L;
    private static final int SEED_COUNT = 3;
    private static final int PEER_COUNT = 1000;
    private static final int LATE_SEARCH_COUNT = 100;
    private static final int FLOOD_PEER_COUNT = 30;
    private static final long JOIN_INTERVAL_MILLIS = 100L;
    private static final long SETTLE_MILLIS = 60_000L;
    private static final long SEARCH_SPREAD_MILLIS = 20_000L;
    private static final long SEARCH_SETTLE_MILLIS = 30_000L;
    private static final long VISIBLE_WITHIN_MILLIS = 10_000L;
    private static final long STEP_MILLIS = 500L;
    private static final long ROOM_LIFETIME_MILLIS = 120_000L;
    private static final long GRAFT_MILLIS = 2_000L;
    private static final double REQUIRED_SHARE = 0.99;
    private static final int CAPACITY = 4;
    private static final int SPAM_ROOMS = 20;
    private static final int FORGED_ROOMS = 2;
    private static final int LATE_FLOOD_ROOMS = 5;
    private static final int FLOOD_MAXIMUM_MESH_DEGREE = 32;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final @NotNull TopicKey TOPIC = TopicKey.of(TopicKind.MOD, "simulated-mod");
    private static final @NotNull ModFingerprint FINGERPRINT = ModFingerprint.of(new byte[]{2, 0, 2, 6});
    /**
     * The NATs of the peers, in turn: half public hosts, one in ten behind a full-cone NAT (full peers too), the rest behind NATs that drop
     * strangers (leaves).
     */
    private static final @Nullable NatType @NotNull [] NAT_MIX = {null, null, null, null, null, NatType.FULL_CONE, NatType.ADDRESS_RESTRICTED,
                                                                  NatType.PORT_RESTRICTED, NatType.PORT_RESTRICTED, NatType.SYMMETRIC};

    /**
     * Starts the searches of the peers spread over a time, then lets the swarm settle.
     */
    private static void startSearches(@NotNull SimulatedNetwork network, @NotNull List<@NotNull SimulatedPeer> searchers) {
        long stepCount = SEARCH_SPREAD_MILLIS / STEP_MILLIS;
        int perStep = (int) Math.ceil((double) searchers.size() / stepCount);

        for (int start = 0; start < searchers.size(); start += perStep) {
            for (SimulatedPeer searcher : searchers.subList(start, Math.min(searchers.size(), start + perStep))) {
                searcher.getPeer().listRooms(TOPIC);
            }

            network.run(STEP_MILLIS);
        }

        network.run(SEARCH_SETTLE_MILLIS);
    }

    private static @NotNull RoomAdvertisement signRoom(@NotNull IdentityKeyPair host, long number, long sequence, long expiryMillis,
                                                       boolean withdrawn) {
        RoomDescription description = new RoomDescription(FINGERPRINT, "Room " + number, "Ashen Ford", 1, CAPACITY);

        return withdrawn ? RoomAdvertisement.signWithdrawal(host, HOST_CONNECTION, TOPIC, number, sequence, expiryMillis, description, List.of())
                         : RoomAdvertisement.signPublication(host, HOST_CONNECTION, TOPIC, number, sequence, expiryMillis, description, List.of(),
                                                             null);
    }

    private static @NotNull RoomAdvertisement forge(@NotNull RoomAdvertisement genuine) {
        WireWriter writer = new WireWriter(RoomAdvertisement.MAXIMUM_ENCODED_LENGTH);

        genuine.write(writer);

        byte[] bytes = writer.toByteArray();

        bytes[bytes.length - 1] ^= 1;

        RoomAdvertisement forged = RoomAdvertisement.read(new WireReader(bytes, 0, bytes.length));

        Assertions.assertNotNull(forged);

        return forged;
    }

    private static boolean lists(@NotNull MeshPeer peer, @NotNull RoomKey key) {
        for (RoomAdvertisement advertisement : peer.listRooms(TOPIC)) {
            if (advertisement.getKey().equals(key)) {
                return true;
            }
        }

        return false;
    }

    private static int countListing(@NotNull List<@NotNull SimulatedPeer> peers, @NotNull RoomKey key) {
        int count = 0;

        for (SimulatedPeer peer : peers) {
            if (lists(peer.getPeer(), key)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Runs the network in steps until every peer of the list shows the wanted state of a room or the time passed.
     *
     * @return the time it took, in milliseconds, or the time limit
     */
    private static long runUntil(@NotNull SimulatedNetwork network, @NotNull List<@NotNull SimulatedPeer> peers, @NotNull RoomKey key, boolean listed,
                                 long maximumMillis) {
        long waited = 0L;

        while (waited < maximumMillis && countListing(peers, key) != (listed ? peers.size() : 0)) {
            network.run(STEP_MILLIS);
            waited += STEP_MILLIS;
        }

        return waited;
    }

    private static @NotNull String countHandled(@NotNull List<@NotNull SimulatedPeer> peers) {
        StringBuilder text = new StringBuilder();

        for (MessageKind kind : List.of(MessageKind.GET_TOPIC, MessageKind.ANNOUNCE, MessageKind.GRAFT, MessageKind.PRUNE, MessageKind.GOSSIP,
                                        MessageKind.IHAVE, MessageKind.IWANT)) {
            long count = 0L;

            for (SimulatedPeer peer : peers) {
                count += peer.getPeer().getStatistics().getHandledCount(kind);
            }

            text.append(' ').append(kind).append('=').append(count);
        }

        return text.toString();
    }

    private static @NotNull SimulatedNetwork createNetwork(@NotNull PeerConfiguration configuration, long seed, int peerCount) {
        SimulatedNetwork network = new SimulatedNetwork(seed, REGIONS, LOSS_RATE);
        List<InetSocketAddress> seeds = new ArrayList<>();

        for (int index = 0; index < SEED_COUNT; index++) {
            seeds.add(network.addPeer(configuration, null, List.copyOf(seeds)).getAddress());
            network.run(JOIN_INTERVAL_MILLIS);
        }

        // Every peer knows one of the seeds, as a release ships one seed.
        for (int index = SEED_COUNT; index < peerCount; index++) {
            network.addPeer(configuration, NAT_MIX[index % NAT_MIX.length], List.of(seeds.get(network.getRandom().nextInt(seeds.size()))));
            network.run(JOIN_INTERVAL_MILLIS);
        }

        network.run(SETTLE_MILLIS);

        return network;
    }

    @Test
    @LargeSimulation
    void a_room_reaches_the_searches_of_a_thousand_peers_within_ten_seconds_until_withdrawn_or_expired() {
        long wallStart = System.nanoTime();
        PeerConfiguration configuration = new PeerConfiguration.Builder().setProofDifficulty(PROOF_DIFFICULTY).build();
        SimulatedNetwork network = createNetwork(configuration, RANDOM_SEED, PEER_COUNT);
        SplittableRandom random = network.getRandom();
        List<SimulatedPeer> peers = network.getRunningPeers();
        List<SimulatedPeer> searchers = new ArrayList<>(peers.subList(0, PEER_COUNT - LATE_SEARCH_COUNT));
        List<SimulatedPeer> lateSearchers = new ArrayList<>(peers.subList(PEER_COUNT - LATE_SEARCH_COUNT, PEER_COUNT));
        long networkSent = network.getSentCount();

        startSearches(network, searchers);

        long swarmSent = network.getSentCount() - networkSent;
        IdentityKeyPair host = network.createKeyPair();
        MeshPeer homePeer = searchers.get(random.nextInt(searchers.size())).getPeer();
        long publishedMillis = network.getClock().getEpochMillis();
        RoomAdvertisement room = signRoom(host, 1L, 1L, publishedMillis + ROOM_LIFETIME_MILLIS, false);

        Assertions.assertEquals(RecordOffer.ACCEPTED, homePeer.publishRoom(room));

        long spreadMillis = runUntil(network, searchers, room.getKey(), true, VISIBLE_WITHIN_MILLIS);
        int listing = countListing(searchers, room.getKey());

        for (SimulatedPeer late : lateSearchers) {
            late.getPeer().listRooms(TOPIC);
        }

        long lateMillis = runUntil(network, lateSearchers, room.getKey(), true, VISIBLE_WITHIN_MILLIS);
        int lateListing = countListing(lateSearchers, room.getKey());
        double visible = (double) (listing + lateListing) / PEER_COUNT;

        Assertions.assertEquals(RecordOffer.ACCEPTED, homePeer.publishRoom(signRoom(host, 1L, 2L, room.getExpiryMillis(), true)));

        long withdrawMillis = runUntil(network, peers, room.getKey(), false, VISIBLE_WITHIN_MILLIS);
        double hidden = 1.0 - (double) countListing(peers, room.getKey()) / PEER_COUNT;
        IdentityKeyPair otherHost = network.createKeyPair();
        MeshPeer otherHomePeer = peers.get(random.nextInt(peers.size())).getPeer();
        RoomAdvertisement expiring = signRoom(otherHost, 1L, 1L, network.getClock().getEpochMillis() + ROOM_LIFETIME_MILLIS, false);

        Assertions.assertEquals(RecordOffer.ACCEPTED, otherHomePeer.publishRoom(expiring));
        runUntil(network, peers, expiring.getKey(), true, VISIBLE_WITHIN_MILLIS);

        double expiringVisible = (double) countListing(peers, expiring.getKey()) / PEER_COUNT;

        network.run(expiring.getExpiryMillis() - network.getClock().getEpochMillis() + STEP_MILLIS);

        int listingExpired = countListing(peers, expiring.getKey());

        LOGGER.info("Topic simulation: {0} peers; swarm of {1} searches built with {2} datagrams; room listed by {3} of {4} searches in {5} ms " +
                    "and by {6} of {7} later searches in {8} ms ({9}); withdrawal hidden at {10} in {11} ms; second room listed by {12}, by {13} " +
                    "after its expiry; {14} datagrams sent, {15} lost, {16} filtered; handled{17}; {18} ms virtual, {19} ms wall", peers.size(),
                    searchers.size(), swarmSent, listing, searchers.size(), spreadMillis, lateListing, lateSearchers.size(), lateMillis, visible,
                    hidden, withdrawMillis, expiringVisible, listingExpired, network.getSentCount(), network.getLostCount(),
                    network.getFilteredCount(), countHandled(peers), network.getClock().getEpochMillis() - ManualProtocolClock.START_MILLIS,
                    (System.nanoTime() - wallStart) / NANOS_PER_MILLI);

        Assertions.assertTrue(visible >= REQUIRED_SHARE, "room listed by " + visible + " of the searches within ten seconds");
        Assertions.assertTrue(hidden >= REQUIRED_SHARE, "withdrawal hidden at " + hidden + " of the peers within ten seconds");
        Assertions.assertTrue(expiringVisible >= REQUIRED_SHARE, "second room listed by " + expiringVisible);
        Assertions.assertEquals(0, listingExpired, "peers listing the expired room");
    }

    @Test
    void a_flooding_member_is_graylisted_and_honest_rooms_still_spread() {
        // The victim takes every graft, so that the flooder finds room in its mesh whatever the other members did.
        PeerConfiguration configuration = new PeerConfiguration.Builder().setProofDifficulty(PROOF_DIFFICULTY)
                                                                         .setMaximumMeshDegree(FLOOD_MAXIMUM_MESH_DEGREE).build();
        SimulatedNetwork network = createNetwork(configuration, RANDOM_SEED + 1L, FLOOD_PEER_COUNT);
        List<SimulatedPeer> peers = network.getRunningPeers();

        startSearches(network, peers);

        SimulatedPeer victim = peers.get(SEED_COUNT);
        TopicFlooder flooder = new TopicFlooder(network, TOPIC, victim.getAddress(), PROOF_DIFFICULTY);
        IdentityKeyPair spammer = network.createKeyPair();
        List<RoomAdvertisement> spam = new ArrayList<>();
        long expiryMillis = network.getClock().getEpochMillis() + ROOM_LIFETIME_MILLIS;

        Assertions.assertNull(victim.getNat(), "the victim is a public host");

        flooder.join();
        network.run(GRAFT_MILLIS);

        Assertions.assertTrue(flooder.isGrafted(), "the flooder joins the victim's mesh as a member would");

        for (int number = 1; number <= SPAM_ROOMS; number++) {
            spam.add(signRoom(spammer, number, 1L, expiryMillis, false));
        }

        for (int number = 1; number <= FORGED_ROOMS; number++) {
            spam.add(forge(signRoom(network.createKeyPair(), number, 1L, expiryMillis, false)));
        }

        flooder.flood(spam);
        network.run(GRAFT_MILLIS);

        int answers = flooder.getAnswerCount();
        List<RoomAdvertisement> lateSpam = new ArrayList<>();

        for (int number = 1; number <= LATE_FLOOD_ROOMS; number++) {
            lateSpam.add(signRoom(network.createKeyPair(), number, 1L, expiryMillis, false));
        }

        flooder.flood(lateSpam);
        flooder.join();
        network.run(GRAFT_MILLIS);

        IdentityKeyPair host = network.createKeyPair();
        RoomAdvertisement honest = signRoom(host, 1L, 1L, network.getClock().getEpochMillis() + ROOM_LIFETIME_MILLIS, false);

        Assertions.assertEquals(RecordOffer.ACCEPTED, peers.get(peers.size() - 1).getPeer().publishRoom(honest));

        long spreadMillis = runUntil(network, peers, honest.getKey(), true, VISIBLE_WITHIN_MILLIS);
        MeshPeer victimPeer = victim.getPeer();
        long droppedRecords = victimPeer.getStatistics().getDroppedCount(DropReason.INVALID_RECORD);
        long graylisted = victimPeer.getStatistics().getDroppedCount(DropReason.GRAYLISTED);

        LOGGER.info("Flood: victim dropped {0} records, {1} graylisted datagrams; honest room listed by {2} of {3} peers in {4} ms",
                    droppedRecords, graylisted, countListing(peers, honest.getKey()), peers.size(), spreadMillis);

        Assertions.assertTrue(droppedRecords >= SPAM_ROOMS / 2, "the records over the spammer's rate and the forged records are dropped");
        Assertions.assertEquals(LATE_FLOOD_ROOMS + 1, graylisted, "every later datagram of the flooder is dropped");
        Assertions.assertEquals(answers, flooder.getAnswerCount(), "the graylisted flooder gets no answer");
        Assertions.assertTrue(lists(victimPeer, honest.getKey()));
        Assertions.assertEquals(peers.size(), countListing(peers, honest.getKey()), "the honest room reaches every peer");

        for (RoomAdvertisement late : lateSpam) {
            Assertions.assertFalse(lists(victimPeer, late.getKey()), "a record of the graylisted flooder is listed");
        }
    }
}
