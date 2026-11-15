package org.fogofwarlabs.sc4j.engine.state.snapshot;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.replay.ReplayGameScenario;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;

/**
 * Tests state snapshots of an engine-only skirmish game (the engine default systems and pools, two computer players): a snapshot restored into
 * another game of the same definition, even one that already ran with another seed, reaches the captured game's state hash in every later
 * frame, and a snapshot whose wire form is cut short or whose values were changed is refused.
 */
final class StateSnapshotTest {
    private static final int SNAPSHOT_FRAME = 900;
    private static final int LATER_FRAME_COUNT = 900;
    private static final int DISTURBING_FRAME_COUNT = 50;
    /**
     * The position of the lowest byte of the captured frame number in the wire form: after the frame, the state hash and the values length come
     * the values, which start with the frame clock.
     */
    private static final int CLOCK_FRAME_LOW_BYTE = Integer.BYTES + Long.BYTES + Integer.BYTES + Integer.BYTES - 1;

    private static long @NotNull [] runFrames(@NotNull Game game, int count) {
        long[] hashes = new long[count];

        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
            hashes[frame] = game.computeStateHash();
        }

        return hashes;
    }

    private static byte @NotNull [] writeWireForm(@NotNull StateSnapshot snapshot) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);

        snapshot.write(output);
        output.flush();

        return bytes.toByteArray();
    }

    private static @NotNull StateSnapshot readWireForm(byte @NotNull [] contents) throws IOException {
        return StateSnapshot.read(new DataInputStream(new ByteArrayInputStream(contents)));
    }


    private final @NotNull ReplayGameScenario scenario;

    StateSnapshotTest() throws IOException {
        this.scenario = new ReplayGameScenario();
    }

    @Test
    void restored_skirmish_reaches_the_captured_game_state_hash_in_every_later_frame() throws IOException {
        GameSetup setup = this.scenario.createSetup(ReplayGameScenario.SEED);
        Game captured = this.scenario.createGame(setup);

        runFrames(captured, SNAPSHOT_FRAME);

        StateSnapshot snapshot = readWireForm(writeWireForm(StateSnapshot.capture(captured)));
        long[] expected = runFrames(captured, LATER_FRAME_COUNT);
        Game restored = this.scenario.createGame(this.scenario.createSetup(ReplayGameScenario.SEED + 1));

        runFrames(restored, DISTURBING_FRAME_COUNT);
        snapshot.restore(restored);

        Assertions.assertEquals(SNAPSHOT_FRAME, snapshot.getFrame());
        Assertions.assertEquals(SNAPSHOT_FRAME, restored.getClock().getFrame());
        Assertions.assertEquals(snapshot.getStateHash(), restored.computeStateHash());
        Assertions.assertArrayEquals(expected, runFrames(restored, LATER_FRAME_COUNT));
        Assertions.assertTrue(captured.getUnits().getUsedSlots().getSize() > 0);
    }

    @Test
    void restore_should_refuse_a_cut_or_changed_snapshot() throws IOException {
        Game captured = this.scenario.createGame(this.scenario.createSetup(ReplayGameScenario.SEED));

        runFrames(captured, SNAPSHOT_FRAME);

        byte[] contents = writeWireForm(StateSnapshot.capture(captured));
        byte[] changed = contents.clone();

        changed[CLOCK_FRAME_LOW_BYTE]++;

        StateSnapshot changedSnapshot = readWireForm(changed);
        Game target = this.scenario.createGame(this.scenario.createSetup(ReplayGameScenario.SEED));

        Assertions.assertThrows(IOException.class, () -> readWireForm(Arrays.copyOf(contents, contents.length - 1)));
        Assertions.assertThrows(IOException.class, () -> changedSnapshot.restore(target));
    }
}
