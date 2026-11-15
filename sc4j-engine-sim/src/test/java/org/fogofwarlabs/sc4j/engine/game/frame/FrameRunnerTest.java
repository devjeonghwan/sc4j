package org.fogofwarlabs.sc4j.engine.game.frame;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameDefinition;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine frame skeleton: command application point, frame counter, phase order and schedules, random range,
 * sync hash point and failure.
 */
final class FrameRunnerTest {
    /**
     * Records the frame number, the random gate and its name when it runs.
     */
    private static final class RecordingPhase implements FramePhase {
        private final @NotNull String name;
        private final @NotNull PhaseSchedule schedule;
        private final @NotNull List<@NotNull String> log;

        private RecordingPhase(@NotNull String name, @NotNull PhaseSchedule schedule, @NotNull List<@NotNull String> log) {
            this.name     = name;
            this.schedule = schedule;
            this.log      = log;
        }

        @Override
        public @NotNull PhaseSchedule getSchedule() {
            return this.schedule;
        }

        @Override
        public void run(@NotNull Game game) {
            this.log.add(this.name + " " + game.getClock().getFrame() + (game.getRandom().isActive() ? " active" : " inactive"));
        }
    }

    /**
     * Holds two phases of one system.
     */
    private static final class TwoPhaseSystem implements GameSystem {
        private final @NotNull List<@NotNull String> log;
        private final @NotNull FramePhase first;
        private final @NotNull FramePhase second;

        private TwoPhaseSystem(@NotNull List<@NotNull String> log) {
            this.log    = log;
            this.first  = game -> this.log.add("first " + game.getClock().getFrame());
            this.second = game -> this.log.add("second " + game.getClock().getFrame());
        }

        @Override
        public void attach(@NotNull Game game) {
        }
    }

    @Test
    void system_phases_should_resolve_once_per_game_through_their_selectors() {
        List<String> log = new ArrayList<>();
        SystemKey<TwoPhaseSystem> key = SystemKey.of("twoPhases", TwoPhaseSystem.class);
        GameDefinition definition = new GameDefinition.Builder().useSystem(key, context -> new TwoPhaseSystem(log))
                                                                .addFramePhase(key, system -> system.second)
                                                                .addFramePhase(key, system -> system.first)
                                                                .build();
        Game game = Game.create(definition, createSetup());

        game.getFrameRunner().runFrame();

        Assertions.assertEquals(List.of("second 1", "first 1"), log);
        Assertions.assertSame(game.getModSystem(key).second, game.getFrameRunner().getPhases().get(0));
    }

    @Test
    void run_frame_should_apply_commands_then_advance_and_run_phases_in_order() {
        List<String> log = new ArrayList<>();
        Game game = createGame(log);

        game.getFrameRunner().setCommandSource(commandGame -> log.add("commands " + commandGame.getClock().getFrame()));
        game.getFrameRunner().runFrame();
        game.getFrameRunner().runFrame();

        Assertions.assertEquals(List.of("commands 0", "running 1 active", "paused 1 active", "setup 1 active", "every 1 active",
                                        "commands 1", "running 2 active", "paused 2 active", "setup 2 active", "every 2 active"), log);
        Assertions.assertEquals(2, game.getClock().getFrame());
        Assertions.assertFalse(game.getRandom().isActive());
    }

    @Test
    void paused_and_setup_frames_run_their_phases_without_advancing() {
        List<String> log = new ArrayList<>();
        Game game = createGame(log);

        game.getFrameRunner().runSetupFrame();
        game.getClock().setPaused(true);
        game.getFrameRunner().runFrame();

        Assertions.assertEquals(List.of("setup 0 active", "every 0 active", "paused 0 active", "every 0 active"), log);
        Assertions.assertEquals(0, game.getClock().getFrame());
        Assertions.assertFalse(game.getRandom().isActive());
    }

    @Test
    void sync_hash_point_should_report_every_interval_frames_that_advanced() {
        Game game = createGame(new ArrayList<>());
        List<Integer> frames = new ArrayList<>();
        List<Long> hashes = new ArrayList<>();

        game.getFrameRunner().setSyncHashListener((frame, hash) -> {
            frames.add(frame);
            hashes.add(hash);
        }, 2);

        for (int frame = 0; frame < 5; frame++) {
            game.getFrameRunner().runFrame();
        }

        game.getClock().setPaused(true);
        game.getFrameRunner().runFrame();

        Assertions.assertEquals(List.of(2, 4), frames);
        Assertions.assertNotEquals(hashes.get(0), hashes.get(1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> game.getFrameRunner().setSyncHashListener(null, 0));
    }

    @Test
    void failing_phase_should_fail_the_game() {
        GameDefinition definition = new GameDefinition.Builder().addFramePhase(game -> {
            throw Exceptions.newIllegalStateException("The phase fails.");
        }).build();
        Game game = Game.create(definition, createSetup());
        FrameRunner runner = game.getFrameRunner();

        Assertions.assertThrows(IllegalStateException.class, runner::runFrame);
        Assertions.assertTrue(runner.isFailed());
        Assertions.assertFalse(game.getRandom().isActive());
        Assertions.assertThrows(IllegalStateException.class, runner::runFrame);
        Assertions.assertThrows(IllegalStateException.class, runner::runSetupFrame);
    }

    private static @NotNull Game createGame(@NotNull List<@NotNull String> log) {
        GameDefinition definition = new GameDefinition.Builder()
                .addFramePhase(new RecordingPhase("running", PhaseSchedule.RUNNING, log))
                .addFramePhase(new RecordingPhase("paused", PhaseSchedule.RUNNING_AND_PAUSED, log))
                .addFramePhase(new RecordingPhase("setup", PhaseSchedule.RUNNING_AND_SETUP, log))
                .addFramePhase(new RecordingPhase("every", PhaseSchedule.EVERY_FRAME, log))
                .build();

        return Game.create(definition, createSetup());
    }

    private static @NotNull GameSetup createSetup() {
        return new GameSetup.Builder(new MapReference("empty", new byte[0])).build();
    }
}
