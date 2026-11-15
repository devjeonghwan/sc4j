package org.fogofwarlabs.sc4j.engine.replay;

import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.PlayerSlot;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentityException;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentityMismatch;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the engine replay format: a recorded skirmish game survives it with its identity, setup, loop count, commands and checkpoints, its
 * summary names its map, length and players, a replay of another mod identity is refused with the differing part as its reason, and a
 * truncated, damaged, foreign or newer file is refused with an error that says so.
 */
final class EngineReplayFormatTest {
    private static final int LOOP_COUNT = 700;
    private static final int TRUNCATED_BYTES = 9;
    private static final int FORMAT_VERSION_OFFSET = 5;

    private final @NotNull ReplayGameScenario scenario;
    private final @NotNull EngineReplayFormat format;

    EngineReplayFormatTest() throws IOException, ManifestException {
        this.scenario = new ReplayGameScenario();
        this.format   = this.scenario.createFormat(ReplayGameScenario.createIdentity("1.0.0", 1));
    }

    @Test
    void read_should_return_the_replay_that_write_wrote() throws IOException {
        EngineReplay recorded = this.scenario.record(this.format, LOOP_COUNT, new ArrayList<>());
        byte[] contents = this.format.write(recorded);
        EngineReplay read = this.format.read(contents);
        GameSetup setup = read.getSetup();

        Assertions.assertEquals(this.format.getIdentity(), read.getIdentity());
        Assertions.assertEquals(recorded.getSetup().getPlayers(), setup.getPlayers());
        Assertions.assertEquals(ReplayGameScenario.SEED, setup.getSeed());
        Assertions.assertEquals(recorded.getSetup().getMap().getName(), setup.getMap().getName());
        Assertions.assertArrayEquals(recorded.getSetup().getMap().getDigest(), setup.getMap().getDigest());
        Assertions.assertEquals(LOOP_COUNT, read.getLoopCount());
        Assertions.assertEquals(ReplayGameScenario.ORDER_LOOPS.length, read.getCommandCount());
        Assertions.assertEquals((LOOP_COUNT - 1) / ReplayGameScenario.CHECKPOINT_INTERVAL, read.getCheckpointCount());
        Assertions.assertArrayEquals(contents, this.format.write(read));
    }

    @Test
    void read_summary_should_return_the_map_length_and_the_playing_slots_of_the_replay() throws IOException {
        EngineReplay recorded = this.scenario.record(this.format, LOOP_COUNT, new ArrayList<>());
        byte[] contents = this.format.write(recorded);
        ReplaySummary summary = this.format.readSummary(contents);
        List<PlayerSlot> playing = recorded.getSetup().getPlayers().stream()
                                           .filter(slot -> slot.getControllerKind() != ControllerKind.NEUTRAL &&
                                                           slot.getControllerKind() != ControllerKind.OBSERVER)
                                           .toList();

        Assertions.assertTrue(this.format.isReplay(contents));
        Assertions.assertFalse(this.format.isReplay("not a replay at all".getBytes(StandardCharsets.US_ASCII)));
        Assertions.assertEquals(EngineReplayFormat.FILE_EXTENSION, this.format.getFileExtension());
        Assertions.assertEquals(recorded.getSetup().getMap().getName(), summary.getMapName());
        Assertions.assertEquals(LOOP_COUNT, summary.getFrameCount());
        Assertions.assertEquals(playing, summary.getPlayers(), "the neutral slot is no player of the list");
        Assertions.assertEquals(playing.size(), summary.getPlayerCount());
    }

    @Test
    void read_should_refuse_a_replay_of_another_mod_identity_with_the_differing_part() throws ManifestException {
        byte[] contents = this.format.write(this.scenario.record(this.format, 1, new ArrayList<>()));
        ModIdentity newer = ReplayGameScenario.createIdentity("1.0.1", 1);
        ModIdentity changed = ReplayGameScenario.createIdentity("1.0.0", 2);

        ModIdentityException version = Assertions.assertThrows(ModIdentityException.class, () -> this.scenario.createFormat(newer).read(contents));
        ModIdentityException content = Assertions.assertThrows(ModIdentityException.class, () -> this.scenario.createFormat(changed).read(contents));

        Assertions.assertEquals(ModIdentityMismatch.MOD_VERSION, version.getMismatch());
        Assertions.assertEquals(this.format.getIdentity(), version.getRecordedIdentity());
        Assertions.assertTrue(version.getMessage().endsWith("in its mod version."), version.getMessage());
        Assertions.assertEquals(ModIdentityMismatch.MOD_CONTENT, content.getMismatch());
    }

    @Test
    void read_should_refuse_a_truncated_damaged_foreign_or_newer_file_with_a_clear_error() {
        byte[] contents = this.format.write(this.scenario.record(this.format, LOOP_COUNT, new ArrayList<>()));
        byte[] damaged = contents.clone();
        byte[] newer = contents.clone();

        damaged[contents.length / 2] ^= 1;
        newer[FORMAT_VERSION_OFFSET]++;

        this.assertRefused(Arrays.copyOf(contents, contents.length - TRUNCATED_BYTES), "The engine replay is truncated or damaged");
        this.assertRefused(damaged, "The engine replay is truncated or damaged");
        this.assertRefused("not a replay at all".getBytes(StandardCharsets.US_ASCII), "The file is not an engine replay.");
        this.assertRefused(new byte[0], "The file is not an engine replay.");
        this.assertRefused(newer, "The engine replay has the format version 2; this engine reads version 1.");
    }

    private void assertRefused(byte @NotNull [] contents, @NotNull String message) {
        IOException exception = Assertions.assertThrows(IOException.class, () -> this.format.read(contents));

        Assertions.assertFalse(exception instanceof ModIdentityException, exception.getMessage());
        Assertions.assertTrue(exception.getMessage().startsWith(message), exception.getMessage());
    }
}
