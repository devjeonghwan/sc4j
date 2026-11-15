package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.engine.command.CommandInput;
import org.fogofwarlabs.sc4j.engine.command.CommandOutput;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.controller.ControllerCommandSource;
import org.fogofwarlabs.sc4j.engine.controller.ControllerSource;
import org.fogofwarlabs.sc4j.engine.controller.PlayerController;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.map.*;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommand;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommandType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.trigger.PlayerOutcome;
import org.fogofwarlabs.sc4j.engine.trigger.StandardTriggerSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests a full engine-only game without installed game data: on the skirmish map of the engine map format, two engine default computer players of
 * the two skirmish factions gather, build, train and fight until the annihilation triggers of the map decide the game, while a person's scout gets
 * order commands; the same seed gives the same state hash every frame, and a replay of the recorded commands reproduces the game.
 */
final class SkirmishTest {
    private static final long SEED = 0x5C4A_2026_1104L;
    private static final int FRAME_LIMIT = 60000;
    private static final int COMMAND_ID = 0;
    private static final int SCOUT_SLOT = 3;
    private static final int PATROL_FRAME = 40;
    private static final int MOVE_FRAME = 3000;
    private static final int RETURN_FRAME = 6000;
    private static final int PATROL_X = 600;
    private static final int PATROL_Y = 140;
    private static final int AWAY_X = 200;
    private static final int AWAY_Y = 420;
    private static final int BACK_X = 420;
    private static final int BACK_Y = 120;
    private static final int RETURN_PATROL_X = 120;
    private static final int RETURN_PATROL_Y = 420;
    private static final int NANOSECONDS_PER_MILLISECOND = 1_000_000;

    /**
     * Holds one recorded command: the frame it was submitted before, the issuing player and its encoded payload.
     */
    private static final class RecordedCommand {
        private final int frame;
        private final int playerIndex;
        private final byte @NotNull [] payload;

        private RecordedCommand(int frame, int playerIndex, byte @NotNull [] payload) {
            this.frame       = frame;
            this.playerIndex = playerIndex;
            this.payload     = payload;
        }
    }

    /**
     * Records every command it receives with its frame and passes it on.
     */
    private static final class RecordingSink implements CommandSink {
        private final @NotNull List<@NotNull RecordedCommand> records;
        private final @NotNull CommandOutput output;

        private @Nullable CommandSink target;
        private int frame;

        private RecordingSink(@NotNull List<@NotNull RecordedCommand> records) {
            this.records = records;
            this.output  = new CommandOutput();

            this.target = null;
            this.frame  = 0;
        }

        @Override
        public <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
            CommandSink sink = this.target;

            Assertions.assertNotNull(sink);

            this.output.clear();
            type.write(command, this.output);
            this.records.add(new RecordedCommand(this.frame, player.getIndex(), this.output.toByteArray()));
            sink.submit(player, type, command);
        }
    }

    /**
     * Plays the person of the scout slot: it patrols the scout across the base, later moves it away and back, each as an order command.
     */
    private static final class ScoutController implements PlayerController {
        private final @NotNull SkirmishScenario scenario;
        private final @NotNull IssueOrderCommandType commands;
        private final @NotNull RecordingSink recording;

        private ScoutController(@NotNull SkirmishScenario scenario, @NotNull IssueOrderCommandType commands, @NotNull RecordingSink recording) {
            this.scenario  = scenario;
            this.commands  = commands;
            this.recording = recording;
        }

        @Override
        public @NotNull ControllerSource getSource() {
            return ControllerSource.HUMAN;
        }

        @Override
        public void collectCommands(@NotNull Game game, @NotNull CommandSink sink) {
            int frame = game.getClock().getFrame();
            Player player = game.getPlayer(SCOUT_SLOT);
            Unit scout = findUnit(game, player);

            this.recording.target = sink;
            this.recording.frame  = frame;

            if (scout == null) {
                return;
            }

            if (frame == PATROL_FRAME) {
                this.recording.submit(player, this.commands,
                                      this.commands.create(game, scout, this.scenario.getPatrol(), PATROL_X, PATROL_Y, null, false));
            } else if (frame == MOVE_FRAME) {
                this.recording.submit(player, this.commands, this.commands.create(game, scout, this.scenario.getMove(), AWAY_X, AWAY_Y, null, false));
                this.recording.submit(player, this.commands, this.commands.create(game, scout, this.scenario.getMove(), BACK_X, BACK_Y, null, true));
            } else if (frame == RETURN_FRAME) {
                this.recording.submit(player, this.commands,
                                      this.commands.create(game, scout, this.scenario.getPatrol(), RETURN_PATROL_X, RETURN_PATROL_Y, null, false));
            }
        }
    }

    /**
     * Replays recorded commands at their frames.
     */
    private static final class ReplayController implements PlayerController {
        private final @NotNull List<@NotNull RecordedCommand> records;
        private final @NotNull IssueOrderCommandType commands;

        private int next;

        private ReplayController(@NotNull List<@NotNull RecordedCommand> records, @NotNull IssueOrderCommandType commands) {
            this.records  = records;
            this.commands = commands;

            this.next = 0;
        }

        @Override
        public @NotNull ControllerSource getSource() {
            return ControllerSource.REPLAY;
        }

        @Override
        public void collectCommands(@NotNull Game game, @NotNull CommandSink sink) {
            int frame = game.getClock().getFrame();

            while (this.next < this.records.size() && this.records.get(this.next).frame == frame) {
                RecordedCommand record = this.records.get(this.next++);

                try {
                    IssueOrderCommand command = this.commands.read(new CommandInput(record.payload));

                    sink.submit(game.getPlayer(record.playerIndex), this.commands, command);
                } catch (IOException exception) {
                    Assertions.fail("A recorded command cannot be read.", exception);
                }
            }
        }
    }

    /**
     * Holds what a run of the game ended with.
     */
    private static final class Outcome {
        private final int frames;
        private final long finalHash;
        private final long frameHashes;
        private final @NotNull String outcomes;

        private Outcome(int frames, long finalHash, long frameHashes, @NotNull String outcomes) {
            this.frames      = frames;
            this.finalHash   = finalHash;
            this.frameHashes = frameHashes;
            this.outcomes    = outcomes;
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
    private final @NotNull IssueOrderCommandType commands;

    SkirmishTest() {
        this.scenario = new SkirmishScenario();
        this.commands = new IssueOrderCommandType(COMMAND_ID, 0);
    }

    @Test
    void two_computer_players_play_until_one_wins_and_replays_reproduce_the_game() throws IOException {
        List<RecordedCommand> records = new ArrayList<>();
        long start = System.nanoTime();
        Outcome live = this.play(new ScoutController(this.scenario, this.commands, new RecordingSink(records)));
        long liveTime = System.nanoTime() - start;
        Outcome again = this.play(new ScoutController(this.scenario, this.commands, new RecordingSink(new ArrayList<>())));
        Outcome replay = this.play(new ReplayController(records, this.commands));

        System.out.println("skirmish frames " + live.frames + ", run time " + liveTime / NANOSECONDS_PER_MILLISECOND + " ms, commands " +
                           records.size() + ", " + live.outcomes + ", final hash " + Long.toHexString(live.finalHash));

        Assertions.assertTrue(live.frames < FRAME_LIMIT, live.outcomes);
        Assertions.assertTrue(live.outcomes.contains("VICTORY") && live.outcomes.contains("DEFEAT"), live.outcomes);
        Assertions.assertTrue(records.size() >= 3, "the scout orders up to the scout's death: " + records.size());
        Assertions.assertEquals(live.frames, again.frames);
        Assertions.assertEquals(live.frameHashes, again.frameHashes);
        Assertions.assertEquals(live.finalHash, again.finalHash);
        Assertions.assertEquals(live.frames, replay.frames);
        Assertions.assertEquals(live.frameHashes, replay.frameHashes);
        Assertions.assertEquals(live.finalHash, replay.finalHash);
        Assertions.assertEquals(live.outcomes, replay.outcomes);
    }

    private @NotNull Outcome play(@NotNull PlayerController controller) throws IOException {
        byte[] contents = MapTestResources.read(MapTestResources.TWIN_ROCKS);
        MapModel map = new JsonMapLoader(this.scenario.getUnitTypes(), this.scenario.getResourceKinds()).read(contents, MapTestResources.TWIN_ROCKS);
        StandardMapData data = map.requireModData(StandardMapData.class);
        GameSetup setup = new GameSetup.Builder(new MapReference(data.getName(), contents))
                .addPlayer(new PlayerSlot(0, ControllerKind.COMPUTER, 0, 0, this.scenario.getWardens()))
                .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, this.scenario.getRaiders()))
                .addPlayer(new PlayerSlot(2, ControllerKind.NEUTRAL, 2, 2, null))
                .addPlayer(new PlayerSlot(SCOUT_SLOT, ControllerKind.HUMAN, 0, 3, null))
                .setSeed(SEED)
                .build();
        Game game = Game.create(this.scenario.createDefinition(map.getTerrain(), data.getTriggers()).build(), setup);
        StandardTriggerSystem triggers = (StandardTriggerSystem) game.getTriggers();
        StateHash frameHashes = new StateHash();

        this.scenario.prepare(game);
        MapPlacementUtils.placeUnits(game, map);
        game.getFrameRunner().setCommandSource(new ControllerCommandSource(List.of(controller)));
        game.getFrameRunner().setSyncHashListener((frame, hash) -> frameHashes.mixLong(hash), 1);

        while (!triggers.isGameOver() && game.getClock().getFrame() < FRAME_LIMIT) {
            game.getFrameRunner().runFrame();
        }

        StringBuilder outcomes = new StringBuilder();

        for (int index = 0; index < game.getPlayerCount(); index++) {
            outcomes.append(index).append('=').append(triggers.getOutcome(game.getPlayer(index))).append(' ');
        }

        Assertions.assertNotEquals(PlayerOutcome.PLAYING, triggers.getOutcome(game.getPlayer(0)), outcomes.toString());

        return new Outcome(game.getClock().getFrame(), game.computeStateHash(), frameHashes.getValue(), outcomes.toString().trim());
    }
}
