package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.lockstep.InMemoryLockstepNetwork;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepGameScenario;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepPeer;
import org.fogofwarlabs.sc4j.engine.lockstep.TurnSchedule;
import org.fogofwarlabs.sc4j.engine.map.MapTestResources;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.*;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.version.EngineVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Tests the engine network lobby on 127.0.0.1: a host and a joiner start a game whose setup and seed reach the joiner, and their sessions
 * over UDP reach the same state hash every frame as the in-memory reference; a joiner without the room's map receives it, chooses its faction,
 * and the host starts only once every member is ready with a setup of the room's map and the members' factions, and a new map clears the ready
 * flags; a joiner with another mod identity is refused with the first differing part; a full room refuses a joiner and a leaving joiner frees
 * its place; a joiner whose connection is refused closes its lobby.
 */
final class NetworkLobbyTest {
    private static final @NotNull InetSocketAddress LOOPBACK = new InetSocketAddress("127.0.0.1", 0);
    private static final int DIGEST_LENGTH = 32;
    private static final int SCOUT_SLOT = 3;
    private static final int FRAME_COUNT = 600;
    private static final int PATROL_FRAME = 40;
    private static final int NO_ORDER = -1;
    private static final long ROUND_MILLIS = 5L;
    private static final long WAIT_SECONDS = 10L;
    private static final @NotNull String MAP_EXTENSION = ".map";
    private static final @NotNull TransportTimeouts TIMEOUTS = new TransportTimeouts(20L, 200L, 1000L);

    /**
     * Tests a condition of the polled lobbies.
     */
    @FunctionalInterface
    private interface Condition {
        boolean isMet();
    }

    /**
     * Holds the maps of one machine in memory and records the maps it received.
     */
    private static final class MemoryMaps implements LobbyMaps {
        private final @NotNull List<@NotNull MapReference> references;
        private final @NotNull List<byte @NotNull []> contents;
        private int receivedCount;

        private MemoryMaps() {
            this.references    = new ArrayList<>();
            this.contents      = new ArrayList<>();
            this.receivedCount = 0;
        }

        private @NotNull MapReference add(@NotNull String name, byte @NotNull [] mapContents) {
            MapReference reference = MapReference.create(name, mapContents);

            this.references.add(reference);
            this.contents.add(mapContents);

            return reference;
        }

        @Override
        public boolean hasMap(@NotNull MapReference map) {
            return this.find(map) >= 0;
        }

        @Override
        public byte @Nullable [] readMap(@NotNull MapReference map) {
            int index = this.find(map);

            return index < 0 ? null : this.contents.get(index);
        }

        @Override
        public void addMap(@NotNull MapReference map, byte @NotNull [] mapContents) {
            this.references.add(map);
            this.contents.add(mapContents);
            this.receivedCount++;
        }

        private int find(@NotNull MapReference map) {
            for (int index = 0; index < this.references.size(); index++) {
                if (this.references.get(index).isSameMap(map)) {
                    return index;
                }
            }

            return -1;
        }
    }

    private static @NotNull ModIdentity createIdentity(@NotNull String version, int content) throws ManifestException {
        String text = "{\"format\": 1, \"id\": \"duel\", \"name\": {\"en\": \"Duel\"}, \"version\": \"" + version +
                      "\", \"engine\": \"0.1\", \"entry\": \"org.example.DuelMod\"}";
        ModManifest manifest = ModManifestReader.parse(text.getBytes(StandardCharsets.UTF_8));
        byte[] digest = new byte[DIGEST_LENGTH];

        digest[0] = (byte) content;

        return ModIdentity.create(EngineVersion.CURRENT, manifest, digest, List.of());
    }

    private final @NotNull LockstepGameScenario scenario;
    private final @NotNull GameSetupCodec codec;
    private final @NotNull MemoryMaps hostMaps;
    private final @NotNull MapReference twinRocks;
    private final @NotNull List<@NotNull DatagramEndpoint> endpoints;
    private final @NotNull List<@NotNull NetworkLobby> lobbies;

    NetworkLobbyTest() throws IOException {
        this.scenario  = new LockstepGameScenario();
        this.codec     = new GameSetupCodec(List.of(this.scenario.getScenario().getWardens(), this.scenario.getScenario().getRaiders()), List.of());
        this.hostMaps  = new MemoryMaps();
        this.twinRocks = this.scenario.createSetup(LockstepGameScenario.SEED).getMap();
        this.endpoints = new ArrayList<>();
        this.lobbies   = new ArrayList<>();

        this.hostMaps.add(this.twinRocks.getName(), MapTestResources.read(MapTestResources.TWIN_ROCKS));
    }

    @AfterEach
    void close() {
        for (NetworkLobby lobby : this.lobbies) {
            lobby.close();
        }

        for (DatagramEndpoint endpoint : this.endpoints) {
            IgnoreException.execute(
                    endpoint::close
            );
        }
    }

    @Test
    void a_started_game_reaches_the_joiner_and_runs_the_same_state_hash_every_frame_as_in_memory() throws IOException, ManifestException {
        ModIdentity identity = createIdentity("1.0.0", 1);
        DatagramEndpoint hostGame = this.open();
        DatagramEndpoint joinerGame = this.open();
        NetworkLobby host = this.host(identity, hostGame.getLocalAddress(), new int[]{SCOUT_SLOT}, 2);
        NetworkLobby joiner = this.join(host, identity, this.hostMaps, joinerGame.getLocalAddress().getPort());

        this.pollUntil(() -> host.getMembers().size() == 2 && joiner.getMembers().size() == 2, host, joiner);

        LobbyMember joinerMember = joiner.getLocalMember();

        Assertions.assertEquals(LobbyState.OPEN, joiner.getLobbyState());
        Assertions.assertNotNull(joinerMember);
        Assertions.assertTrue(joinerMember.isObserver());
        Assertions.assertEquals(SCOUT_SLOT, host.getMembers().getFirst().getSlotIndex());
        Assertions.assertFalse(host.assignSlot(joinerMember.getPeer(), SCOUT_SLOT));

        GameSetup setup = this.scenario.createSetup(LockstepGameScenario.SEED);

        joiner.setReady(true);
        host.setReady(true);
        this.pollUntil(host::isReadyToStart, host, joiner);

        NetworkGameStart hostStart = host.start(setup, new TurnSchedule(LockstepGameScenario.FRAMES_PER_TURN, LockstepGameScenario.LATENCY_TURNS));

        this.pollUntil(() -> joiner.getLobbyState() == LobbyState.STARTED, host, joiner);

        NetworkGameStart joinerStart = joiner.getStart();

        Assertions.assertNotNull(joinerStart);
        Assertions.assertEquals(hostStart.getSessionId(), joinerStart.getSessionId());
        Assertions.assertEquals(LockstepGameScenario.SEED, joinerStart.getSetup().getSeed());
        Assertions.assertEquals(setup.getPlayers(), joinerStart.getSetup().getPlayers());
        Assertions.assertEquals(hostGame.getLocalAddress(), joinerStart.getAddresses().getFirst().getAddress());
        Assertions.assertEquals(joinerGame.getLocalAddress(), hostStart.getAddresses().get(1).getAddress());

        ManualNetworkClock clock = new ManualNetworkClock();
        UdpLockstepTransport hostTransport = hostStart.createTransport(hostGame, clock, TIMEOUTS);
        UdpLockstepTransport joinerTransport = joinerStart.createTransport(joinerGame, clock, TIMEOUTS);
        List<LockstepGameScenario.Machine> machines = List.of(this.createMachine(hostStart, hostTransport, PATROL_FRAME),
                                                              this.createMachine(joinerStart, joinerTransport, NO_ORDER));

        LockstepGameScenario.run(machines, FRAME_COUNT, () -> {
            clock.advance(ROUND_MILLIS);
            hostTransport.poll();
            joinerTransport.poll();
        });

        Assertions.assertEquals(this.runInMemory(), machines.get(0).getFrameHashes());
        Assertions.assertEquals(machines.get(0).getFrameHashes(), machines.get(1).getFrameHashes());
        Assertions.assertEquals(1, machines.get(1).getExecuted());
    }

    @Test
    void a_joiner_receives_the_missing_map_chooses_its_faction_and_the_host_starts_once_every_member_is_ready() throws IOException,
                                                                                                                       ManifestException {
        ModIdentity identity = createIdentity("1.0.0", 1);
        MemoryMaps joinerMaps = new MemoryMaps();
        NetworkLobby host = this.host(identity, this.open().getLocalAddress(), new int[]{0, 1}, 2);
        NetworkLobby joiner = this.join(host, identity, joinerMaps, this.open().getLocalAddress().getPort());

        this.pollUntil(() -> this.findMapState(host, 1) == LobbyMapState.PRESENT && joiner.getLocalMember() != null, host, joiner);

        Assertions.assertEquals(1, joinerMaps.receivedCount);
        Assertions.assertTrue(joinerMaps.hasMap(this.twinRocks));
        Assertions.assertNotNull(joiner.getMap());
        Assertions.assertTrue(this.twinRocks.isSameMap(joiner.getMap()));

        joiner.setFaction(this.scenario.getScenario().getRaiders());
        joiner.setReady(true);
        this.pollUntil(() -> host.getMembers().get(1).isReady(), host, joiner);

        Assertions.assertSame(this.scenario.getScenario().getRaiders(), host.getMembers().get(1).getFaction());
        Assertions.assertFalse(host.isReadyToStart());
        Assertions.assertThrows(IllegalStateException.class, () -> host.start(this.createDuelSetup(this.twinRocks), this.createSchedule()));

        host.setReady(true);

        Assertions.assertTrue(host.isReadyToStart());
        Assertions.assertThrows(IllegalArgumentException.class, () -> host.start(this.createDuelSetup(this.twinRocks, 0), this.createSchedule()));

        // A new map clears every ready flag; the joiner receives that map too.
        MapReference renamed = this.hostMaps.add("renamed rocks", MapTestResources.read(MapTestResources.TWIN_ROCKS));

        host.setMap(renamed);

        Assertions.assertFalse(host.isReadyToStart());

        this.pollUntil(() -> this.findMapState(host, 1) == LobbyMapState.PRESENT && joiner.getMembers().stream().noneMatch(LobbyMember::isReady),
                       host, joiner);

        Assertions.assertEquals(2, joinerMaps.receivedCount);
        Assertions.assertThrows(IllegalStateException.class, () -> host.start(this.createDuelSetup(renamed), this.createSchedule()));

        joiner.setReady(true);
        host.setReady(true);
        this.pollUntil(host::isReadyToStart, host, joiner);

        Assertions.assertThrows(IllegalArgumentException.class, () -> host.start(this.createDuelSetup(this.twinRocks), this.createSchedule()));

        host.start(this.createDuelSetup(renamed), this.createSchedule());
        this.pollUntil(() -> joiner.getLobbyState() == LobbyState.STARTED, host, joiner);

        NetworkGameStart joinerStart = joiner.getStart();

        Assertions.assertNotNull(joinerStart);
        Assertions.assertTrue(renamed.isSameMap(joinerStart.getSetup().getMap()));
        Assertions.assertSame(this.scenario.getScenario().getRaiders(), joinerStart.getSetup().getPlayers().get(1).getFaction());
    }

    @Test
    void a_joiner_with_folder_maps_saves_the_missing_map_and_has_it_in_the_next_room(@TempDir @NotNull Path folder) throws IOException,
                                                                                                                           ManifestException {
        ModIdentity identity = createIdentity("1.0.0", 1);
        MapFolders folders = MapFolders.of(List.of(folder), MapFolders.DOWNLOAD_FOLDER);
        NetworkLobby host = this.host(identity, this.open().getLocalAddress(), new int[]{0, 1}, 3);
        NetworkLobby joiner = this.join(host, identity, new FolderLobbyMaps(folders, MAP_EXTENSION), this.open().getLocalAddress().getPort());

        this.pollUntil(() -> this.findMapState(host, 1) == LobbyMapState.PRESENT, host, joiner);

        Path saved = folder.resolve(MapFolders.DOWNLOAD_FOLDER).resolve(this.twinRocks.getName() + MAP_EXTENSION);

        Assertions.assertArrayEquals(MapTestResources.read(MapTestResources.TWIN_ROCKS), Files.readAllBytes(saved));

        NetworkLobby next = this.join(host, identity, new FolderLobbyMaps(folders, MAP_EXTENSION), this.open().getLocalAddress().getPort());

        this.pollUntil(() -> this.findMapState(host, 2) == LobbyMapState.PRESENT, host, joiner, next);

        try (Stream<Path> files = Files.list(saved.getParent())) {
            Assertions.assertEquals(List.of(saved), files.toList(), "the next joiner finds the saved map");
        }
    }

    @Test
    void request_slot_should_move_the_joiner_between_an_open_free_slot_and_the_observers() throws IOException, ManifestException {
        ModIdentity identity = createIdentity("1.0.0", 1);
        NetworkLobby host = this.host(identity, this.open().getLocalAddress(), new int[]{0, 1}, 2);
        NetworkLobby joiner = this.join(host, identity, this.hostMaps, this.open().getLocalAddress().getPort());

        this.pollUntil(() -> host.getMembers().size() == 2 && joiner.getMembers().size() == 2, host, joiner);

        Assertions.assertEquals(0, host.getMembers().getFirst().getSlotIndex());
        Assertions.assertEquals(1, host.getMembers().get(1).getSlotIndex(), "a joiner takes the first free slot");

        joiner.requestSlot(NetworkLobby.OBSERVER_SLOT);
        this.pollUntil(() -> joiner.getMembers().get(1).isObserver(), host, joiner);

        Assertions.assertTrue(host.getMembers().get(1).isObserver());

        joiner.requestSlot(1);
        this.pollUntil(() -> joiner.getMembers().get(1).getSlotIndex() == 1, host, joiner);

        Assertions.assertEquals(1, host.getMembers().get(1).getSlotIndex());
        Assertions.assertEquals(0, host.getMembers().getFirst().getSlotIndex(), "the host keeps its slot");
        Assertions.assertThrows(IllegalStateException.class, () -> host.requestSlot(1), "the host does not request a slot");
    }

    @Test
    void a_joiner_with_another_mod_identity_is_refused_with_the_first_differing_part() throws IOException, ManifestException {
        ModIdentity identity = createIdentity("1.0.0", 1);
        NetworkLobby host = this.host(identity, this.open().getLocalAddress(), new int[]{SCOUT_SLOT}, 3);
        NetworkLobby newer = this.join(host, createIdentity("1.0.1", 1), this.hostMaps, this.open().getLocalAddress().getPort());
        NetworkLobby changed = this.join(host, createIdentity("1.0.0", 2), this.hostMaps, this.open().getLocalAddress().getPort());

        this.pollUntil(() -> newer.getLobbyState() == LobbyState.REFUSED && changed.getLobbyState() == LobbyState.REFUSED, host, newer, changed);

        Assertions.assertEquals(JoinRefusal.MOD_VERSION, newer.getRefusal());
        Assertions.assertEquals(JoinRefusal.MOD_CONTENT, changed.getRefusal());
        Assertions.assertTrue(newer.getRefusalText().startsWith("The host runs duel 1.0.0 (engine 0.1"), newer.getRefusalText());
        Assertions.assertEquals(1, host.getMembers().size());
    }

    @Test
    void a_full_room_refuses_a_joiner_and_a_leaving_joiner_frees_its_place() throws IOException, ManifestException {
        ModIdentity identity = createIdentity("1.0.0", 1);
        NetworkLobby host = this.host(identity, this.open().getLocalAddress(), new int[]{SCOUT_SLOT}, 2);
        NetworkLobby first = this.join(host, identity, this.hostMaps, this.open().getLocalAddress().getPort());

        this.pollUntil(() -> first.getLobbyState() == LobbyState.OPEN, host, first);

        NetworkLobby second = this.join(host, identity, this.hostMaps, this.open().getLocalAddress().getPort());

        this.pollUntil(() -> second.getLobbyState() == LobbyState.REFUSED, host, first, second);

        Assertions.assertEquals(JoinRefusal.ROOM_FULL, second.getRefusal());

        first.close();
        this.pollUntil(() -> host.getMembers().size() == 1, host);

        NetworkLobby third = this.join(host, identity, this.hostMaps, this.open().getLocalAddress().getPort());

        this.pollUntil(() -> third.getLobbyState() == LobbyState.OPEN && third.getMembers().size() == 2, host, third);

        LobbyMember thirdMember = third.getLocalMember();

        Assertions.assertNotNull(thirdMember);
        Assertions.assertEquals(2, thirdMember.getPeer().getNumber());
    }

    @Test
    void a_joiner_whose_connection_is_refused_closes_its_lobby_and_later_polls_do_nothing() throws IOException, ManifestException {
        InetSocketAddress unused;

        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(LOOPBACK);
            unused = (InetSocketAddress) server.getLocalAddress();
        }

        NetworkLobby joiner = NetworkLobby.join(unused, this.open().getLocalAddress().getPort(), "joiner", createIdentity("1.0.0", 1), this.codec,
                                                this.hostMaps);

        this.lobbies.add(joiner);
        this.pollUntil(() -> joiner.getLobbyState() == LobbyState.CLOSED, joiner);
        joiner.poll();

        Assertions.assertEquals(LobbyState.CLOSED, joiner.getLobbyState());
        Assertions.assertFalse(joiner.getRefusalText().isEmpty());
    }

    private @NotNull LockstepGameScenario.Machine createMachine(@NotNull NetworkGameStart start, @NotNull UdpLockstepTransport transport, int orderFrame) throws IOException {
        Game game = this.scenario.createGame(start.getSetup());

        return this.scenario.createMachine(game, listener -> start.createSession(game, transport, this.scenario.getCommands(), null, listener),
                                           orderFrame);
    }

    private @NotNull List<@NotNull Long> runInMemory() throws IOException {
        LockstepPeer first = new LockstepPeer(0);
        LockstepPeer second = new LockstepPeer(1);
        InMemoryLockstepNetwork network = new InMemoryLockstepNetwork(List.of(first, second));
        List<LockstepGameScenario.Machine> machines = List.of(
                this.scenario.createMachine(first, network.getTransport(first), 2, LockstepGameScenario.SEED, PATROL_FRAME),
                this.scenario.createMachine(second, network.getTransport(second), 2, LockstepGameScenario.SEED, NO_ORDER)
        );

        LockstepGameScenario.run(machines, FRAME_COUNT, () -> {
        });

        return machines.getFirst().getFrameHashes();
    }

    private @NotNull GameSetup createDuelSetup(@NotNull MapReference map) {
        return this.createDuelSetup(map, 1);
    }

    /**
     * Creates the setup of a game of the two members on slots 0 and 1; the joiner's slot 1 gets the faction of the given slot.
     */
    private @NotNull GameSetup createDuelSetup(@NotNull MapReference map, int joinerFactionSlot) {
        return new GameSetup.Builder(map)
                .addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, this.scenario.getScenario().getWardens()))
                .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, joinerFactionSlot == 0 ? this.scenario.getScenario().getWardens()
                                                                                                : this.scenario.getScenario().getRaiders()))
                .setSeed(LockstepGameScenario.SEED)
                .build();
    }

    private @NotNull TurnSchedule createSchedule() {
        return new TurnSchedule(LockstepGameScenario.FRAMES_PER_TURN, LockstepGameScenario.LATENCY_TURNS);
    }

    private @Nullable LobbyMapState findMapState(@NotNull NetworkLobby lobby, int peerNumber) {
        for (LobbyMember member : lobby.getMembers()) {
            if (member.getPeer().getNumber() == peerNumber) {
                return member.getMapState();
            }
        }

        return null;
    }

    private @NotNull NetworkLobby host(@NotNull ModIdentity identity, @NotNull InetSocketAddress gameAddress, int @NotNull [] openSlots, int capacity) throws IOException {
        NetworkLobby lobby = NetworkLobby.host(LOOPBACK, gameAddress, "host", identity, this.codec, this.hostMaps, this.twinRocks, openSlots, capacity);

        this.lobbies.add(lobby);

        return lobby;
    }

    private @NotNull NetworkLobby join(@NotNull NetworkLobby host, @NotNull ModIdentity identity, @NotNull LobbyMaps maps, int gamePort) throws IOException {
        NetworkLobby lobby = NetworkLobby.join(host.getLocalAddress(), gamePort, "joiner " + this.lobbies.size(), identity, this.codec, maps);

        this.lobbies.add(lobby);

        return lobby;
    }

    private @NotNull DatagramEndpoint open() throws IOException {
        DatagramEndpoint endpoint = UdpDatagramEndpoint.open(LOOPBACK);

        this.endpoints.add(endpoint);

        return endpoint;
    }

    private void pollUntil(@NotNull Condition condition, @NotNull NetworkLobby @NotNull ... lobbies) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);

        while (!condition.isMet() && System.nanoTime() < deadline) {
            for (NetworkLobby lobby : lobbies) {
                lobby.poll();
            }

            Thread.onSpinWait();
        }

        Assertions.assertTrue(condition.isMet(), "The lobbies did not answer in time.");
    }
}
