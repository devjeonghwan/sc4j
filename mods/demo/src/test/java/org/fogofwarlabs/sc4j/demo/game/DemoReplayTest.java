package org.fogofwarlabs.sc4j.demo.game;

import org.fogofwarlabs.sc4j.demo.order.DemoOrders;
import org.fogofwarlabs.sc4j.demo.rules.DemoRules;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.controller.ControllerSource;
import org.fogofwarlabs.sc4j.engine.controller.PlayerController;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.GameSetupCodec;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.replay.EngineReplay;
import org.fogofwarlabs.sc4j.engine.replay.EngineReplayFormat;
import org.fogofwarlabs.sc4j.engine.replay.ReplayController;
import org.fogofwarlabs.sc4j.engine.replay.ReplayRecorder;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

/**
 * Tests the replay round trip of a demo game through the engine replay format: a game of two computer players, in which a person also orders a
 * worker of the first player, or trains and cancels at its town hall, is recorded, written and read again, and its playback reaches every state
 * hash checkpoint, the same state hash after every game loop and the same outcome.
 */
final class DemoReplayTest {
    private static final long SEED = 0x5C4A_2026_1108L;
    private static final int TRAIN_LOOP = 300;
    private static final int CANCEL_LOOP = 310;
    private static final int CHECKPOINT_INTERVAL = 100;
    private static final int @NotNull [] ORDER_LOOPS = {200, 201, 900};
    private static final int @NotNull [] TARGETS = {700, 420, 300, 700, 520, 520};

    /**
     * Orders the first worker of the first player to move at the {@link #ORDER_LOOPS}, as a person would; the second order is queued.
     */
    private static final class WorkerOrders implements PlayerController {
        private final @NotNull DemoOrders orders;

        private int next;

        private WorkerOrders(@NotNull DemoOrders orders) {
            this.orders = orders;

            this.next = 0;
        }

        @Override
        public @NotNull ControllerSource getSource() {
            return ControllerSource.HUMAN;
        }

        @Override
        public void collectCommands(@NotNull Game game, @NotNull CommandSink sink) {
            Player player = game.getPlayer(DemoGames.FIRST_SLOT);
            Unit worker = DemoGameScenario.findUnit(game, player, "tiller");

            if (this.next >= ORDER_LOOPS.length || game.getClock().getFrame() != ORDER_LOOPS[this.next] || worker == null) {
                return;
            }

            sink.submit(player, this.orders.getOrderCommand(),
                        this.orders.getOrderCommand().create(game, worker, this.orders.getMove(), TARGETS[this.next * 2], TARGETS[this.next * 2 + 1],
                                                             null, this.next == 1));
            this.next++;
        }
    }

    @Test
    void a_recorded_game_plays_back_through_the_engine_replay_format_with_the_same_state_every_loop() throws IOException, ManifestException {
        try (DemoGameScenario scenario = new DemoGameScenario()) {
            DemoOrders orders = scenario.getContent().getOrders();
            EngineReplayFormat format = new EngineReplayFormat(DemoGameScenario.createIdentity(), orders.getCommands(),
                                                               new GameSetupCodec(scenario.getContent().getFactions(), List.of()));
            GameSetup setup = scenario.createSetup(SEED);
            ReplayRecorder recorder = format.createRecorder(setup, CHECKPOINT_INTERVAL);
            DemoGameScenario.Outcome live = scenario.play(setup, List.of(new WorkerOrders(orders)), recorder);
            byte[] contents = format.write(recorder.createReplay());
            EngineReplay replay = format.read(contents);
            ReplayController controller = replay.createController(null);
            DemoGameScenario.Outcome playedBack = scenario.playBack(replay.getSetup(), controller);

            Assertions.assertEquals(ORDER_LOOPS.length, replay.getCommandCount());
            Assertions.assertEquals(live.getLoops(), replay.getLoopCount());
            Assertions.assertEquals((live.getLoops() - 1) / CHECKPOINT_INTERVAL, replay.getCheckpointCount());
            Assertions.assertArrayEquals(contents, format.write(replay));
            Assertions.assertEquals(0, controller.getMismatchCount());
            DemoGameAssert.assertSameGame(live, playedBack);
        }
    }

    @Test
    void a_recorded_game_with_queue_cancels_plays_back_with_the_same_state_every_loop() throws IOException, ManifestException {
        try (DemoGameScenario scenario = new DemoGameScenario()) {
            DemoOrders orders = scenario.getContent().getOrders();
            EngineReplayFormat format = new EngineReplayFormat(DemoGameScenario.createIdentity(), orders.getCommands(),
                                                               new GameSetupCodec(scenario.getContent().getFactions(), List.of()));
            GameSetup setup = scenario.createSetup(SEED);
            ReplayRecorder recorder = format.createRecorder(setup, CHECKPOINT_INTERVAL);
            DemoGameScenario.QueueCommands commands = new DemoGameScenario.QueueCommands(scenario.getContent(), TRAIN_LOOP, CANCEL_LOOP);
            DemoGameScenario.Outcome live = scenario.play(setup, List.of(commands), recorder);
            EngineReplay replay = format.read(format.write(recorder.createReplay()));
            ReplayController controller = replay.createController(null);
            DemoGameScenario.Outcome playedBack = scenario.playBack(replay.getSetup(), controller);

            Assertions.assertTrue(commands.getCancelCount() > 0, "the queue held entries to cancel");
            Assertions.assertEquals(DemoRules.QUEUE_CAPACITY + 1 + commands.getCancelCount(), replay.getCommandCount(),
                                    "the training and cancel commands");
            Assertions.assertEquals(0, controller.getMismatchCount());
            DemoGameAssert.assertSameGame(live, playedBack);
        }
    }
}
