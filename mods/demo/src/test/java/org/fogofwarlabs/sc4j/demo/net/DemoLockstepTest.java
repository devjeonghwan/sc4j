package org.fogofwarlabs.sc4j.demo.net;

import org.fogofwarlabs.sc4j.beaconlink.client.BeaconLinkClient;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.test.client.ClientScenario;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.demo.client.DemoClient;
import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.demo.game.DemoGameScenario;
import org.fogofwarlabs.sc4j.demo.game.DemoGames;
import org.fogofwarlabs.sc4j.demo.map.DemoMaps;
import org.fogofwarlabs.sc4j.demo.order.DemoOrders;
import org.fogofwarlabs.sc4j.engine.client.game.GameSession;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.fogofwarlabs.sc4j.engine.lockstep.InMemoryLockstepNetwork;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepPeer;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepSession;
import org.fogofwarlabs.sc4j.engine.lockstep.TurnSchedule;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.NetworkClock;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.FoundRoom;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.RoomSearch;
import org.fogofwarlabs.sc4j.engine.net.lobby.LobbyMapState;
import org.fogofwarlabs.sc4j.engine.net.lobby.LobbyMember;
import org.fogofwarlabs.sc4j.engine.net.lobby.LobbyState;
import org.fogofwarlabs.sc4j.engine.net.lobby.NetworkGameStart;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommand;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.replay.EngineReplay;
import org.fogofwarlabs.sc4j.engine.replay.EngineReplayFormat;
import org.fogofwarlabs.sc4j.engine.replay.ReplayRecorder;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Tests the demo's two-player lockstep games: two machines over the engine's in-memory network, two machines over the UDP transport on
 * 127.0.0.1, and machines over Beacon.Link on 127.0.0.1 behind emulated NATs (two players with a direct channel; two players and an observer
 * where the symmetric pair goes through the host), started from the demo's lobby with the joiner's faction choice, play the same game to its end
 * (the same state hash after every game loop, the same outcome, no desync); the person at each machine sends a worker to scout through the
 * turns, each order runs on every machine at the same game loop and changes the game (the state hashes leave those of the same game without
 * the orders at that loop), and every machine records the same replay bytes, which play back with every checkpoint. Two people who train a
 * worker and cancel it run both on both machines alike: the same state hashes and replay, the gold back and no worker made.
 */
final class DemoLockstepTest {
    private static final long SEED = 20261108L;
    private static final int CHECKPOINT_INTERVAL = 100;
    private static final int ORDER_LOOP = 240;
    private static final int PERSON_COUNT = 2;
    private static final long NOT_RUN = -1L;
    private static final long WAIT_SECONDS = 120L;
    private static final long LINK_MILLIS = 60_000L;
    private static final long LINK_STEP_MILLIS = 100L;
    private static final int QUEUE_TRAIN_LOOP = 120;
    private static final int QUEUE_CANCEL_LOOP = 140;
    private static final int QUEUE_GAME_LOOPS = 400;
    private static final int START_WORKERS = 4;
    /**
     * The commands each person submits in the queue game: one training and one cancel.
     */
    private static final int QUEUE_COMMANDS_PER_PERSON = 2;
    private static final @NotNull String @NotNull [] TOWN_HALLS = {"hearthstead", "ember-lodge"};
    private static final @NotNull String @NotNull [] WORKERS = {"tiller", "grubber"};

    /**
     * Holds one machine of a game in this JVM: its game session, the recorder, the state hash after every loop it ran and, for the person of
     * each slot, the scout order and the game loop at which this machine ran it.
     */
    private static final class Machine {
        private final @NotNull Game game;
        private final @NotNull GameSession session;
        private final @NotNull ReplayRecorder recorder;

        private final @NotNull List<@NotNull Long> frameHashes;
        private final @Nullable IssueOrderCommand @NotNull [] scoutOrders;
        private final long @NotNull [] scoutLoops;

        private boolean ordered;
        private boolean trained;
        private boolean cancelled;

        private Machine(@NotNull Game game, @NotNull GameSession session, @NotNull ReplayRecorder recorder) {
            this.game     = game;
            this.session  = session;
            this.recorder = recorder;

            this.frameHashes = new ArrayList<>();
            this.scoutOrders = new IssueOrderCommand[PERSON_COUNT];
            this.scoutLoops  = new long[PERSON_COUNT];

            this.ordered   = false;
            this.trained   = false;
            this.cancelled = false;

            Arrays.fill(this.scoutLoops, NOT_RUN);
        }

        /**
         * Runs the next loop if its turns arrived; the person sends a worker to scout once at the order loop.
         */
        private void step(@NotNull DemoGames games, @NotNull DemoOrders orders, @NotNull DemoNetworkGame network) {
            if (this.isDone()) {
                network.poll();

                return;
            }

            this.order(games, orders);

            if (network.runFrame()) {
                this.recordFrame(orders);
            }
        }

        private void step(@NotNull DemoGames games, @NotNull DemoOrders orders) {
            if (this.isDone()) {
                return;
            }

            this.order(games, orders);

            if (this.session.runFrame()) {
                this.recordFrame(orders);
            }
        }

        /**
         * Creates the scout order of the person of each slot at the order loop, the same on every machine, and submits the local person's.
         */
        private void order(@NotNull DemoGames games, @NotNull DemoOrders orders) {
            Player person = this.session.getLocalPlayer();
            CommandSink sink = this.session.getCommandSink();

            if (this.ordered || person == null || sink == null || this.game.getClock().getFrame() != ORDER_LOOP) {
                return;
            }

            for (int slot = 0; slot < PERSON_COUNT; slot++) {
                this.scoutOrders[slot] = games.createScoutOrder(this.game, this.game.getPlayer(slot));
            }

            IssueOrderCommand command = this.scoutOrders[person.getIndex()];

            Assertions.assertNotNull(command, "the person has a worker and the map another start location");
            sink.submit(person, orders.getOrderCommand(), command);
            this.ordered = true;
        }

        /**
         * Records the state hash of the loop that ran and the loop at which each scout first runs its move order to the scout target.
         */
        private void recordFrame(@NotNull DemoOrders orders) {
            this.frameHashes.add(this.game.computeStateHash());

            for (int slot = 0; slot < PERSON_COUNT; slot++) {
                IssueOrderCommand command = this.scoutOrders[slot];
                Unit scout = command == null ? null : this.game.getUnits().resolveHandle(command.getUnitHandle());

                if (this.scoutLoops[slot] == NOT_RUN && scout != null && scout.getOrderType() == orders.getMove() &&
                    scout.getOrderTargetX() == command.getTargetX() && scout.getOrderTargetY() == command.getTargetY()) {
                    this.scoutLoops[slot] = this.game.getClock().getFrame();
                }
            }
        }

        private boolean isDone() {
            return DemoGames.isOver(this.game) || this.game.getClock().getFrame() >= DemoGameScenario.LOOP_LIMIT;
        }

        /**
         * Runs the next loop up to the queue game's last loop if its turns arrived; the person trains a worker at the town hall at the train
         * loop and cancels the front queue entry at the cancel loop.
         */
        private void stepQueue(@NotNull DemoContent content) {
            Player person = this.session.getLocalPlayer();
            CommandSink sink = this.session.getCommandSink();
            int loop = this.game.getClock().getFrame();

            if (loop >= QUEUE_GAME_LOOPS || person == null || sink == null) {
                return;
            }

            DemoOrders orders = content.getOrders();
            Unit hall = DemoGameScenario.findUnit(this.game, person, TOWN_HALLS[person.getIndex()]);

            Assertions.assertNotNull(hall, "the person has a town hall");

            if (!this.trained && loop == QUEUE_TRAIN_LOOP) {
                sink.submit(person, orders.getTrainCommand(),
                            orders.getTrainCommand().create(this.game, hall, content.requireUnitType(WORKERS[person.getIndex()])));
                this.trained = true;
            }

            if (!this.cancelled && loop == QUEUE_CANCEL_LOOP) {
                sink.submit(person, orders.getCancelCommand(), orders.getCancelCommand().create(this.game, hall, 0));
                this.cancelled = true;
            }

            if (this.session.runFrame()) {
                this.frameHashes.add(this.game.computeStateHash());
            }
        }
    }

    private static void waitUntil(@NotNull BooleanSupplier condition, @NotNull Runnable round) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);

        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            round.run();
        }

        Assertions.assertTrue(condition.getAsBoolean(), "the machines did not get there in time");
    }

    private static void assertSameGame(@NotNull DemoGameScenario scenario, @NotNull EngineReplayFormat format, @NotNull GameSetup setup,
                                       @NotNull Machine first, @NotNull Machine second) throws IOException {
        byte[] firstReplay = format.write(first.recorder.createReplay());
        byte[] secondReplay = format.write(second.recorder.createReplay());

        Assertions.assertTrue(DemoGames.isOver(first.game), "the game is decided");
        Assertions.assertEquals(first.frameHashes, second.frameHashes, "the same state hash after every game loop");
        Assertions.assertEquals(DemoGames.getOutcome(first.game, first.game.getPlayer(DemoGames.FIRST_SLOT)),
                                DemoGames.getOutcome(second.game, second.game.getPlayer(DemoGames.FIRST_SLOT)));
        Assertions.assertArrayEquals(firstReplay, secondReplay, "both machines record the same replay");

        EngineReplay replay = format.read(firstReplay);
        DemoGameScenario.Outcome playedBack = scenario.playBack(replay.getSetup(), replay.createController(null));

        Assertions.assertEquals(PERSON_COUNT, replay.getCommandCount(), "the scout orders of both people");
        Assertions.assertEquals(first.frameHashes, playedBack.getFrameHashes(), "the replay plays the game back");
        assertOrdersChangeTheGame(scenario, setup, first, second);
    }

    /**
     * Asserts that each person's scout order ran on both machines at the same game loop within the turn latency after it was submitted, and
     * that the game leaves the same game without the orders exactly at the first of those loops.
     */
    private static void assertOrdersChangeTheGame(@NotNull DemoGameScenario scenario, @NotNull GameSetup setup, @NotNull Machine first,
                                                  @NotNull Machine second) {
        TurnSchedule schedule = DemoNetworkSettings.ANY_PORTS.getSchedule();
        long latestLoop = ORDER_LOOP + (long) schedule.getFramesPerTurn() * (schedule.getLatencyTurns() + 1);
        List<Long> withoutOrders = scenario.play(setup, List.of(), null).getFrameHashes();
        long firstOrderLoop = Long.MAX_VALUE;

        Assertions.assertArrayEquals(first.scoutLoops, second.scoutLoops, "each scout order runs at the same game loop on both machines");

        for (long loop : first.scoutLoops) {
            Assertions.assertTrue(loop > ORDER_LOOP && loop <= latestLoop, "a scout order ran at game loop " + loop);
            firstOrderLoop = Math.min(firstOrderLoop, loop);
        }

        int firstDifference = 0;

        while (firstDifference < first.frameHashes.size() && firstDifference < withoutOrders.size() &&
               first.frameHashes.get(firstDifference).equals(withoutOrders.get(firstDifference))) {
            firstDifference++;
        }

        Assertions.assertEquals(firstOrderLoop, firstDifference + 1L, "the game without the orders differs from the loop of the first order on");
    }

    @Test
    void two_machines_over_the_in_memory_network_play_the_same_game_and_record_the_same_replay() throws IOException, ManifestException {
        try (DemoGameScenario scenario = new DemoGameScenario()) {
            DemoGames games = scenario.getGames();
            MapReference map = scenario.getMaps().requireReference(DemoMaps.ASHEN_FORD);
            EngineReplayFormat format = new EngineReplayFormat(DemoGameScenario.createIdentity(), scenario.getContent().getOrders().getCommands(),
                                                               scenario.getContent().createSetupCodec());
            DemoOrders orders = scenario.getContent().getOrders();
            List<LockstepPeer> peers = List.of(new LockstepPeer(0), new LockstepPeer(1));
            List<LobbyMember> members = List.of(new LobbyMember(peers.get(0), "first", DemoGames.FIRST_SLOT),
                                                new LobbyMember(peers.get(1), "second", DemoGames.SECOND_SLOT));
            GameSetup setup = games.createNetworkSetup(map, members, ControllerKind.COMPUTER, SEED);
            InMemoryLockstepNetwork network = new InMemoryLockstepNetwork(peers);
            List<Machine> machines = new ArrayList<>();

            for (LockstepPeer peer : peers) {
                Game game = games.createGame(setup);
                List<Player> peerPlayers = List.of(game.getPlayer(DemoGames.FIRST_SLOT), game.getPlayer(DemoGames.SECOND_SLOT));
                LockstepSession lockstep = new LockstepSession(network.getTransport(peer), scenario.getContent().getOrders().getCommands(), peer,
                                                               peerPlayers, DemoNetworkSettings.ANY_PORTS.getSchedule(), null, null);
                GameSession session = DemoClient.createSessionFactory(scenario.getContent(), game, scenario.getMaps().require(setup))
                                                .createNetwork(game, peerPlayers.get(peer.getNumber()), lockstep, List.of());
                ReplayRecorder recorder = format.createRecorder(setup, CHECKPOINT_INTERVAL);

                session.getCommandSource().setRecorder(recorder);
                machines.add(new Machine(game, session, recorder));
            }

            waitUntil(() -> machines.get(0).isDone() && machines.get(1).isDone(), () -> {
                machines.get(0).step(games, orders);
                machines.get(1).step(games, orders);
            });

            assertSameGame(scenario, format, setup, machines.get(0), machines.get(1));
        }
    }

    @Test
    void two_people_over_the_in_memory_network_train_and_cancel_a_worker_alike_on_both_machines() throws IOException, ManifestException {
        try (DemoGameScenario scenario = new DemoGameScenario()) {
            DemoGames games = scenario.getGames();
            DemoContent content = scenario.getContent();
            MapReference map = scenario.getMaps().requireReference(DemoMaps.ASHEN_FORD);
            EngineReplayFormat format = new EngineReplayFormat(DemoGameScenario.createIdentity(), content.getOrders().getCommands(),
                                                               content.createSetupCodec());
            List<LockstepPeer> peers = List.of(new LockstepPeer(0), new LockstepPeer(1));
            List<LobbyMember> members = List.of(new LobbyMember(peers.get(0), "first", DemoGames.FIRST_SLOT),
                                                new LobbyMember(peers.get(1), "second", DemoGames.SECOND_SLOT));
            GameSetup setup = games.createNetworkSetup(map, members, ControllerKind.HUMAN, SEED);
            InMemoryLockstepNetwork network = new InMemoryLockstepNetwork(peers);
            List<Machine> machines = new ArrayList<>();

            for (LockstepPeer peer : peers) {
                Game game = games.createGame(setup);
                List<Player> peerPlayers = List.of(game.getPlayer(DemoGames.FIRST_SLOT), game.getPlayer(DemoGames.SECOND_SLOT));
                LockstepSession lockstep = new LockstepSession(network.getTransport(peer), content.getOrders().getCommands(), peer, peerPlayers,
                                                               DemoNetworkSettings.ANY_PORTS.getSchedule(), null, null);
                GameSession session = DemoClient.createSessionFactory(content, game, scenario.getMaps().require(setup))
                                                .createNetwork(game, peerPlayers.get(peer.getNumber()), lockstep, List.of());
                ReplayRecorder recorder = format.createRecorder(setup, CHECKPOINT_INTERVAL);

                session.getCommandSource().setRecorder(recorder);
                machines.add(new Machine(game, session, recorder));
            }

            waitUntil(() -> machines.stream().allMatch(machine -> machine.game.getClock().getFrame() >= QUEUE_GAME_LOOPS), () -> {
                machines.get(0).stepQueue(content);
                machines.get(1).stepQueue(content);
            });

            Machine first = machines.get(0);
            Machine second = machines.get(1);
            byte[] firstReplay = format.write(first.recorder.createReplay());
            EngineReplay replay = format.read(firstReplay);

            Assertions.assertEquals(first.frameHashes, second.frameHashes, "the same state hash after every game loop");
            Assertions.assertArrayEquals(firstReplay, format.write(second.recorder.createReplay()), "both machines record the same replay");
            Assertions.assertEquals(QUEUE_COMMANDS_PER_PERSON * PERSON_COUNT, replay.getCommandCount(),
                                    "the training and cancel commands of both people");
            Assertions.assertEquals(first.frameHashes, scenario.playBack(replay.getSetup(), replay.createController(null)).getFrameHashes(),
                                    "the replay plays the game back");

            for (Machine machine : machines) {
                for (int slot = 0; slot < PERSON_COUNT; slot++) {
                    Player player = machine.game.getPlayer(slot);
                    Unit hall = DemoGameScenario.findUnit(machine.game, player, TOWN_HALLS[slot]);
                    int workers = machine.game.getContent().getProgress().getUnitCount(player, content.requireUnitType(WORKERS[slot]));

                    Assertions.assertNotNull(hall);
                    Assertions.assertEquals(0, machine.game.getProduction().getQueueLength(hall), "the queue is empty");
                    Assertions.assertEquals(DemoGames.START_GOLD, machine.game.getEconomy().getAmount(player, content.getGold()), "the gold is back");
                    Assertions.assertEquals(START_WORKERS, workers, "no worker is made");
                }
            }
        }
    }

    @Test
    void two_machines_over_udp_loopback_start_from_the_lobby_and_play_the_same_game_to_its_end() throws IOException, ManifestException {
        try (DemoGameScenario scenario = new DemoGameScenario()) {
            DemoGames games = scenario.getGames();
            DemoNetworkSettings settings = DemoNetworkSettings.ANY_PORTS;
            DemoOrders orders = scenario.getContent().getOrders();
            ModIdentity identity = DemoGameScenario.createIdentity();
            MapReference map = scenario.getMaps().requireReference(DemoMaps.ASHEN_FORD);
            EngineReplayFormat format = new EngineReplayFormat(identity, scenario.getContent().getOrders().getCommands(),
                                                               scenario.getContent().createSetupCodec());

            try (DemoLobby host = DemoLobby.host(settings, identity, scenario.getContent().createSetupCodec(), scenario.getMaps(), "host",
                                                 DemoLobby.createRoomName("host"), map, NetworkClock.SYSTEM);
                 DemoLobby joiner = DemoLobby.join(settings, identity, scenario.getContent().createSetupCodec(), scenario.getMaps(), "joiner",
                                                   host.getLobby().getLocalAddress())) {
                Runnable poll = () -> {
                    try {
                        host.poll();
                        joiner.poll();
                    } catch (IOException exception) {
                        Assertions.fail(exception);
                    }
                };

                waitUntil(() -> {
                    LobbyMember local = joiner.getLobby().getLocalMember();

                    return local != null && local.getMapState() == LobbyMapState.PRESENT;
                }, poll);
                joiner.getLobby().setFaction(scenario.getContent().getCindermaw());
                joiner.getLobby().setReady(true);
                host.getLobby().setFaction(scenario.getContent().getDawnhold());
                host.getLobby().setReady(true);
                waitUntil(host.getLobby()::isReadyToStart, poll);

                NetworkGameStart hostStart = host.start(games, ControllerKind.COMPUTER, SEED);

                waitUntil(() -> joiner.getLobby().getLobbyState() == LobbyState.STARTED, poll);

                NetworkGameStart joinerStart = joiner.getLobby().getStart();

                Assertions.assertNotNull(joinerStart);
                Assertions.assertSame(scenario.getContent().getCindermaw(),
                                      joinerStart.getSetup().getPlayers().get(DemoGames.SECOND_SLOT).getFaction(), "the joiner's faction choice");

                try (DemoNetworkGame hostGame = new DemoNetworkGame(hostStart, host, NetworkClock.SYSTEM, settings,
                                                                    scenario.getContent(), scenario.getMaps(), games, format, CHECKPOINT_INTERVAL);
                     DemoNetworkGame joinerGame = new DemoNetworkGame(joinerStart, joiner, NetworkClock.SYSTEM, settings,
                                                                      scenario.getContent(), scenario.getMaps(), games, format, CHECKPOINT_INTERVAL)) {
                    Machine first = new Machine(hostGame.getGame(), hostGame.getSession(), hostGame.getRecorder());
                    Machine second = new Machine(joinerGame.getGame(), joinerGame.getSession(), joinerGame.getRecorder());

                    waitUntil(() -> first.isDone() && second.isDone(), () -> {
                        first.step(games, orders, hostGame);
                        second.step(games, orders, joinerGame);
                    });

                    hostGame.leave();
                    joinerGame.leave();
                    waitUntil(() -> hostGame.isDepartureComplete() && joinerGame.isDepartureComplete(), () -> {
                        hostGame.poll();
                        joinerGame.poll();
                    });

                    Assertions.assertEquals(0, hostGame.getLockstep().getDesyncMonitor().getMismatchCount());
                    Assertions.assertEquals(0, joinerGame.getLockstep().getDesyncMonitor().getMismatchCount());
                    assertSameGame(scenario, format, hostStart.getSetup(), first, second);
                }
            }
        }
    }

    /**
     * Opens a Beacon.Link client behind an emulated NAT of the scenario and waits until it registers at the node.
     */
    private static @NotNull BeaconLinkClient openLinkClient(@NotNull ClientScenario link, @NotNull PeerSocket node, @NotNull NatType type,
                                                            @NotNull ModIdentity identity) throws IOException {
        BeaconLinkClient client = link.openClient(identity, List.of(ClientScenario.listed(node)), link.openNat(type).openEndpoint());

        link.await(() -> client.getObservedAddress() != null, "the client registers");

        return client;
    }

    /**
     * Searches the rooms of the demo with a client until a search finds one.
     */
    private static @NotNull FoundRoom findLinkRoom(@NotNull ClientScenario link, @NotNull BeaconLinkClient client) throws IOException {
        FoundRoom[] found = new FoundRoom[1];
        RoomSearch[] search = {client.searchRooms()};

        link.advanceUntil(() -> {
            if (search[0].isComplete() && !search[0].getRooms().isEmpty()) {
                found[0] = search[0].getRooms().getFirst();
            } else if (search[0].isComplete()) {
                search[0] = client.searchRooms();
            }

            return found[0] != null;
        }, LINK_MILLIS, LINK_STEP_MILLIS, "a search finds the room");

        return found[0];
    }

    /**
     * Plays a demo network game over Beacon.Link through emulated NATs: the host publishes the room, every joiner finds it and joins it (the
     * first joiner plays the second slot, a later one observes), all of them reach the map, readiness and the channels to each other, the host
     * starts, and every machine plays the game to its end and leaves it.
     */
    private static void playOverBeaconLink(@NotNull NatType hostType, @NotNull NatType @NotNull [] joinerTypes, boolean relayExpected) throws IOException,
                                                                                                                                              ManifestException {
        try (DemoGameScenario scenario = new DemoGameScenario();
             ClientScenario link = new ClientScenario()) {
            DemoGames games = scenario.getGames();
            DemoNetworkSettings settings = DemoNetworkSettings.ANY_PORTS;
            DemoOrders orders = scenario.getContent().getOrders();
            ModIdentity identity = DemoGameScenario.createIdentity();
            MapReference map = scenario.getMaps().requireReference(DemoMaps.ASHEN_FORD);
            EngineReplayFormat format = new EngineReplayFormat(identity, orders.getCommands(), scenario.getContent().createSetupCodec());
            NetworkClock lobbyClock = link.getClock()::getEpochMillis;
            PeerSocket node = link.openNode(ClientScenario.createNodeConfiguration());
            BeaconLinkClient hostClient = openLinkClient(link, node, hostType, identity);
            List<DemoLobby> rooms = new ArrayList<>();

            rooms.add(DemoLobby.host(hostClient, identity, scenario.getContent().createSetupCodec(),
                                     scenario.getMaps(), "host", DemoLobby.createRoomName("host"), map,
                                     joinerTypes.length + 1, DemoNetworkSettings.DEFAULT_TURN_RATE, "", lobbyClock));

            ClientScenario.PollAction pollRooms = () -> {
                for (DemoLobby room : rooms) {
                    room.poll();
                }
            };

            link.addPollAction(pollRooms);

            for (NatType joinerType : joinerTypes) {
                BeaconLinkClient joinerClient = openLinkClient(link, node, joinerType, identity);
                DemoLobby joiner = DemoLobby.join(joinerClient, findLinkRoom(link, joinerClient), identity, scenario.getContent().createSetupCodec(),
                                                  scenario.getMaps(), "joiner " + rooms.size(), "", lobbyClock);

                rooms.add(joiner);
                link.advanceUntil(() -> {
                    LobbyMember local = joiner.getLobby().getLocalMember();

                    return local != null && local.getMapState() == LobbyMapState.PRESENT;
                }, LINK_MILLIS, LINK_STEP_MILLIS, "the joiner has the map");
            }

            DemoLobby host = rooms.getFirst();

            rooms.get(1).getLobby().setFaction(scenario.getContent().getCindermaw());
            host.getLobby().setFaction(scenario.getContent().getDawnhold());

            for (DemoLobby room : rooms) {
                room.getLobby().setReady(true);
            }

            link.advanceUntil(host.getLobby()::isReadyToStart, LINK_MILLIS, LINK_STEP_MILLIS, "every member is ready and connected");

            List<NetworkGameStart> starts = new ArrayList<>(List.of(host.start(games, ControllerKind.COMPUTER, SEED)));

            link.advanceUntil(() -> rooms.stream().allMatch(room -> room.getLobby().getLobbyState() == LobbyState.STARTED), LINK_MILLIS,
                              LINK_STEP_MILLIS, "the start reaches every member");
            link.removePollAction(pollRooms);

            for (DemoLobby joiner : rooms.subList(1, rooms.size())) {
                NetworkGameStart start = joiner.getLobby().getStart();

                Assertions.assertNotNull(start);
                Assertions.assertTrue(start.isOverBeaconLink());
                starts.add(start);
            }

            List<DemoNetworkGame> networks = new ArrayList<>();

            try {
                for (int index = 0; index < rooms.size(); index++) {
                    networks.add(new DemoNetworkGame(starts.get(index), rooms.get(index), NetworkClock.SYSTEM, settings, scenario.getContent(),
                                                     scenario.getMaps(), games, format, CHECKPOINT_INTERVAL));
                }

                List<Machine> machines = new ArrayList<>();

                for (DemoNetworkGame network : networks) {
                    machines.add(new Machine(network.getGame(), network.getSession(), network.getRecorder()));
                }

                long gameStart = System.nanoTime();
                long protocolStart = link.getClock().getEpochMillis();

                waitUntil(() -> machines.stream().allMatch(Machine::isDone), () -> {
                    for (int index = 0; index < machines.size(); index++) {
                        machines.get(index).step(games, orders, networks.get(index));
                    }

                    // The protocol clock follows real time while the game runs, so that the relay's budget refills as it would.
                    long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - gameStart);

                    link.getClock().advance(protocolStart + elapsed - link.getClock().getEpochMillis());
                    pollLink(link);
                });

                for (DemoNetworkGame network : networks) {
                    network.leave();
                }

                waitUntil(() -> networks.stream().allMatch(DemoNetworkGame::isDepartureComplete), () -> {
                    for (DemoNetworkGame network : networks) {
                        network.poll();
                    }

                    pollLink(link);
                });

                for (DemoNetworkGame network : networks) {
                    Assertions.assertEquals(0, network.getLockstep().getDesyncMonitor().getMismatchCount());
                }

                assertSameGame(scenario, format, starts.getFirst().getSetup(), machines.get(0), machines.get(1));

                for (Machine observer : machines.subList(PERSON_COUNT, machines.size())) {
                    Assertions.assertNull(observer.session.getLocalPlayer(), "a later joiner observes");
                    Assertions.assertEquals(machines.getFirst().frameHashes, observer.frameHashes, "the observer runs the same game");
                    Assertions.assertArrayEquals(format.write(machines.getFirst().recorder.createReplay()),
                                                 format.write(observer.recorder.createReplay()), "the observer records the same replay");
                }

                Assertions.assertEquals(relayExpected, hostClient.getRelayedCount() > 0, "the host relays for a pair without a direct path");
            } finally {
                for (DemoNetworkGame network : networks) {
                    network.close();
                }

                for (DemoLobby room : rooms.subList(networks.size(), rooms.size())) {
                    room.close();
                }
            }
        }
    }

    private static void pollLink(@NotNull ClientScenario link) {
        try {
            link.pollAll();
        } catch (IOException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    void two_machines_over_beacon_link_behind_nats_start_from_the_room_and_play_the_same_game_to_its_end() throws IOException,
                                                                                                                  ManifestException {
        playOverBeaconLink(NatType.PORT_RESTRICTED, new NatType[]{NatType.ADDRESS_RESTRICTED}, false);
    }

    @Test
    void three_machines_over_beacon_link_play_the_same_game_with_the_symmetric_pair_through_the_host() throws IOException, ManifestException {
        playOverBeaconLink(NatType.FULL_CONE, new NatType[]{NatType.SYMMETRIC, NatType.SYMMETRIC}, true);
    }
}
