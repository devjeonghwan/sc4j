package org.fogofwarlabs.sc4j.demo.game;

import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.demo.map.DemoMaps;
import org.fogofwarlabs.sc4j.demo.order.DemoOrders;
import org.fogofwarlabs.sc4j.demo.rules.DemoRules;
import org.fogofwarlabs.sc4j.engine.command.CommandRecorder;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.controller.ControllerCommandSource;
import org.fogofwarlabs.sc4j.engine.controller.ControllerSource;
import org.fogofwarlabs.sc4j.engine.controller.PlayerController;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameDefinition;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.lobby.MapFolders;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.replay.ReplayController;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.version.EngineVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Plays games of the demo headless without the runtime: the demo's content and maps from the test class loader, its game definition from
 * {@link DemoRules}, and games of two computer players on Ashen Ford, optionally with controllers and a command recorder, until the
 * annihilation triggers decide them. {@link QueueCommands} trains and cancels at a town hall as a person would.
 */
public final class DemoGameScenario implements Closeable {
    /**
     * The game loops after which a game counts as undecided.
     */
    public static final int LOOP_LIMIT = 60000;

    private static final @NotNull String MANIFEST = "/sc4j-mod.json";
    private static final int DIGEST_LENGTH = 32;
    private static final @NotNull String DAWNHOLD_TOWN_HALL = "hearthstead";
    private static final @NotNull String DAWNHOLD_WORKER = "tiller";

    /**
     * Trains workers at the town hall of the first player and cancels entries as a person would: at the train loop it submits one training
     * command more than the queue holds (the last one meets a full queue if the gold suffices); at the first loop from the cancel loop on at which
     * the queue holds entries, its own or the computer player's, it cancels the last entry and then the first one. It counts the cancels it
     * submits.
     */
    public static final class QueueCommands implements PlayerController {
        private final @NotNull DemoOrders orders;
        private final @NotNull UnitType townHall;
        private final @NotNull UnitType worker;
        private final int trainLoop;
        private final int cancelLoop;

        private int cancelCount;

        /**
         * Creates a {@link QueueCommands} instance for the first player, a Dawnhold player.
         *
         * @param content    the demo's content
         * @param trainLoop  the game loop of the training commands
         * @param cancelLoop the first game loop of the cancel commands
         */
        public QueueCommands(@NotNull DemoContent content, int trainLoop, int cancelLoop) {
            this.orders     = content.getOrders();
            this.townHall   = content.requireUnitType(DAWNHOLD_TOWN_HALL);
            this.worker     = content.requireUnitType(DAWNHOLD_WORKER);
            this.trainLoop  = trainLoop;
            this.cancelLoop = cancelLoop;

            this.cancelCount = 0;
        }

        /**
         * @return the cancel commands submitted so far
         */
        public int getCancelCount() {
            return this.cancelCount;
        }

        /**
         * @return where the commands of this controller come from: {@link ControllerSource#HUMAN}
         */
        @Override
        public @NotNull ControllerSource getSource() {
            return ControllerSource.HUMAN;
        }

        /**
         * Hands over the commands due at the frame about to run, in their order.
         *
         * @param game the game
         * @param sink the sink of the commands
         */
        @Override
        public void collectCommands(@NotNull Game game, @NotNull CommandSink sink) {
            Player player = game.getPlayer(DemoGames.FIRST_SLOT);
            Unit hall = findUnit(game, player, this.townHall.getDebugName());
            int loop = game.getClock().getFrame();

            if (hall == null) {
                return;
            }

            if (loop == this.trainLoop) {
                for (int entry = 0; entry <= DemoRules.QUEUE_CAPACITY; entry++) {
                    sink.submit(player, this.orders.getTrainCommand(), this.orders.getTrainCommand().create(game, hall, this.worker));
                }
            }

            ProductionSystem production = game.getProduction();
            int length = production.getQueueLength(hall);

            if (loop < this.cancelLoop || length == 0 || this.cancelCount > 0) {
                return;
            }

            sink.submit(player, this.orders.getCancelCommand(), this.orders.getCancelCommand().create(game, hall, length - 1));
            this.cancelCount++;

            if (length > 1) {
                sink.submit(player, this.orders.getCancelCommand(), this.orders.getCancelCommand().create(game, hall, 0));
                this.cancelCount++;
            }
        }
    }

    /**
     * Holds what a played game ended with.
     */
    public static final class Outcome {
        private final int loops;
        private final @NotNull List<@NotNull Long> frameHashes;
        private final @NotNull String outcomes;
        private final @NotNull Set<@NotNull String> builtTypes;

        private Outcome(int loops, @NotNull List<@NotNull Long> frameHashes, @NotNull String outcomes, @NotNull Set<@NotNull String> builtTypes) {
            this.loops       = loops;
            this.frameHashes = List.copyOf(frameHashes);
            this.outcomes    = outcomes;
            this.builtTypes  = Set.copyOf(builtTypes);
        }

        /**
         * @return the game loops the game ran
         */
        public int getLoops() {
            return this.loops;
        }

        /**
         * @return the state hash after every game loop
         */
        public @NotNull List<@NotNull Long> getFrameHashes() {
            return this.frameHashes;
        }

        /**
         * @return the outcome of every player slot, as {@code slot=OUTCOME} separated by spaces
         */
        public @NotNull String getOutcomes() {
            return this.outcomes;
        }

        /**
         * @return the debug names of the unit types that came into the game after its start
         */
        public @NotNull Set<@NotNull String> getBuiltTypes() {
            return this.builtTypes;
        }
    }

    /**
     * Creates the demo's mod identity from its manifest on the test class path, with an empty content digest.
     *
     * @return the created {@link ModIdentity} instance
     * @throws IOException       if the manifest cannot be read
     * @throws ManifestException if the manifest is invalid
     */
    public static @NotNull ModIdentity createIdentity() throws IOException, ManifestException {
        byte[] manifest;

        try (InputStream input = DemoGameScenario.class.getResourceAsStream(MANIFEST)) {
            Assertions.assertNotNull(input);

            manifest = input.readAllBytes();
        }

        return ModIdentity.create(EngineVersion.CURRENT, ModManifestReader.parse(manifest), new byte[DIGEST_LENGTH], List.of());
    }

    /**
     * Finds the first unit of a player of a type, in slot order.
     *
     * @param game   the game
     * @param owner  the player
     * @param typeId the pack id of the unit type
     * @return the unit, or {@code null} if the player has none
     */
    public static @Nullable Unit findUnit(@NotNull Game game, @NotNull Player owner, @NotNull String typeId) {
        EntityPool<Unit> units = game.getUnits();
        SlotList used = units.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit unit = units.get(index);
            UnitType type = unit.getType();

            if (unit.getOwner() == owner && type != null && type.getDebugName().equals(typeId)) {
                return unit;
            }
        }

        return null;
    }

    private static void collectTypes(@NotNull Game game, @NotNull Set<@NotNull String> seenTypes) {
        EntityPool<Unit> units = game.getUnits();
        SlotList used = units.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            UnitType type = units.get(index).getType();

            if (type != null) {
                seenTypes.add(type.getDebugName());
            }
        }
    }


    private final @NotNull DemoContent content;
    private final @NotNull DemoMaps maps;
    private final @NotNull DemoGames games;

    /**
     * Creates a {@link DemoGameScenario} instance with the demo's content, maps and game definition.
     *
     * @throws IOException if the demo's pack or maps cannot be read
     */
    public DemoGameScenario() throws IOException {
        ClassLoader loader = DemoGameScenario.class.getClassLoader();
        GameDefinition.Builder builder = new GameDefinition.Builder();

        this.content = DemoContent.open(loader);
        this.maps    = DemoMaps.load(loader, this.content, new MapFolders(List.of(), null));

        new DemoRules(this.content, this.maps).define(builder);

        this.games = new DemoGames(builder.build(), this.content, this.maps);
    }

    /**
     * @return the demo's content
     */
    public @NotNull DemoContent getContent() {
        return this.content;
    }

    /**
     * @return the demo maps
     */
    public @NotNull DemoMaps getMaps() {
        return this.maps;
    }

    /**
     * @return the game factory of the demo
     */
    public @NotNull DemoGames getGames() {
        return this.games;
    }


    /**
     * Creates the setup of a game of two computer players on Ashen Ford.
     *
     * @param seed the seed
     * @return the created {@link GameSetup} instance
     */
    public @NotNull GameSetup createSetup(long seed) {
        return this.games.createComputerSetup(this.maps.requireReference(DemoMaps.ASHEN_FORD), seed);
    }

    /**
     * Plays a game of a setup until it is decided or reaches the {@link #LOOP_LIMIT}.
     *
     * @param setup       the setup
     * @param controllers the controllers of the game's command source, beside the computer players
     * @param recorder    the recorder of the command source, or {@code null}
     * @return what the game ended with
     */
    public @NotNull Outcome play(@NotNull GameSetup setup, @NotNull List<@NotNull PlayerController> controllers, @Nullable CommandRecorder recorder) {
        Game game = this.games.createGame(setup);
        ControllerCommandSource source = new ControllerCommandSource(controllers);
        Set<String> startTypes = new TreeSet<>();
        Set<String> seenTypes = new TreeSet<>();
        List<Long> frameHashes = new ArrayList<>();

        collectTypes(game, startTypes);
        source.setRecorder(recorder);
        game.getFrameRunner().setCommandSource(source);

        while (!DemoGames.isOver(game) && game.getClock().getFrame() < LOOP_LIMIT) {
            game.getFrameRunner().runFrame();
            frameHashes.add(game.computeStateHash());
            collectTypes(game, seenTypes);
        }

        seenTypes.removeAll(startTypes);

        return new Outcome(game.getClock().getFrame(), frameHashes, this.describeOutcomes(game), seenTypes);
    }

    /**
     * Plays a replay back in a game of its setup until the replay's last game loop.
     *
     * @param setup      the setup of the replay
     * @param controller the replay controller
     * @return what the replayed game ended with
     */
    public @NotNull Outcome playBack(@NotNull GameSetup setup, @NotNull ReplayController controller) {
        Game game = this.games.createGame(setup);
        List<Long> frameHashes = new ArrayList<>();

        game.getFrameRunner().setCommandSource(new ControllerCommandSource(List.of(controller)));

        while (!controller.isFinished()) {
            game.getFrameRunner().runFrame();
            frameHashes.add(game.computeStateHash());
        }

        return new Outcome(game.getClock().getFrame(), frameHashes, this.describeOutcomes(game), Set.of());
    }

    private @NotNull String describeOutcomes(@NotNull Game game) {
        StringBuilder outcomes = new StringBuilder();

        for (int index = 0; index < game.getPlayerCount(); index++) {
            if (!outcomes.isEmpty()) {
                outcomes.append(' ');
            }

            outcomes.append(index).append('=').append(DemoGames.getOutcome(game, game.getPlayer(index)));
        }

        return outcomes.toString();
    }

    /**
     * Closes the demo's pack.
     *
     * @throws IOException if the pack cannot be closed
     */
    @Override
    public void close() throws IOException {
        this.content.close();
    }
}
