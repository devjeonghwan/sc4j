package org.fogofwarlabs.sc4j.engine.save;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentityException;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentityMismatch;
import org.fogofwarlabs.sc4j.engine.replay.ReplayGameScenario;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Tests the engine save format with an engine-only skirmish game: a game saved to a file and loaded into a new game reaches the saved game's
 * state hash in every later frame, and a saved game of another mod identity, a truncated, damaged or foreign file and a newer format version
 * are refused.
 */
final class EngineSaveFormatTest {
    private static final int SAVE_FRAME = 700;
    private static final int LATER_FRAME_COUNT = 800;
    private static final int TRUNCATED_BYTES = 7;
    private static final int FORMAT_VERSION_OFFSET = 5;

    private static long @NotNull [] runFrames(@NotNull Game game, int count) {
        long[] hashes = new long[count];

        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
            hashes[frame] = game.computeStateHash();
        }

        return hashes;
    }


    private final @NotNull ReplayGameScenario scenario;
    private final @NotNull EngineSaveFormat format;

    EngineSaveFormatTest() throws IOException, ManifestException {
        this.scenario = new ReplayGameScenario();
        this.format   = new EngineSaveFormat(ReplayGameScenario.createIdentity("1.0.0", 1), this.scenario.getSetupCodec());
    }

    @Test
    void saved_skirmish_loaded_from_its_file_reaches_the_saved_game_state_hash_every_later_frame() throws IOException {
        Game saved = this.scenario.createGame(this.scenario.createSetup(ReplayGameScenario.SEED));

        runFrames(saved, SAVE_FRAME);

        byte[] contents = this.format.write(this.format.createSave(saved));
        long[] expected = runFrames(saved, LATER_FRAME_COUNT);
        EngineSave loaded = this.format.read(contents);
        SaveSummary summary = this.format.readSummary(contents);
        Game resumed = this.scenario.createGame(loaded.getSetup());

        loaded.restore(resumed);

        Assertions.assertTrue(this.format.isSave(contents));
        Assertions.assertEquals(EngineSaveFormat.FILE_EXTENSION, this.format.getFileExtension());
        Assertions.assertEquals(saved.getSetup().getMap().getName(), summary.getMapName());
        Assertions.assertEquals(SAVE_FRAME, summary.getFrame());
        Assertions.assertEquals(saved.getSetup().getPlayers().size(), summary.getPlayerCount());
        Assertions.assertEquals(ReplayGameScenario.SEED, loaded.getSetup().getSeed());
        Assertions.assertEquals(SAVE_FRAME, resumed.getClock().getFrame());
        Assertions.assertArrayEquals(expected, runFrames(resumed, LATER_FRAME_COUNT));
    }

    @Test
    void read_should_refuse_a_saved_game_of_another_mod_identity_with_the_differing_part() throws ManifestException {
        Game saved = this.scenario.createGame(this.scenario.createSetup(ReplayGameScenario.SEED));
        byte[] contents = this.format.write(this.format.createSave(saved));
        EngineSaveFormat newer = new EngineSaveFormat(ReplayGameScenario.createIdentity("1.0.1", 1), this.scenario.getSetupCodec());
        ModIdentity changedIdentity = ReplayGameScenario.createIdentity("1.0.0", 2);
        EngineSaveFormat changed = new EngineSaveFormat(changedIdentity, this.scenario.getSetupCodec());

        ModIdentityException version = Assertions.assertThrows(ModIdentityException.class, () -> newer.read(contents));
        ModIdentityException content = Assertions.assertThrows(ModIdentityException.class, () -> changed.readSummary(contents));

        Assertions.assertEquals(ModIdentityMismatch.MOD_VERSION, version.getMismatch());
        Assertions.assertEquals(this.format.getIdentity(), version.getRecordedIdentity());
        Assertions.assertTrue(version.getMessage().startsWith("The saved game was written by "), version.getMessage());
        Assertions.assertEquals(ModIdentityMismatch.MOD_CONTENT, content.getMismatch());
    }

    @Test
    void read_should_refuse_a_truncated_damaged_foreign_or_newer_file_with_a_clear_error() {
        Game saved = this.scenario.createGame(this.scenario.createSetup(ReplayGameScenario.SEED));

        runFrames(saved, SAVE_FRAME);

        byte[] contents = this.format.write(this.format.createSave(saved));
        byte[] damaged = contents.clone();
        byte[] newer = contents.clone();

        damaged[contents.length / 2] ^= 1;
        newer[FORMAT_VERSION_OFFSET]++;

        this.assertRefused(Arrays.copyOf(contents, contents.length - TRUNCATED_BYTES), "The engine saved game is truncated or damaged");
        this.assertRefused(damaged, "The engine saved game is truncated or damaged");
        this.assertRefused("not a saved game".getBytes(StandardCharsets.US_ASCII), "The file is not an engine saved game.");
        this.assertRefused(new byte[0], "The file is not an engine saved game.");
        this.assertRefused(newer, "The engine saved game has the format version 2; this engine reads version 1.");
        Assertions.assertFalse(this.format.isSave("SC4R engine replay".getBytes(StandardCharsets.US_ASCII)));
    }

    private void assertRefused(byte @NotNull [] contents, @NotNull String message) {
        IOException exception = Assertions.assertThrows(IOException.class, () -> this.format.read(contents));

        Assertions.assertFalse(exception instanceof ModIdentityException, exception.getMessage());
        Assertions.assertTrue(exception.getMessage().startsWith(message), exception.getMessage());
    }
}
