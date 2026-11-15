package org.fogofwarlabs.sc4j.demo;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.demo.game.DemoGames;
import org.fogofwarlabs.sc4j.demo.map.DemoMaps;
import org.fogofwarlabs.sc4j.demo.net.DemoLobby;
import org.fogofwarlabs.sc4j.demo.net.DemoNetworkGame;
import org.fogofwarlabs.sc4j.demo.net.DemoNetworkSettings;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.controller.ControllerCommandSource;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.mod.HeadlessRunner;
import org.fogofwarlabs.sc4j.engine.mod.ModException;
import org.fogofwarlabs.sc4j.engine.mod.ModLaunch;
import org.fogofwarlabs.sc4j.engine.mod.ModStorage;
import org.fogofwarlabs.sc4j.engine.net.NetworkClock;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.*;
import org.fogofwarlabs.sc4j.engine.net.lobby.*;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommand;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.replay.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Runs the demo headless for the runtime ({@code --headless}), without window, audio and input:
 * <ul>
 *     <li>without {@code --replay}, {@code --join} and {@value #HOST_ARGUMENT}, it plays a game of two computer players to its end and writes
 *     the replay to {@value #REPLAY_PATH} in the mod storage;</li>
 *     <li>with {@value #HOST_ARGUMENT} or the runtime's {@code --join}, it plays a network game of two machines on the loopback address: the
 *     host opens a room on any free port and writes its lobby port to the given file, the joiner joins the room at the address
 *     {@code --join} names; each machine's slot is a computer player every machine runs, and the person at each machine sends its first worker
 *     to scout the other start location at game loop {@value #NETWORK_ORDER_LOOP}, a command that travels with the turns and runs on both
 *     machines at the same game loop. Each machine writes the replay to
 *     {@value #NETWORK_REPLAY_PATH};</li>
 *     <li>with {@value #BEACON_LINK_HOST_ARGUMENT} or {@value #BEACON_LINK_JOIN_ARGUMENT}, it plays the same network game over Beacon.Link:
 *     both machines open a Beacon.Link session with the nodes of the runtime's command line ({@code --seed host:port}; a node on the loopback
 *     address keeps every socket on it), the host publishes a room, the joiner searches the rooms of the demo until it finds one and joins it;
 *     the lobby and the game go over the overlay's channels;</li>
 *     <li>with {@code --replay}, it plays a replay of the engine format back and fails when the replayed game misses a state hash checkpoint
 *     of the recording.</li>
 * </ul>
 * The demo's own arguments (after {@code --}): {@code --map <name>} (default {@value DemoMaps#ASHEN_FORD}), {@code --seed <number>},
 * {@value #HOST_ARGUMENT} {@code <file>}, {@code --frame-hashes <file>}, which writes the state hash after every game loop as one
 * hexadecimal number per line, so that two runs can be compared, and {@value #OUTCOME_ARGUMENT} {@code <file>}, which writes the game loops
 * and every player's outcome.
 */
public final class DemoHeadlessRunner implements HeadlessRunner {
    /**
     * The storage path of the replay of the last played game.
     */
    public static final @NotNull String REPLAY_PATH = "replays/last-game." + EngineReplayFormat.FILE_EXTENSION;
    /**
     * The storage path of the replay of the last played network game.
     */
    public static final @NotNull String NETWORK_REPLAY_PATH = "replays/last-network-game." + EngineReplayFormat.FILE_EXTENSION;
    /**
     * The argument that names the map.
     */
    public static final @NotNull String MAP_ARGUMENT = "--map";
    /**
     * The argument that gives the seed.
     */
    public static final @NotNull String SEED_ARGUMENT = "--seed";
    /**
     * The argument that names the file of the per-loop state hashes.
     */
    public static final @NotNull String FRAME_HASHES_ARGUMENT = "--frame-hashes";
    /**
     * The argument that names the file of the game's outcome.
     */
    public static final @NotNull String OUTCOME_ARGUMENT = "--outcome";
    /**
     * The argument that hosts a network game and names the file the lobby port is written to.
     */
    public static final @NotNull String HOST_ARGUMENT = "--host";
    /**
     * The argument that hosts a network game over Beacon.Link at the nodes of the runtime's command line.
     */
    public static final @NotNull String BEACON_LINK_HOST_ARGUMENT = "--beaconlink-host";
    /**
     * The argument that joins a network game over Beacon.Link that the nodes of the runtime's command line list.
     */
    public static final @NotNull String BEACON_LINK_JOIN_ARGUMENT = "--beaconlink-join";
    /**
     * The seed of a game without {@value #SEED_ARGUMENT}.
     */
    public static final long DEFAULT_SEED = 1L;
    /**
     * The game loops after which a game without outcome ends.
     */
    public static final int LOOP_LIMIT = 100000;
    /**
     * The game loops between two state hash checkpoints of a recorded replay.
     */
    public static final int CHECKPOINT_INTERVAL = 100;
    /**
     * The game loop at which the person at each machine of a network game sends a worker to scout.
     */
    public static final int NETWORK_ORDER_LOOP = 240;
    /**
     * The time a network game waits for its room, its turns or its departure before it fails.
     */
    public static final long NETWORK_TIMEOUT_MILLIS = 60000L;

    private static final @NotNull String HOST_NAME = "host";
    private static final @NotNull String JOINER_NAME = "joiner";
    private static final @NotNull String PORT_FILE_SUFFIX = ".part";
    private static final long WAIT_NANOS = TimeUnit.MICROSECONDS.toNanos(200L);
    private static final long SEARCH_PAUSE_NANOS = TimeUnit.MILLISECONDS.toNanos(200L);

    private static long parseSeed(@Nullable String seedText) throws ModException {
        if (seedText == null) {
            return DEFAULT_SEED;
        }

        try {
            return Long.parseLong(seedText);
        } catch (NumberFormatException exception) {
            throw ModException.create(exception, "The seed {0} is not a number.", seedText);
        }
    }

    private static void writeFrameHashes(@NotNull Path file, @NotNull List<@NotNull Long> frameHashes) throws IOException {
        StringBuilder text = new StringBuilder();

        for (long hash : frameHashes) {
            text.append(Long.toHexString(hash)).append('\n');
        }

        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static @NotNull String describeOutcome(@NotNull Game game) {
        StringBuilder outcomes = new StringBuilder();

        for (int index = 0; index < game.getPlayerCount(); index++) {
            outcomes.append(' ').append(index).append('=').append(DemoGames.getOutcome(game, game.getPlayer(index)));
        }

        return outcomes.toString();
    }

    private static void waitBriefly() {
        LockSupport.parkNanos(WAIT_NANOS);
    }


    private final @NotNull DemoContent content;
    private final @NotNull DemoMaps maps;
    private final @NotNull ModStorage storage;
    private final @NotNull List<@NotNull String> arguments;
    private final @NotNull Logger logger;

    /**
     * Creates a {@link DemoHeadlessRunner} instance.
     *
     * @param content   the demo's content
     * @param maps      the demo's maps
     * @param storage   the mod storage
     * @param arguments the demo's own command line arguments, copied
     * @param logger    the logger of the mod
     */
    public DemoHeadlessRunner(@NotNull DemoContent content, @NotNull DemoMaps maps, @NotNull ModStorage storage,
                              @NotNull List<@NotNull String> arguments, @NotNull Logger logger) {
        this.content   = content;
        this.maps      = maps;
        this.storage   = storage;
        this.arguments = List.copyOf(arguments);
        this.logger    = logger;
    }

    /**
     * Runs the mod headless until the launch request is done.
     * <p>
     * Plays a computer game, a network game or the requested replay.
     *
     * @param launch the launch request of the command line and the mod's identity
     * @throws ModException if the mod refuses the launch request or its run fails
     */
    @Override
    public void run(@NotNull ModLaunch launch) throws ModException {
        DemoGames games = new DemoGames(launch.getGameDefinition(), this.content, this.maps);
        Path replay = launch.getReplay();
        ReplayFormat replayFormat = replay == null ? launch.getReplayFormats().getFirst() : launch.getReplayFormat();

        if (!(replayFormat instanceof EngineReplayFormat format)) {
            throw ModException.create("The demo plays only replays of the engine format, not {0}.", replayFormat);
        }

        String mapName = this.findArgument(MAP_ARGUMENT);
        String seedText = this.findArgument(SEED_ARGUMENT);
        String hashesFile = this.findArgument(FRAME_HASHES_ARGUMENT);
        String outcomeFile = this.findArgument(OUTCOME_ARGUMENT);
        String hostFile = this.findArgument(HOST_ARGUMENT);
        boolean linkHost = this.arguments.contains(BEACON_LINK_HOST_ARGUMENT);
        boolean linkJoin = this.arguments.contains(BEACON_LINK_JOIN_ARGUMENT);
        List<Long> frameHashes = new ArrayList<>();
        Game game;

        try {
            if (replay != null) {
                game = this.playReplay(games, format, replay, frameHashes);
            } else if (linkHost || linkJoin) {
                game = this.playLinkGame(games, format, launch, this.requireMap(mapName), parseSeed(seedText), linkHost, frameHashes);
            } else if (hostFile != null || launch.getJoin() != null) {
                game = this.playNetworkGame(games, format, launch, this.requireMap(mapName), parseSeed(seedText), hostFile, frameHashes);
            } else {
                game = this.playComputerGame(games, format, this.requireMap(mapName), parseSeed(seedText), frameHashes);
            }

            if (hashesFile != null) {
                writeFrameHashes(Path.of(hashesFile), frameHashes);
            }

            if (outcomeFile != null) {
                Files.writeString(Path.of(outcomeFile), "loops " + game.getClock().getFrame() + '\n' + describeOutcome(game).trim() + '\n',
                                  StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw ModException.create(exception, "The demo game failed: {0}", exception.getMessage());
        }
    }

    private @NotNull MapReference requireMap(@Nullable String mapName) throws ModException {
        String name = mapName != null ? mapName : DemoMaps.ASHEN_FORD;

        try {
            return this.maps.requireReference(name);
        } catch (IllegalArgumentException exception) {
            throw ModException.create(exception, "Unknown map {0}.", name);
        }
    }

    private @NotNull Game playComputerGame(@NotNull DemoGames games, @NotNull EngineReplayFormat format, @NotNull MapReference map, long seed,
                                           @NotNull List<@NotNull Long> frameHashes) throws IOException {
        GameSetup setup = games.createComputerSetup(map, seed);
        Game game = games.createGame(setup);
        ReplayRecorder recorder = format.createRecorder(setup, CHECKPOINT_INTERVAL);
        ControllerCommandSource source = new ControllerCommandSource(List.of());

        source.setRecorder(recorder);
        game.getFrameRunner().setCommandSource(source);

        while (!DemoGames.isOver(game) && game.getClock().getFrame() < LOOP_LIMIT) {
            game.getFrameRunner().runFrame();
            frameHashes.add(game.computeStateHash());
        }

        this.writeReplay(REPLAY_PATH, format.write(recorder.createReplay()));
        this.logOutcome(game, "game on " + map.getName() + " with seed " + seed);

        return game;
    }

    /**
     * Plays a network game of two machines on the loopback address: hosts or joins the room, waits until both members have the map and are
     * ready, plays the started game to its end, leaves it and writes the replay.
     */
    private @NotNull Game playNetworkGame(@NotNull DemoGames games, @NotNull EngineReplayFormat format, @NotNull ModLaunch launch,
                                          @NotNull MapReference map, long seed, @Nullable String hostFile,
                                          @NotNull List<@NotNull Long> frameHashes) throws ModException, IOException {
        DemoNetworkSettings settings = DemoNetworkSettings.ANY_PORTS;
        GameSetupCodec codec = this.content.createSetupCodec();
        String join = launch.getJoin();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(NETWORK_TIMEOUT_MILLIS);
        DemoLobby lobby;

        try {
            lobby = hostFile != null ? DemoLobby.host(settings, launch.getIdentity(), codec, this.maps, HOST_NAME, DemoLobby.createRoomName(HOST_NAME),
                                                      map, NetworkClock.SYSTEM)
                                     : DemoLobby.join(settings, launch.getIdentity(), codec, this.maps, JOINER_NAME,
                                                      DemoLobby.parseAddress(join != null ? join : ""));
        } catch (IllegalArgumentException exception) {
            throw ModException.create(exception, "The demo cannot join {0}.", join);
        }

        NetworkGameStart start;

        try {
            if (hostFile != null) {
                Path portFile = Path.of(hostFile);
                Path partFile = Path.of(hostFile + PORT_FILE_SUFFIX);

                Files.writeString(partFile, Integer.toString(lobby.getLobby().getLocalAddress().getPort()), StandardCharsets.UTF_8);
                Files.move(partFile, portFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }

            start = this.waitForStart(lobby, games, seed, deadline);
        } catch (IOException | ModException | RuntimeException exception) {
            lobby.close();

            throw exception;
        }

        return this.playStartedGame(games, format, map, lobby, start, settings, frameHashes);
    }

    /**
     * Plays a network game of two machines over Beacon.Link with the nodes of the runtime's command line: opens the session, hosts a room or
     * searches and joins the first room of the demo, waits until both members have the map, are ready and connected, plays the started game
     * to its end, leaves it and writes the replay.
     */
    private @NotNull Game playLinkGame(@NotNull DemoGames games, @NotNull EngineReplayFormat format, @NotNull ModLaunch launch,
                                       @NotNull MapReference map, long seed, boolean host,
                                       @NotNull List<@NotNull Long> frameHashes) throws ModException, IOException {
        BeaconLink beaconLink = launch.getBeaconLink();
        DemoNetworkSettings settings = DemoNetworkSettings.ANY_PORTS;
        GameSetupCodec codec = this.content.createSetupCodec();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(NETWORK_TIMEOUT_MILLIS);

        if (beaconLink == null) {
            throw ModException.create("Beacon.Link is not available in this run: start it with a node (--seed host:port).");
        }

        BeaconLinkSession session;

        try {
            session = beaconLink.open();
        } catch (IllegalArgumentException exception) {
            throw ModException.create(exception, "The demo cannot open a Beacon.Link session: {0}", exception.getMessage());
        }

        DemoLobby lobby;
        NetworkGameStart start;

        try {
            this.waitForRegistration(session, deadline);
            lobby = host ? DemoLobby.host(session, launch.getIdentity(), codec, this.maps, HOST_NAME, DemoLobby.createRoomName(HOST_NAME), map,
                                          DemoNetworkSettings.ROOM_CAPACITY, DemoNetworkSettings.DEFAULT_TURN_RATE, "", NetworkClock.SYSTEM)
                         : DemoLobby.join(session, this.findRoom(session, deadline), launch.getIdentity(), codec, this.maps, JOINER_NAME, "",
                                          NetworkClock.SYSTEM);
        } catch (IOException | ModException | RuntimeException exception) {
            session.close();

            throw exception;
        }

        try {
            start = this.waitForStart(lobby, games, seed, deadline);
        } catch (IOException | ModException | RuntimeException exception) {
            lobby.close();

            throw exception;
        }

        return this.playStartedGame(games, format, map, lobby, start, settings, frameHashes);
    }

    private void waitForRegistration(@NotNull BeaconLinkSession session, long deadline) throws ModException, IOException {
        while (session.getObservedAddress() == null) {
            session.poll();

            LinkFailure failure = session.getFailure();

            if (failure != null) {
                throw ModException.create("Beacon.Link is not available: {0}", failure.getDescription());
            }

            if (System.nanoTime() > deadline) {
                throw ModException.create("No Beacon.Link node answered in {0} ms.", NETWORK_TIMEOUT_MILLIS);
            }

            waitBriefly();
        }
    }

    /**
     * Searches the rooms of the demo until a search finds one.
     */
    private @NotNull FoundRoom findRoom(@NotNull BeaconLinkSession session, long deadline) throws ModException, IOException {
        while (true) {
            RoomSearch search = session.searchRooms();

            while (!search.isComplete()) {
                session.poll();
                waitBriefly();
            }

            if (!search.getRooms().isEmpty()) {
                return search.getRooms().getFirst();
            }

            if (System.nanoTime() > deadline) {
                throw ModException.create("No Beacon.Link room of the demo was found in {0} ms.", NETWORK_TIMEOUT_MILLIS);
            }

            LockSupport.parkNanos(SEARCH_PAUSE_NANOS);
        }
    }

    private @NotNull Game playStartedGame(@NotNull DemoGames games, @NotNull EngineReplayFormat format, @NotNull MapReference map,
                                          @NotNull DemoLobby lobby, @NotNull NetworkGameStart start, @NotNull DemoNetworkSettings settings,
                                          @NotNull List<@NotNull Long> frameHashes) throws ModException, IOException {
        try (DemoNetworkGame network = new DemoNetworkGame(start, lobby, NetworkClock.SYSTEM, settings, this.content, this.maps, games, format,
                                                           CHECKPOINT_INTERVAL)) {
            this.playNetworkLoops(network, games, frameHashes);
            network.finishDeparture();
            this.writeReplay(NETWORK_REPLAY_PATH, format.write(network.getRecorder().createReplay()));
            this.logOutcome(network.getGame(), "network game on " + map.getName() + " with seed " + start.getSetup().getSeed());

            return network.getGame();
        }
    }

    private @NotNull NetworkGameStart waitForStart(@NotNull DemoLobby room, @NotNull DemoGames games, long seed, long deadline) throws ModException, IOException {
        NetworkLobby lobby = room.getLobby();
        boolean readySent = false;

        while (true) {
            room.poll();

            NetworkGameStart start = lobby.getStart();
            LobbyMember local = lobby.getLocalMember();

            if (start != null) {
                return start;
            }

            if (lobby.getLobbyState() == LobbyState.REFUSED || lobby.getLobbyState() == LobbyState.CLOSED) {
                throw ModException.create("The network room ended before the start: {0} {1}", lobby.getLobbyState(), lobby.getRefusalText());
            }

            if (!readySent && local != null && local.getMapState() == LobbyMapState.PRESENT &&
                (lobby.isHost() || lobby.getMembers().size() == DemoNetworkSettings.ROOM_CAPACITY)) {
                lobby.setReady(true);
                readySent = true;
            }

            if (lobby.isHost() && lobby.getMembers().size() == DemoNetworkSettings.ROOM_CAPACITY && lobby.isReadyToStart()) {
                return room.start(games, ControllerKind.COMPUTER, seed);
            }

            if (System.nanoTime() > deadline) {
                throw ModException.create("The network room did not start in {0} ms.", NETWORK_TIMEOUT_MILLIS);
            }

            waitBriefly();
        }
    }

    private void playNetworkLoops(@NotNull DemoNetworkGame network, @NotNull DemoGames games, @NotNull List<@NotNull Long> frameHashes) throws ModException {
        Game game = network.getGame();
        long waitStart = System.nanoTime();
        boolean ordered = false;

        while (!network.isOver() && game.getClock().getFrame() < LOOP_LIMIT) {
            if (!ordered && game.getClock().getFrame() == NETWORK_ORDER_LOOP) {
                this.sendScout(network, games);
                ordered = true;
            }

            if (network.runFrame()) {
                frameHashes.add(game.computeStateHash());
                waitStart = System.nanoTime();
            } else if (System.nanoTime() - waitStart > TimeUnit.MILLISECONDS.toNanos(NETWORK_TIMEOUT_MILLIS)) {
                throw ModException.create("The network game waited too long for a turn at game loop {0}.", game.getClock().getFrame());
            } else {
                waitBriefly();
            }
        }
    }

    /**
     * Submits the person's order to send its first worker to the other start location, a command of the next turn.
     */
    private void sendScout(@NotNull DemoNetworkGame network, @NotNull DemoGames games) {
        Player player = network.getLocalPlayer();
        CommandSink sink = network.getSession().getCommandSink();
        IssueOrderCommand command = player == null ? null : games.createScoutOrder(network.getGame(), player);

        if (player != null && sink != null && command != null) {
            sink.submit(player, this.content.getOrders().getOrderCommand(), command);
        }
    }

    private @NotNull Game playReplay(@NotNull DemoGames games, @NotNull EngineReplayFormat format, @NotNull Path file,
                                     @NotNull List<@NotNull Long> frameHashes) throws ModException, IOException {
        EngineReplay replay = format.read(Files.readAllBytes(file));
        Game game;

        try {
            game = games.createGame(replay.getSetup());
        } catch (IllegalArgumentException exception) {
            throw ModException.create(exception, "The replay {0} is of a map the demo does not have: {1}", file, replay.getSetup().getMap().getName());
        }

        ReplayController controller = replay.createController(null);

        game.getFrameRunner().setCommandSource(new ControllerCommandSource(List.of(controller)));

        while (!controller.isFinished()) {
            game.getFrameRunner().runFrame();
            frameHashes.add(game.computeStateHash());
        }

        if (controller.getMismatchCount() > 0) {
            throw ModException.create("The replay {0} does not reproduce its game: {1} state hash checkpoints differ.", file,
                                      controller.getMismatchCount());
        }

        this.logOutcome(game, "replay " + file);

        return game;
    }

    private void writeReplay(@NotNull String storagePath, byte @NotNull [] contents) throws IOException {
        Path replay = this.storage.resolve(storagePath);

        Files.createDirectories(replay.getParent());
        Files.write(replay, contents);
    }

    private void logOutcome(@NotNull Game game, @NotNull String description) {
        this.logger.info("DEMO: {0} ended after {1} game loops:{2}", description, game.getClock().getFrame(), describeOutcome(game));
    }

    private @Nullable String findArgument(@NotNull String name) throws ModException {
        int index = this.arguments.indexOf(name);

        if (index < 0) {
            return null;
        }

        if (index + 1 >= this.arguments.size()) {
            throw ModException.create("The demo argument {0} needs a value.", name);
        }

        return this.arguments.get(index + 1);
    }
}
