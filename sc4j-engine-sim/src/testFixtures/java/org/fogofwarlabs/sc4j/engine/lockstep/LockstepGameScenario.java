package org.fogofwarlabs.sc4j.engine.lockstep;

import org.fogofwarlabs.sc4j.engine.command.CommandRecorder;
import org.fogofwarlabs.sc4j.engine.command.CommandRegistry;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.controller.ControllerCommandSource;
import org.fogofwarlabs.sc4j.engine.controller.ControllerSource;
import org.fogofwarlabs.sc4j.engine.controller.PlayerController;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.map.*;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommandType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Runs engine lockstep machines on the skirmish map: every machine has its own game and session over a transport, every participant plays the
 * scout's player, and the person at each machine orders the scout once at the machine's order frame. A machine may leave the session at a
 * frame; a machine the others dropped stops. The machines record the state hash after every frame and the departures their sessions report,
 * and a machine's game may have a command recorder, for example a replay recorder.
 */
public final class LockstepGameScenario {
    /**
     * The seed of the machines' games.
     */
    public static final long SEED = 0x5C4A_2026_1106L;
    /**
     * The game loops of one turn.
     */
    public static final int FRAMES_PER_TURN = 2;
    /**
     * The turns kept in transit.
     */
    public static final int LATENCY_TURNS = 3;

    private static final int COMMAND_ID = 0;
    private static final int INSTRUCTION_LIMIT = 100000;
    private static final int SCOUT_SLOT = 3;
    private static final int TARGET_X = 600;
    private static final int TARGET_Y = 140;
    private static final int ROUND_LIMIT_PER_FRAME = 200;

    /**
     * One machine: its game, its session and the person's scout order it submits.
     */
    public final class Machine {
        private final @NotNull Game game;
        private final @NotNull List<@NotNull String> departures;
        private final @NotNull LockstepSession session;
        private final int orderFrame;

        private final @NotNull List<@NotNull Long> frameHashes;
        private int executed;
        private int leaveFrame;
        private int leaveReason;
        private boolean left;

        private final @NotNull ControllerCommandSource source;

        private Machine(@NotNull Game game,
                        @NotNull Function<@NotNull PeerDepartureListener, @NotNull LockstepSession> sessionFactory,
                        int orderFrame) {
            List<String> reported = new ArrayList<>();

            this.game       = game;
            this.departures = reported;
            this.session    = sessionFactory.apply((departure, frame) -> reported.add(frame + " " + departure));
            this.orderFrame = orderFrame;

            this.frameHashes = new ArrayList<>();
            this.executed    = 0;
            this.leaveFrame  = -1;
            this.leaveReason = PeerDeparture.NO_REASON;
            this.left        = false;

            PlayerController counting = new PlayerController() {
                @Override
                public @NotNull ControllerSource getSource() {
                    return Machine.this.session.getController().getSource();
                }

                @Override
                public void collectCommands(@NotNull Game game, @NotNull CommandSink sink) {
                    Machine.this.session.getController().collectCommands(game, new CommandSink() {
                        @Override
                        public <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
                            Machine.this.executed++;
                            sink.submit(player, type, command);
                        }
                    });
                }
            };

            this.source = new ControllerCommandSource(List.of(counting));

            this.session.attach(this.game);
            this.game.getFrameRunner().setCommandSource(this.source);
        }

        /**
         * @return the machine's game
         */
        public @NotNull Game getGame() {
            return this.game;
        }

        /**
         * @return the machine's session
         */
        public @NotNull LockstepSession getSession() {
            return this.session;
        }

        /**
         * @return the state hash after every frame run so far
         */
        public @NotNull List<@NotNull Long> getFrameHashes() {
            return this.frameHashes;
        }

        /**
         * @return the departures the session reported, each as the frame about to run (counted from {@code 1}) and the departure
         */
        public @NotNull List<@NotNull String> getDepartures() {
            return this.departures;
        }

        /**
         * @return the commands the session handed over so far
         */
        public int getExecuted() {
            return this.executed;
        }

        /**
         * @return {@code true} if the other machines dropped this one, {@code false} otherwise
         */
        public boolean isDropped() {
            for (PeerDeparture departure : this.session.getDepartures()) {
                if (departure.getPeer() == this.session.getLocalPeer()) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Sets the recorder of the commands the machine's game runs, before its first frame.
         *
         * @param recorder the recorder, or {@code null} to record nothing
         */
        public void setRecorder(@Nullable CommandRecorder recorder) {
            this.source.setRecorder(recorder);
        }

        /**
         * Makes the machine leave the session before a frame.
         *
         * @param frame  the frame it does not run any more
         * @param reason the departure reason
         */
        public void setLeave(int frame, int reason) {
            this.leaveFrame  = frame;
            this.leaveReason = reason;
        }


        /**
         * Runs the next game loop if its turns arrived; the person orders the scout once at this machine's order frame.
         *
         * @return {@code true} if the loop ran, {@code false} otherwise
         */
        public boolean step() {
            if (this.left || this.isDropped()) {
                return false;
            }

            int frame = this.game.getClock().getFrame();

            if (frame == this.leaveFrame) {
                this.session.leave(this.leaveReason);
                this.left = true;

                return false;
            }

            if (!this.session.prepareFrame(this.game)) {
                return false;
            }

            Player player = this.game.getPlayer(SCOUT_SLOT);
            Unit scout = findUnit(this.game, player);

            if (frame == this.orderFrame && scout != null) {
                IssueOrderCommandType orders = LockstepGameScenario.this.orders;

                this.session.getOutgoingSink().submit(player, orders, orders.create(this.game, scout, LockstepGameScenario.this.scenario.getPatrol(),
                                                                                    TARGET_X, TARGET_Y, null, false));
            }

            this.game.getFrameRunner().runFrame();
            this.frameHashes.add(this.game.computeStateHash());

            return true;
        }

        private boolean isFinished(int frameCount) {
            return this.left || this.isDropped() || this.frameHashes.size() >= frameCount;
        }
    }

    /**
     * Runs machines until every one ran a number of frames, left or was dropped.
     *
     * @param machines   the machines, stepped in their order
     * @param frameCount the frames every machine runs
     * @param round      called after every round of steps, for example to advance a clock and poll the transports
     */
    public static void run(@NotNull List<@NotNull Machine> machines, int frameCount, @NotNull Runnable round) {
        int rounds = 0;

        while (!machines.stream().allMatch(machine -> machine.isFinished(frameCount))) {
            for (Machine machine : machines) {
                if (!machine.isFinished(frameCount)) {
                    machine.step();
                }
            }

            round.run();
            Assertions.assertTrue(++rounds < frameCount * ROUND_LIMIT_PER_FRAME, "The machines stopped advancing.");
        }
    }

    private static @Nullable Unit findUnit(@NotNull Game game, @NotNull Player owner) {
        EntityPool<Unit> units = game.getUnits();
        SlotList used = units.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            if (units.get(index).getOwner() == owner) {
                return units.get(index);
            }
        }

        return null;
    }


    private final @NotNull SkirmishScenario scenario;
    private final @NotNull IssueOrderCommandType orders;
    private final @NotNull CommandRegistry<CommandType<?>> commands;

    /**
     * Creates a {@link LockstepGameScenario} instance with the skirmish content and its scout order command.
     */
    public LockstepGameScenario() {
        this.scenario = new SkirmishScenario();
        this.orders   = new IssueOrderCommandType(COMMAND_ID, INSTRUCTION_LIMIT);
        this.commands = new CommandRegistry<>(1);

        this.commands.register(this.orders);
        this.commands.freeze();
    }

    /**
     * @return the skirmish content
     */
    public @NotNull SkirmishScenario getScenario() {
        return this.scenario;
    }

    /**
     * @return the command types of the machines' games by id
     */
    public @NotNull CommandRegistry<CommandType<?>> getCommands() {
        return this.commands;
    }


    /**
     * Creates a machine with a new game.
     *
     * @param peer       the machine's participant
     * @param transport  the machine's transport
     * @param peerCount  the participants of the session, all playing the scout's player
     * @param seed       the seed of the machine's game
     * @param orderFrame the frame at which the person orders the scout
     * @return the created {@link Machine} instance
     * @throws IOException if the map cannot be read
     */
    public @NotNull Machine createMachine(@NotNull LockstepPeer peer, @NotNull LockstepTransport transport, int peerCount, long seed,
                                          int orderFrame) throws IOException {
        Game game = this.createGame(this.createSetup(seed));
        List<Player> peerPlayers = Collections.nCopies(peerCount, game.getPlayer(SCOUT_SLOT));

        return new Machine(game, listener -> new LockstepSession(transport, this.commands, peer, peerPlayers,
                                                                 new TurnSchedule(FRAMES_PER_TURN, LATENCY_TURNS), null, listener), orderFrame);
    }

    /**
     * Creates a machine of a game whose session another part creates, for example a network lobby's start.
     *
     * @param game           the machine's game
     * @param sessionFactory creates the machine's session with the listener of its departures
     * @param orderFrame     the frame at which the person orders the scout
     * @return the created {@link Machine} instance
     */
    public @NotNull Machine createMachine(@NotNull Game game,
                                          @NotNull Function<@NotNull PeerDepartureListener, @NotNull LockstepSession> sessionFactory,
                                          int orderFrame) {
        return new Machine(game, sessionFactory, orderFrame);
    }

    /**
     * Creates the setup of the skirmish game: two computer players, a neutral player and the scout's human player.
     *
     * @param seed the seed
     * @return the created {@link GameSetup} instance
     * @throws IOException if the map cannot be read
     */
    public @NotNull GameSetup createSetup(long seed) throws IOException {
        byte[] contents = MapTestResources.read(MapTestResources.TWIN_ROCKS);
        String name = this.readMap(contents).requireModData(StandardMapData.class).getName();

        return new GameSetup.Builder(MapReference.create(name, contents))
                .addPlayer(new PlayerSlot(0, ControllerKind.COMPUTER, 0, 0, this.scenario.getWardens()))
                .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, this.scenario.getRaiders()))
                .addPlayer(new PlayerSlot(2, ControllerKind.NEUTRAL, 2, 2, null))
                .addPlayer(new PlayerSlot(SCOUT_SLOT, ControllerKind.HUMAN, 0, SCOUT_SLOT, null))
                .setSeed(seed)
                .build();
    }

    /**
     * Creates the skirmish game of a setup with the map's units placed.
     *
     * @param setup the setup
     * @return the created {@link Game} instance
     * @throws IOException if the map cannot be read
     */
    public @NotNull Game createGame(@NotNull GameSetup setup) throws IOException {
        MapModel map = this.readMap(MapTestResources.read(MapTestResources.TWIN_ROCKS));
        Game game = Game.create(this.scenario.createDefinition(map.getTerrain(), map.requireModData(StandardMapData.class).getTriggers()).build(),
                                setup);

        this.scenario.prepare(game);
        MapPlacementUtils.placeUnits(game, map);

        return game;
    }

    private @NotNull MapModel readMap(byte @NotNull [] contents) throws IOException {
        return new JsonMapLoader(this.scenario.getUnitTypes(), this.scenario.getResourceKinds()).read(contents, MapTestResources.TWIN_ROCKS);
    }
}
