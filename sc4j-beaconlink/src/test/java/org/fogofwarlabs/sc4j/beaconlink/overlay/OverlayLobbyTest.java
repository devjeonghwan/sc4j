package org.fogofwarlabs.sc4j.beaconlink.overlay;

import org.fogofwarlabs.sc4j.beaconlink.client.BeaconLinkClient;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.test.client.ClientScenario;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.lockstep.PeerTurn;
import org.fogofwarlabs.sc4j.engine.lockstep.TurnSchedule;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.NetworkClock;
import org.fogofwarlabs.sc4j.engine.net.TransportTimeouts;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.*;
import org.fogofwarlabs.sc4j.engine.net.lobby.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Tests the engine's network lobby over the Beacon.Link overlay on 127.0.0.1 behind emulated NATs: a published room is the lobby, three
 * members whose symmetric pair has no direct path hold channels to each other through the host, a joiner without the map receives it over
 * the lobby port, the host starts only once the members are ready and connected, every member's start names the same session and
 * participants, and the lockstep transports of the start carry the same turns in the same order at every member, also when two members are
 * connections of one account; a join the host refuses ends with its reason, and a join of a pair that cannot connect ends with the failure.
 */
final class OverlayLobbyTest {
    private static final @NotNull ModIdentity IDENTITY = ClientScenario.createIdentity("1.0.0");
    private static final long CHANNEL_STEP_MILLIS = 100L;
    private static final long CONNECT_MILLIS = 30_000L;
    private static final long GAME_MILLIS = 60_000L;
    private static final int MAP_LENGTH = 50_000;
    private static final int TURN_COUNT = 40;
    private static final int TURN_LENGTH = 1500;
    private static final int TURNS_IN_TRANSIT = 2;
    private static final int ROOM_CAPACITY = 3;
    private static final int FIRST_SLOT = 0;
    private static final int SECOND_SLOT = 1;
    private static final long SEED = 20261108L;
    private static final int FRAMES_PER_TURN = 2;
    private static final int LATENCY_TURNS = 2;

    /**
     * Holds the maps of one machine in memory.
     */
    private static final class MemoryMaps implements LobbyMaps {
        private final @NotNull List<@NotNull MapReference> references;
        private final @NotNull List<byte @NotNull []> contents;

        private MemoryMaps() {
            this.references = new ArrayList<>();
            this.contents   = new ArrayList<>();
        }

        @Override
        public boolean hasMap(@NotNull MapReference map) {
            return this.readMap(map) != null;
        }

        @Override
        public byte @Nullable [] readMap(@NotNull MapReference map) {
            for (int index = 0; index < this.references.size(); index++) {
                if (this.references.get(index).isSameMap(map)) {
                    return this.contents.get(index);
                }
            }

            return null;
        }

        @Override
        public void addMap(@NotNull MapReference map, byte @NotNull [] mapContents) {
            this.references.add(map);
            this.contents.add(mapContents);
        }
    }

    /**
     * Holds one member's transport of the started game, its next turn and the turns it received in order.
     */
    private static final class Machine {
        private final @NotNull LinkLockstepTransport transport;
        private final int peerNumber;
        private final @NotNull List<@NotNull String> received;
        private int sent;

        private Machine(@NotNull LinkLockstepTransport transport, int peerNumber) {
            this.transport  = transport;
            this.peerNumber = peerNumber;
            this.received   = new ArrayList<>();
            this.sent       = 0;
        }

        private void step() {
            this.transport.poll();

            while (this.sent < TURN_COUNT && this.transport.getTurnsInTransit() < TURNS_IN_TRANSIT) {
                this.transport.sendTurn(createTurn(this.peerNumber, this.sent));
                this.sent++;
            }

            List<PeerTurn> turns = this.transport.receiveTurns();

            while (turns != null) {
                for (PeerTurn turn : turns) {
                    this.received.add(turn.getPeer().getNumber() + ":" + Arrays.hashCode(turn.getTurn()));
                }

                turns = this.transport.receiveTurns();
            }
        }
    }

    private static byte @NotNull [] createTurn(int peerNumber, int index) {
        byte[] turn = new byte[TURN_LENGTH];

        for (int position = 0; position < turn.length; position++) {
            turn[position] = (byte) (peerNumber * 31 + index * 7 + position);
        }

        return turn;
    }

    private static @NotNull NetworkGameStart requireStart(@NotNull NetworkLobby lobby) {
        NetworkGameStart start = lobby.getStart();

        Assertions.assertNotNull(start, "the member's lobby started");

        return start;
    }


    private final @NotNull ClientScenario scenario;
    private final @NotNull NetworkClock clock;
    private final @NotNull GameSetupCodec codec;
    private final @NotNull MemoryMaps hostMaps;
    private final @NotNull MapReference map;
    private final @NotNull List<@NotNull NetworkLobby> lobbies;

    OverlayLobbyTest() {
        byte[] mapContents = new byte[MAP_LENGTH];

        new SplittableRandom(SEED).nextBytes(mapContents);

        this.scenario = new ClientScenario();
        this.clock    = this.scenario.getClock()::getEpochMillis;
        this.codec    = new GameSetupCodec(List.of(new Faction("north"), new Faction("south")), List.of());
        this.hostMaps = new MemoryMaps();
        this.map      = MapReference.create("Ford", mapContents);
        this.lobbies  = new ArrayList<>();

        this.hostMaps.addMap(this.map, mapContents);
    }

    @AfterEach
    void closeScenario() {
        for (NetworkLobby lobby : this.lobbies) {
            lobby.close();
        }

        this.scenario.close();
    }

    private @NotNull BeaconLinkClient openClient(@NotNull PeerSocket node, @NotNull NatType type, @NotNull ModIdentity identity) throws IOException {
        BeaconLinkClient client = this.scenario.openClient(identity, List.of(ClientScenario.listed(node)),
                                                           this.scenario.openNat(type).openEndpoint());

        this.scenario.await(() -> client.getObservedAddress() != null, "the client registers");

        return client;
    }

    private @NotNull NetworkLobby host(@NotNull BeaconLinkClient host) throws IOException {
        HostedRoom room = host.publishRoom(new RoomSettings("Evening game", this.map.getName(), 1, ROOM_CAPACITY));

        this.scenario.await(() -> room.getState() == HostedRoomState.PUBLISHED, "the room is published");

        NetworkLobby lobby = NetworkLobby.host(room, host.getOverlay(), this.clock, "host", IDENTITY, this.codec, this.hostMaps, this.map,
                                               new int[]{FIRST_SLOT, SECOND_SLOT}, ROOM_CAPACITY);

        this.lobbies.add(lobby);
        this.scenario.addPollAction(lobby::poll);

        return lobby;
    }

    /**
     * Joins the room of a host found by the host's own search, which lists the rooms of the host's mod identity.
     */
    private @NotNull NetworkLobby join(@NotNull BeaconLinkClient host, @NotNull BeaconLinkClient joiner, @NotNull String name) throws IOException {
        RoomSearch search = host.searchRooms();

        this.scenario.await(search::isComplete, "the search completes");

        FoundRoom found = search.getRooms().getFirst();
        NetworkLobby lobby = NetworkLobby.join(joiner.joinRoom(found), joiner.getOverlay(), this.clock, name, IDENTITY, this.codec,
                                               new MemoryMaps());

        this.lobbies.add(lobby);
        this.scenario.addPollAction(lobby::poll);

        return lobby;
    }

    private void awaitMapPresent(@NotNull NetworkLobby lobby) throws IOException {
        this.scenario.advanceUntil(() -> {
            LobbyMember local = lobby.getLocalMember();

            return local != null && local.getMapState() == LobbyMapState.PRESENT;
        }, CONNECT_MILLIS, CHANNEL_STEP_MILLIS, "the joiner has the room's map");
    }

    @Test
    void three_members_through_a_relay_receive_the_map_start_the_same_game_and_carry_the_same_turns() throws IOException {
        PeerSocket node = this.scenario.openNode(ClientScenario.createNodeConfiguration());
        BeaconLinkClient host = this.openClient(node, NatType.FULL_CONE, IDENTITY);
        BeaconLinkClient first = this.openClient(node, NatType.SYMMETRIC, IDENTITY);
        BeaconLinkClient second = this.openClient(node, NatType.SYMMETRIC, IDENTITY);

        this.playGame(host, first, second);

        Assertions.assertTrue(host.getRelayedCount() > 0, "the host relays for the symmetric pair");
    }

    @Test
    void two_connections_of_one_account_and_a_third_player_play_the_same_game() throws IOException {
        PeerSocket node = this.scenario.openNode(ClientScenario.createNodeConfiguration());
        BeaconLinkClient host = this.openClient(node, NatType.FULL_CONE, IDENTITY);
        BeaconLinkClient first = this.openClient(node, NatType.PORT_RESTRICTED, IDENTITY);
        BeaconLinkClient second = this.scenario.openConnection(first, IDENTITY, List.of(ClientScenario.listed(node)));

        this.scenario.await(() -> second.getObservedAddress() != null, "the account's second connection registers");

        Assertions.assertEquals(first.getParticipant().getPlayer(), second.getParticipant().getPlayer());

        this.playGame(host, first, second);
    }

    /**
     * Plays a game of three members over the overlay: the host's room is the lobby, both joiners receive the map, every member gets ready,
     * the start reaches everybody with the same session and participants, and the lockstep transports carry every turn to every member.
     */
    private void playGame(@NotNull BeaconLinkClient host, @NotNull BeaconLinkClient first, @NotNull BeaconLinkClient second) throws IOException {
        NetworkLobby hostLobby = this.host(host);
        NetworkLobby firstLobby = this.join(host, first, "first");

        this.awaitMapPresent(firstLobby);

        NetworkLobby secondLobby = this.join(host, second, "second");

        this.awaitMapPresent(secondLobby);
        Assertions.assertThrows(IllegalStateException.class, hostLobby::getLocalAddress, "a room over Beacon.Link has no listening socket");

        for (NetworkLobby lobby : List.of(firstLobby, secondLobby, hostLobby)) {
            lobby.setReady(true);
        }

        this.scenario.advanceUntil(hostLobby::isReadyToStart, CONNECT_MILLIS, CHANNEL_STEP_MILLIS, "every member is ready and connected");

        Assertions.assertEquals(ROOM_CAPACITY, hostLobby.getMembers().size());
        Assertions.assertTrue(first.getOverlay().getParticipants().contains(second.getParticipant()), "the joiners hold a channel to each other");

        GameSetup setup = new GameSetup.Builder(this.map)
                .addPlayer(new PlayerSlot(FIRST_SLOT, ControllerKind.HUMAN, 0, 0, null))
                .addPlayer(new PlayerSlot(SECOND_SLOT, ControllerKind.HUMAN, 1, 1, null))
                .setSeed(SEED)
                .build();
        NetworkGameStart hostStart = hostLobby.start(setup, new TurnSchedule(FRAMES_PER_TURN, LATENCY_TURNS));

        this.scenario.advanceUntil(() -> firstLobby.getLobbyState() == LobbyState.STARTED && secondLobby.getLobbyState() == LobbyState.STARTED,
                                   CONNECT_MILLIS, CHANNEL_STEP_MILLIS, "the start reaches every member");

        List<NetworkGameStart> starts = List.of(hostStart, requireStart(firstLobby), requireStart(secondLobby));
        List<LinkParticipant> participants = List.of(host.getParticipant(), first.getParticipant(), second.getParticipant());
        List<BeaconLinkClient> clients = List.of(host, first, second);
        List<Machine> machines = new ArrayList<>();

        for (int index = 0; index < starts.size(); index++) {
            NetworkGameStart start = starts.get(index);

            Assertions.assertTrue(start.isOverBeaconLink());
            Assertions.assertTrue(start.getAddresses().isEmpty());
            Assertions.assertEquals(hostStart.getSessionId(), start.getSessionId());
            Assertions.assertEquals(participants, start.getLinkPeers().stream().map(LinkPeer::getAddress).toList());
            Assertions.assertEquals(index, start.getLocalPeer().getNumber());
            machines.add(new Machine(start.createTransport(clients.get(index).getOverlay().openPort(LinkOverlay.GAME_PORT), this.clock,
                                                           TransportTimeouts.DEFAULTS), index));
        }

        for (Machine machine : machines) {
            this.scenario.addPollAction(machine::step);
        }

        this.scenario.advanceUntil(() -> machines.stream().allMatch(machine -> machine.received.size() == TURN_COUNT * machines.size()),
                                   GAME_MILLIS, CHANNEL_STEP_MILLIS, "every member receives every turn");

        List<String> expected = new ArrayList<>();

        for (int turn = 0; turn < TURN_COUNT; turn++) {
            for (int peer = 0; peer < machines.size(); peer++) {
                expected.add(peer + ":" + Arrays.hashCode(createTurn(peer, turn)));
            }
        }

        for (Machine machine : machines) {
            Assertions.assertEquals(expected, machine.received, "the turns in execution order at peer " + machine.peerNumber);
        }
    }

    @Test
    void join_the_host_refuses_ends_with_its_reason() throws IOException {
        PeerSocket node = this.scenario.openNode(ClientScenario.createNodeConfiguration());
        BeaconLinkClient host = this.openClient(node, NatType.FULL_CONE, IDENTITY);
        BeaconLinkClient joiner = this.openClient(node, NatType.PORT_RESTRICTED, ClientScenario.createIdentity("1.0.1"));
        NetworkLobby hostLobby = this.host(host);
        NetworkLobby lobby = this.join(host, joiner, "newer");

        this.scenario.advanceUntil(() -> lobby.getLobbyState() != LobbyState.JOINING, CONNECT_MILLIS, CHANNEL_STEP_MILLIS, "the join ends");

        Assertions.assertEquals(LobbyState.REFUSED, lobby.getLobbyState());
        Assertions.assertEquals(JoinRefusal.MOD_VERSION, lobby.getRefusal());
        Assertions.assertTrue(lobby.getRefusalText().contains(JoinRefusal.MOD_VERSION.getDescription()), lobby.getRefusalText());
        Assertions.assertEquals(1, hostLobby.getMembers().size());
    }

    @Test
    void join_of_a_pair_that_cannot_connect_ends_with_the_failure() throws IOException {
        PeerSocket node = this.scenario.openNode(ClientScenario.createNodeConfiguration());
        BeaconLinkClient host = this.openClient(node, NatType.SYMMETRIC, IDENTITY);
        BeaconLinkClient joiner = this.openClient(node, NatType.SYMMETRIC, IDENTITY);

        this.host(host);

        NetworkLobby lobby = this.join(host, joiner, "joiner");

        this.scenario.advanceUntil(() -> lobby.getLobbyState() != LobbyState.JOINING, CONNECT_MILLIS, CHANNEL_STEP_MILLIS, "the join ends");

        Assertions.assertEquals(LobbyState.CLOSED, lobby.getLobbyState());
        Assertions.assertNull(lobby.getRefusal());
        Assertions.assertTrue(lobby.getRefusalText().contains(ChannelFailure.PAIR_CANNOT_CONNECT.getDescription()), lobby.getRefusalText());
    }
}
