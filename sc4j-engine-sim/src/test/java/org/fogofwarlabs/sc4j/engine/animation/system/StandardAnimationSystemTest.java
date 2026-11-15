package org.fogofwarlabs.sc4j.engine.animation.system;

import org.fogofwarlabs.sc4j.engine.animation.AnimationEffects;
import org.fogofwarlabs.sc4j.engine.animation.AnimationKind;
import org.fogofwarlabs.sc4j.engine.animation.AnimationLibrary;
import org.fogofwarlabs.sc4j.engine.animation.AnimationSet;
import org.fogofwarlabs.sc4j.engine.content.definition.ImageDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.ImageType;
import org.fogofwarlabs.sc4j.engine.content.type.SoundType;
import org.fogofwarlabs.sc4j.engine.entity.Image;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.pack.StandardPack;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the engine-only animation smoke run: a standard pack written by the test, one sprite sheet and one JSON animation
 * with waits, a random wait, a random jump loop and a call, played on one pooled image for a fixed number of frames
 * without any mod.
 */
final class StandardAnimationSystemTest {
    private static final long SEED = 0x5eed_0e4aL;
    private static final int FRAME_COUNT = 40;
    private static final int SNAPSHOT_FRAME = 14;
    private static final int IMAGE_LIMIT = 4;
    private static final int ANIMATION_SOURCE = 9;
    private static final int CALL_STACK_CAPACITY = 1;
    private static final int FRAME_SIZE = 2;
    private static final int FRAME_COLUMNS = 4;
    private static final int OPAQUE = 0xFF000000;
    private static final int RED_SHIFT = 16;
    private static final int LAST_FRAME = FRAME_COLUMNS - 1;

    private static final @NotNull String PACK_MANIFEST = """
                                                         {"format": 1, "animationKinds": ["idle", "walk"], "spriteSheets": {"soldier": "units/soldier.json"}}
                                                         """;
    private static final @NotNull String SOLDIER_DEFINITION = """
                                                              {
                                                                "image": "soldier.png", "frameWidth": 2, "frameHeight": 2, "columns": 4, "rows": 1, "anchor": {"x": 1, "y": 1},
                                                                "animations": {
                                                                  "idle": [{"frame": 0}, {"wait": 2}, {"frame": 1}, {"waitRandom": [1, 3]}, {"randomJump": "walk", "chance": 96},
                                                                           {"jump": "idle"}],
                                                                  "walk": [{"frame": 2}, {"call": "step"}, {"frame": 3}, {"attack": "primary"}, {"wait": 1}, {"jump": "idle"}],
                                                                  "step": [{"wait": 2}, {"return": true}]
                                                                }
                                                              }
                                                              """;

    private static final int @NotNull [] EXPECTED_FRAMES = {0, 0, 1, 0, 0, 1, 1, 1, 2, 2, 3, 0, 0, 1, 2, 2, 3, 0, 0, 1,
                                                            0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 2, 2, 3, 0, 0, 1};
    private static final int @NotNull [] EXPECTED_POSITIONS = {2, 2, 4, 2, 2, 4, 4, 4, 13, 13, 11, 2, 2, 4, 13, 13, 11, 2, 2, 4,
                                                               2, 2, 4, 2, 2, 4, 2, 2, 4, 4, 4, 2, 2, 4, 13, 13, 11, 2, 2, 4};
    private static final int EXPECTED_DRAWS = 17;
    private static final @NotNull List<@NotNull String> EXPECTED_EFFECTS = List.of("11: weapon 1 0", "17: weapon 1 0", "37: weapon 1 0");

    /**
     * Records the effects an animation asks for.
     */
    private static final class RecordingEffects implements AnimationEffects {
        private final @NotNull List<@NotNull String> calls;
        private @Nullable Game game;

        private RecordingEffects() {
            this.calls = new ArrayList<>();
            this.game  = null;
        }

        @Override
        public void playSound(@NotNull Image image, @NotNull SoundType sound) {
            this.calls.add(this.getFrame() + ": sound " + image.getIndex() + " " + sound.getDebugName());
        }

        @Override
        public void spawnOverlay(@NotNull Image image, @NotNull ImageType imageType, int offsetX, int offsetY) {
            this.calls.add(this.getFrame() + ": overlay " + image.getIndex() + " " + imageType.getDebugName());
        }

        @Override
        public void fireWeapon(@NotNull Image image, int weaponSlot, int instructionLimit) {
            this.calls.add(this.getFrame() + ": weapon " + image.getIndex() + " " + weaponSlot);
        }

        @Override
        public void move(@NotNull Image image, int distance) {
            this.calls.add(this.getFrame() + ": move " + image.getIndex() + " " + distance);
        }

        private long getFrame() {
            Game running = this.game;

            return running != null ? running.getClock().getFrame() : -1L;
        }
    }

    /**
     * Holds the observations of one run.
     */
    private static final class Run {
        private final int @NotNull [] frames;
        private final int @NotNull [] positions;
        private final long @NotNull [] hashes;
        private final int draws;
        private final @NotNull List<@NotNull String> effects;

        private Run(int @NotNull [] frames, int @NotNull [] positions, long @NotNull [] hashes, int draws, @NotNull List<@NotNull String> effects) {
            this.frames    = frames;
            this.positions = positions;
            this.hashes    = hashes;
            this.draws     = draws;
            this.effects   = effects;
        }
    }

    @Test
    void standard_pack_animation_runs_with_fixed_frames_waits_and_hash(@TempDir Path packFolder) throws IOException {
        writePack(packFolder);

        try (StandardPack pack = StandardPack.open(packFolder)) {
            AnimationLibrary library = pack.loadAnimations();
            SpriteSheet sheet = pack.loadSpriteSheet(ResourceKey.of("soldier"));
            Run first = run(library);
            Run second = run(library);

            Assertions.assertEquals(FRAME_COLUMNS, sheet.getFrameCount());
            Assertions.assertEquals(OPAQUE | LAST_FRAME << RED_SHIFT, sheet.getColor().getValue(sheet.getFrame(LAST_FRAME).getPixelOffset()));
            Assertions.assertArrayEquals(EXPECTED_FRAMES, first.frames, () -> "frames " + Arrays.toString(first.frames));
            Assertions.assertArrayEquals(EXPECTED_POSITIONS, first.positions, () -> "positions " + Arrays.toString(first.positions));
            Assertions.assertEquals(EXPECTED_DRAWS, first.draws);
            Assertions.assertEquals(EXPECTED_EFFECTS, first.effects, () -> "effects " + first.effects);
            Assertions.assertArrayEquals(first.hashes, second.hashes);
            Assertions.assertArrayEquals(first.frames, second.frames);
        }
    }

    @Test
    void standard_pack_animation_restored_from_a_state_snapshot_continues_with_the_same_frames(@TempDir Path packFolder) throws IOException {
        writePack(packFolder);

        try (StandardPack pack = StandardPack.open(packFolder)) {
            AnimationLibrary library = pack.loadAnimations();
            Game captured = createGame(library, new RecordingEffects());
            long[] expected = new long[FRAME_COUNT - SNAPSHOT_FRAME];
            long[] restoredHashes = new long[FRAME_COUNT - SNAPSHOT_FRAME];

            for (int frame = 0; frame < SNAPSHOT_FRAME; frame++) {
                captured.getFrameRunner().runFrame();
            }

            StateSnapshot snapshot = StateSnapshot.capture(captured);
            Game restored = createGame(library, new RecordingEffects());

            for (int frame = 0; frame < expected.length; frame++) {
                captured.getFrameRunner().runFrame();
                expected[frame] = captured.computeStateHash();
            }

            snapshot.restore(restored);

            for (int frame = 0; frame < restoredHashes.length; frame++) {
                restored.getFrameRunner().runFrame();
                restoredHashes[frame] = restored.computeStateHash();
            }

            Assertions.assertArrayEquals(expected, restoredHashes);
            Assertions.assertEquals(EXPECTED_POSITIONS[FRAME_COUNT - 1],
                                    restored.getModSystem(AnimationSystem.KEY).getState().getPosition(restored.getImages().getUsedSlots().getFirst()));
        }
    }

    private static @NotNull Run run(@NotNull AnimationLibrary library) {
        RecordingEffects effects = new RecordingEffects();
        Game game = createGame(library, effects);
        AnimationSystem animations = game.getModSystem(AnimationSystem.KEY);
        Image image = game.getImages().get(game.getImages().getUsedSlots().getFirst());
        int[] frames = new int[FRAME_COUNT];
        int[] positions = new int[FRAME_COUNT];
        long[] hashes = new long[FRAME_COUNT];

        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            game.getFrameRunner().runFrame();

            frames[frame]    = image.getFrameIndex();
            positions[frame] = animations.getState().getPosition(image.getIndex());
            hashes[frame]    = game.computeStateHash();
        }

        return new Run(frames, positions, hashes, game.getRandom().getCallCount(ANIMATION_SOURCE), List.copyOf(effects.calls));
    }

    /**
     * Creates the game of one run: the animation system over the library and one soldier image playing the idle animation.
     */
    private static @NotNull Game createGame(@NotNull AnimationLibrary library, @NotNull RecordingEffects effects) {
        ImageType soldierType = new ImageType("soldier");
        GameDefinition.Builder builder = new GameDefinition.Builder().setLimits(new EngineLimits.Builder().setImages(IMAGE_LIMIT).build())
                                                                     .useSystem(AnimationSystem.KEY,
                                                                                setup -> new StandardAnimationSystem(library, effects, ANIMATION_SOURCE,
                                                                                                                     CALL_STACK_CAPACITY))
                                                                     .addFramePhase(AnimationSystem.KEY);

        builder.getContent().getImageTypes().register(soldierType, new ImageDefinition.Builder().build());

        GameDefinition definition = builder.build();
        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .setSeed(SEED)
                                                                                       .build();
        Game game = Game.create(definition, setup);
        AnimationSystem animations = game.getModSystem(AnimationSystem.KEY);
        AnimationKind idle = library.findKind("idle");
        AnimationSet soldier = library.findSet("soldier");
        Image image = game.getImages().allocate();

        Assertions.assertNotNull(idle);
        Assertions.assertNotNull(soldier);
        Assertions.assertNotNull(image);

        effects.game = game;
        Assertions.assertEquals(soldier.getIndex(), soldierType.getIndex(), "the image type indexes the soldier set");
        image.setType(soldierType);

        Assertions.assertTrue(animations.play(image, idle));

        return game;
    }

    private static void writePack(@NotNull Path packFolder) throws IOException {
        BufferedImage picture = new BufferedImage(FRAME_SIZE * FRAME_COLUMNS, FRAME_SIZE, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < picture.getWidth(); x++) {
            for (int y = 0; y < picture.getHeight(); y++) {
                picture.setRGB(x, y, OPAQUE | x / FRAME_SIZE << RED_SHIFT);
            }
        }

        Files.createDirectories(packFolder.resolve("units"));
        Files.writeString(packFolder.resolve("pack.json"), PACK_MANIFEST, StandardCharsets.UTF_8);
        Files.writeString(packFolder.resolve("units/soldier.json"), SOLDIER_DEFINITION, StandardCharsets.UTF_8);
        ImageIO.write(picture, "png", packFolder.resolve("units/soldier.png").toFile());
    }
}
