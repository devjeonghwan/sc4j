package org.fogofwarlabs.sc4j.engine.replay;

import org.fogofwarlabs.sc4j.engine.client.game.GameSession;
import org.fogofwarlabs.sc4j.engine.controller.ControllerCommandSource;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameSetupCodec;
import org.fogofwarlabs.sc4j.engine.lockstep.InMemoryLockstepNetwork;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepGameScenario;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepPeer;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshot;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests replay playback through the engine replay format: a recorded skirmish game played back from its file reaches the recorded state hash
 * every frame without checkpoint mismatch, a game that differs from the recording is reported at every checkpoint, and the replay one peer of a
 * two-peer lockstep game recorded reaches both peers' state hashes every frame; a seek (a state snapshot of a replayed game restored into a new
 * game, played on from its loop) reaches the recorded state hash in every later frame.
 */
final class ReplayControllerTest {
    private static final int LOOP_COUNT = 1500;
    private static final int LOCKSTEP_FRAME_COUNT = 1200;
    private static final int PATROL_FRAME = 40;
    private static final int MOVE_FRAME = 600;
    /**
     * The game loop of the replay seek: just after the person queued the scout's third order.
     */
    private static final int SEEK_LOOP = 302;

    private final @NotNull ReplayGameScenario scenario;
    private final @NotNull EngineReplayFormat format;

    ReplayControllerTest() throws IOException, ManifestException {
        this.scenario = new ReplayGameScenario();
        this.format   = this.scenario.createFormat(ReplayGameScenario.createIdentity("1.0.0", 1));
    }

    @Test
    void skirmish_replay_reaches_the_recorded_state_hash_every_frame() throws IOException {
        List<Long> live = new ArrayList<>();
        List<Long> played = new ArrayList<>();
        EngineReplay replay = this.format.read(this.format.write(this.scenario.record(this.format, LOOP_COUNT, live)));
        ReplayController controller = this.scenario.play(replay, this.scenario.createGame(replay.getSetup()), null, played);

        Assertions.assertEquals(LOOP_COUNT, live.size());
        Assertions.assertEquals(live, played);
        Assertions.assertEquals(LOOP_COUNT, controller.getLoop());
        Assertions.assertTrue(replay.getCheckpointCount() > 0);
        Assertions.assertEquals(0, controller.getMismatchCount());
    }

    @Test
    void replay_seek_from_a_state_snapshot_reaches_the_recorded_state_hash_every_later_frame() throws IOException {
        List<Long> live = new ArrayList<>();
        EngineReplay replay = this.format.read(this.format.write(this.scenario.record(this.format, LOOP_COUNT, live)));
        Game replayed = this.scenario.createGame(replay.getSetup());
        GameSession replayedSession = this.scenario.createReplaySession(replayed, replay.createController(null));

        for (int loop = 0; loop < SEEK_LOOP; loop++) {
            Assertions.assertTrue(replayedSession.runFrame());
        }

        StateSnapshot snapshot = StateSnapshot.capture(replayed);
        Game sought = this.scenario.createGame(replay.getSetup());
        ReplayController controller = replay.createController(null, SEEK_LOOP);
        GameSession session = this.scenario.createReplaySession(sought, controller);
        List<Long> played = new ArrayList<>();

        snapshot.restore(sought);

        while (!controller.isFinished()) {
            Assertions.assertTrue(session.runFrame());
            played.add(sought.computeStateHash());
        }

        Assertions.assertEquals(live.subList(SEEK_LOOP, LOOP_COUNT), played);
        Assertions.assertEquals(0, controller.getMismatchCount());
        Assertions.assertThrows(IllegalArgumentException.class, () -> replay.createController(null, LOOP_COUNT + 1));
    }

    @Test
    void replay_of_another_game_reports_a_mismatch_at_every_checkpoint() throws IOException {
        List<Long> mismatchLoops = new ArrayList<>();
        EngineReplay replay = this.format.read(this.format.write(this.scenario.record(this.format, LOOP_COUNT, new ArrayList<>())));
        Game otherGame = this.scenario.createGame(this.scenario.createSetup(ReplayGameScenario.SEED + 1));
        ReplayController controller = this.scenario.play(replay, otherGame, (loop, recordedHash, localHash, mismatchCount) -> {
            Assertions.assertNotEquals(recordedHash, localHash);
            mismatchLoops.add(loop);
        }, new ArrayList<>());

        Assertions.assertEquals(replay.getCheckpointCount(), controller.getMismatchCount());
        Assertions.assertEquals(replay.getCheckpointCount(), mismatchLoops.size());
        Assertions.assertEquals(ReplayGameScenario.CHECKPOINT_INTERVAL, mismatchLoops.getFirst());
    }

    @Test
    void replay_of_a_lockstep_game_reaches_every_peer_state_hash_every_frame() throws IOException, ManifestException {
        LockstepGameScenario lockstep = new LockstepGameScenario();
        LockstepPeer first = new LockstepPeer(0);
        LockstepPeer second = new LockstepPeer(1);
        InMemoryLockstepNetwork network = new InMemoryLockstepNetwork(List.of(first, second));
        List<LockstepGameScenario.Machine> machines = List.of(
                lockstep.createMachine(first, network.getTransport(first), 2, LockstepGameScenario.SEED, PATROL_FRAME),
                lockstep.createMachine(second, network.getTransport(second), 2, LockstepGameScenario.SEED, MOVE_FRAME)
        );
        GameSetupCodec setupCodec = new GameSetupCodec(List.of(lockstep.getScenario().getWardens(), lockstep.getScenario().getRaiders()), List.of());
        EngineReplayFormat lockstepFormat = new EngineReplayFormat(ReplayGameScenario.createIdentity("1.0.0", 1), lockstep.getCommands(), setupCodec);
        ReplayRecorder recorder = lockstepFormat.createRecorder(lockstep.createSetup(LockstepGameScenario.SEED),
                                                                ReplayGameScenario.CHECKPOINT_INTERVAL);

        machines.getFirst().setRecorder(recorder);
        LockstepGameScenario.run(machines, LOCKSTEP_FRAME_COUNT, () -> {
        });

        EngineReplay replay = lockstepFormat.read(lockstepFormat.write(recorder.createReplay()));
        ReplayController controller = replay.createController(null);
        Game game = lockstep.createGame(replay.getSetup());
        List<Long> played = new ArrayList<>();

        game.getFrameRunner().setCommandSource(new ControllerCommandSource(List.of(controller)));

        while (!controller.isFinished()) {
            game.getFrameRunner().runFrame();
            played.add(game.computeStateHash());
        }

        Assertions.assertEquals(2, replay.getCommandCount());
        Assertions.assertEquals(machines.get(0).getFrameHashes(), played);
        Assertions.assertEquals(machines.get(1).getFrameHashes(), played);
        Assertions.assertEquals(0, controller.getMismatchCount());
    }
}
