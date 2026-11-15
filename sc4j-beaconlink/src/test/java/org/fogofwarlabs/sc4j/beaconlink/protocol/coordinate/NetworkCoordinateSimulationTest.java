package org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.beaconlink.client.ClientConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.test.ManualProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.test.client.ClientScenario;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.*;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * Tests the network coordinates on the in-memory network with a measured-style latency matrix ({@link InternetLatencyModel}): after the
 * peers and the clients (each with its embedded peer, as the runtime runs it) took the samples of their ordinary traffic, nine in ten peers
 * have a settled coordinate, most found rooms carry a latency estimate, and the median relative error of the settled peers' estimated round
 * trip times and of the latency estimates of the clients' room searches is at most a quarter.
 */
final class NetworkCoordinateSimulationTest {
    private static final @NotNull Logger LOGGER = Logger.of(NetworkCoordinateSimulationTest.class);
    private static final long RANDOM_SEED = 20261110L;
    private static final double LOSS_RATE = 0.01;
    private static final int PROOF_DIFFICULTY = 8;
    private static final int STAMP_DIFFICULTY = 8;
    private static final int SEED_COUNT = 3;
    private static final int PEER_COUNT = 200;
    private static final int CLIENT_COUNT = 40;
    private static final int HOST_COUNT = 10;
    private static final int NODES_PER_CLIENT = 2;
    private static final long JOIN_INTERVAL_MILLIS = 100L;
    private static final long PEER_SETTLE_MILLIS = 900_000L;
    private static final long CLIENT_SETTLE_MILLIS = 900_000L;
    private static final long ROOM_SETTLE_MILLIS = 60_000L;
    private static final long SEARCH_MILLIS = 120_000L;
    private static final long SEARCH_STEP_MILLIS = 5_000L;
    private static final double REQUIRED_MEDIAN_ERROR = 0.25;
    private static final double REQUIRED_SETTLED_SHARE = 0.9;
    private static final double REQUIRED_KNOWN_SHARE = 0.8;
    private static final double TENTH = 0.1;
    private static final double MEDIAN = 0.5;
    private static final double NINETIETH = 0.9;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final @NotNull RoomSettings SETTINGS = new RoomSettings("Latency room", "Ashen Ford", 1, 4);
    private static final @Nullable NatType @NotNull [] PEER_NAT_MIX = {null, null, null, null, null, NatType.FULL_CONE, NatType.ADDRESS_RESTRICTED,
                                                                       NatType.PORT_RESTRICTED, NatType.PORT_RESTRICTED, NatType.SYMMETRIC};
    private static final @Nullable NatType @NotNull [] CLIENT_NAT_MIX = {null, NatType.PORT_RESTRICTED, NatType.ADDRESS_RESTRICTED,
                                                                         NatType.SYMMETRIC};

    private static double findQuantile(@NotNull List<@NotNull Double> values, double quantile) {
        List<Double> sorted = new ArrayList<>(values);

        Collections.sort(sorted);

        return sorted.get(Math.min(sorted.size() - 1, (int) (quantile * sorted.size())));
    }

    private static double findRelativeError(double estimatedMillis, double measuredMillis) {
        return Math.abs(estimatedMillis - measuredMillis) / measuredMillis;
    }

    @Test
    @LargeSimulation
    void peers_and_room_searches_estimate_round_trip_times_within_a_quarter() {
        long wallStart = System.nanoTime();
        SimulatedNetwork network = new SimulatedNetwork(RANDOM_SEED, InternetLatencyModel::new, LOSS_RATE);
        PeerConfiguration peerConfiguration = new PeerConfiguration.Builder().setProofDifficulty(PROOF_DIFFICULTY)
                                                                             .setPlayerStampDifficulty(STAMP_DIFFICULTY).build();
        ClientConfiguration clientConfiguration = new ClientConfiguration.Builder().setStampDifficulty(STAMP_DIFFICULTY).build();
        ModIdentity identity = ClientScenario.createIdentity("1.0.0");
        List<SimulatedPeer> peers = new ArrayList<>();
        List<InetSocketAddress> seeds = new ArrayList<>();
        List<InetSocketAddress> publicPeers = new ArrayList<>();

        for (int index = 0; index < PEER_COUNT; index++) {
            NatType nat = index < SEED_COUNT ? null : PEER_NAT_MIX[index % PEER_NAT_MIX.length];
            List<InetSocketAddress> greeted = index < SEED_COUNT ? List.copyOf(seeds) : List.of(seeds.get(network.getRandom().nextInt(SEED_COUNT)));
            SimulatedPeer peer = network.addPeer(peerConfiguration, nat, greeted);

            peers.add(peer);

            if (index < SEED_COUNT) {
                seeds.add(peer.getAddress());
            }

            if (nat == null) {
                publicPeers.add(peer.getAddress());
            }

            network.run(JOIN_INTERVAL_MILLIS);
        }

        network.run(PEER_SETTLE_MILLIS);

        List<SimulatedClient> clients = new ArrayList<>();

        for (int index = 0; index < CLIENT_COUNT; index++) {
            List<InetSocketAddress> candidates = new ArrayList<>(publicPeers);
            List<InetSocketAddress> nodes = new ArrayList<>();

            for (int node = 0; node < NODES_PER_CLIENT; node++) {
                nodes.add(candidates.remove(network.getRandom().nextInt(candidates.size())));
            }

            SimulatedClient client = network.addClient(clientConfiguration, CLIENT_NAT_MIX[index % CLIENT_NAT_MIX.length], identity,
                                                       "Player" + index, nodes);

            // A runtime polls its session every frame, so a measured round trip time is not stretched to the next tick of the network.
            client.setPromptPolling(true);
            client.getClient().attachPeer(peerConfiguration, network.createKeyPair());
            clients.add(client);
            network.run(JOIN_INTERVAL_MILLIS);
        }

        network.run(CLIENT_SETTLE_MILLIS);

        Map<LinkParticipant, SimulatedClient> hosts = new HashMap<>();

        for (SimulatedClient host : clients.subList(0, HOST_COUNT)) {
            host.getClient().publishRoom(SETTINGS);
            hosts.put(host.getClient().getParticipant(), host);
        }

        network.run(ROOM_SETTLE_MILLIS);

        List<RoomSearch> searches = new ArrayList<>();

        for (SimulatedClient searcher : clients.subList(HOST_COUNT, CLIENT_COUNT)) {
            searches.add(searcher.getClient().searchRooms());
        }

        for (long waited = 0L; waited < SEARCH_MILLIS && !searches.stream().allMatch(search -> search.getRooms().size() == HOST_COUNT);
             waited += SEARCH_STEP_MILLIS) {
            network.run(SEARCH_STEP_MILLIS);

            // A search that ended before its nodes held every room is repeated, as a room list refreshes.
            for (int index = 0; index < searches.size(); index++) {
                if (searches.get(index).isComplete() && searches.get(index).getRooms().size() < HOST_COUNT) {
                    searches.set(index, clients.get(HOST_COUNT + index).getClient().searchRooms());
                }
            }
        }

        List<Double> roomErrors = new ArrayList<>();
        int roomCount = 0;

        for (int index = 0; index < searches.size(); index++) {
            SimulatedClient searcher = clients.get(HOST_COUNT + index);

            for (FoundRoom room : searches.get(index).getRooms()) {
                SimulatedClient host = hosts.get(room.getHost());
                LatencyEstimate estimate = room.getLatencyEstimate();

                Assertions.assertNotNull(host, "a found room has a known host");
                roomCount++;

                if (estimate.isKnown()) {
                    roomErrors.add(findRelativeError(estimate.getRoundTripMillis(),
                                                     network.getRoundTripMillis(searcher.getLocalAddress(), host.getLocalAddress())));
                }
            }
        }

        List<SimulatedPeer> settled = new ArrayList<>();
        List<Double> peerErrors = new ArrayList<>();
        List<Double> roundTrips = new ArrayList<>();

        for (SimulatedPeer peer : peers) {
            if (peer.getPeer().getCoordinates().isSettled()) {
                settled.add(peer);
            }
        }

        for (int first = 0; first < settled.size(); first++) {
            NetworkCoordinate coordinate = settled.get(first).getPeer().getCoordinates().getCoordinate();

            for (int second = first + 1; second < settled.size(); second++) {
                double measured = network.getRoundTripMillis(settled.get(first).getAddress(), settled.get(second).getAddress());
                NetworkCoordinate other = settled.get(second).getPeer().getCoordinates().getCoordinate();

                roundTrips.add(measured);
                peerErrors.add(findRelativeError(coordinate.estimateRoundTripMillis(other), measured));
            }
        }

        long settledClients = clients.stream().filter(client -> client.getClient().getCoordinates().isSettled()).count();
        double peerMedian = findQuantile(peerErrors, MEDIAN);
        double roomMedian = roomErrors.isEmpty() ? Double.NaN : findQuantile(roomErrors, MEDIAN);
        double knownShare = roomCount == 0 ? 0.0 : (double) roomErrors.size() / roomCount;

        double roomNinetieth = roomErrors.isEmpty() ? Double.NaN : findQuantile(roomErrors, NINETIETH);

        LOGGER.info("Coordinate simulation: {0} peers ({1} settled), {2} clients ({3} settled); round trip times of the settled peer pairs: " +
                    "10% {4} ms, median {5} ms, 90% {6} ms; peer pairs: median relative error {7}, 90% {8}; rooms: {9} found, {10} known " +
                    "({11}), median relative error {12}, 90% {13}; {14} sent, {15} lost, {16} filtered; {17} ms virtual, {18} ms wall",
                    peers.size(), settled.size(), clients.size(), settledClients, findQuantile(roundTrips, TENTH), findQuantile(roundTrips, MEDIAN),
                    findQuantile(roundTrips, NINETIETH), peerMedian, findQuantile(peerErrors, NINETIETH), roomCount, roomErrors.size(), knownShare,
                    roomMedian, roomNinetieth, network.getSentCount(), network.getLostCount(), network.getFilteredCount(),
                    network.getClock().getEpochMillis() - ManualProtocolClock.START_MILLIS, (System.nanoTime() - wallStart) / NANOS_PER_MILLI);

        Assertions.assertTrue(settled.size() >= peers.size() * REQUIRED_SETTLED_SHARE, "settled peers " + settled.size());
        Assertions.assertEquals(HOST_COUNT * (CLIENT_COUNT - HOST_COUNT), roomCount, "every searcher finds every room");
        Assertions.assertTrue(knownShare >= REQUIRED_KNOWN_SHARE, "share of rooms with a latency estimate " + knownShare);
        Assertions.assertTrue(peerMedian <= REQUIRED_MEDIAN_ERROR, "median relative error of the peers " + peerMedian);
        Assertions.assertTrue(roomMedian <= REQUIRED_MEDIAN_ERROR, "median relative error of the room estimates " + roomMedian);
    }
}
