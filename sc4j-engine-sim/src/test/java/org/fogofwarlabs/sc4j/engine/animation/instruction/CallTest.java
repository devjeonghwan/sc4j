package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.engine.animation.*;
import org.fogofwarlabs.sc4j.engine.animation.system.AnimationRunner;
import org.fogofwarlabs.sc4j.engine.animation.system.AnimationState;
import org.fogofwarlabs.sc4j.engine.content.type.ImageType;
import org.fogofwarlabs.sc4j.engine.content.type.SoundType;
import org.fogofwarlabs.sc4j.engine.entity.Image;
import org.fogofwarlabs.sc4j.engine.random.PcgRandom;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests that a call returns to the next position of the calling instruction, also in a program with a successor table.
 */
final class CallTest {
    private static final int OBJECT = 1;
    private static final int STATE_LENGTH = 2;
    private static final int CALL_STACK_CAPACITY = 1;
    private static final int RANDOM_SOURCE = 0;
    private static final long SEED = 42L;
    private static final int INSTRUCTION_LIMIT = 100;
    private static final int SKIPPED_FRAME = 9;
    private static final int CALLED_FRAME = 1;
    private static final int RETURNED_FRAME = 2;
    private static final @NotNull AnimationKind MAIN = AnimationKind.of(0, "main");
    private static final @NotNull Image OBJECT_IMAGE = new Image(OBJECT);

    /**
     * Ignores every effect.
     */
    private static final class NoEffects implements AnimationEffects {
        @Override
        public void playSound(@NotNull Image image, @NotNull SoundType sound) {
        }

        @Override
        public void spawnOverlay(@NotNull Image image, @NotNull ImageType imageType, int offsetX, int offsetY) {
        }

        @Override
        public void fireWeapon(@NotNull Image image, int weaponSlot, int instructionLimit) {
        }

        @Override
        public void move(@NotNull Image image, int distance) {
        }
    }

    @Test
    void execute_should_push_the_successor_position_when_the_program_has_a_successor_table() {
        int[] frames = new int[STATE_LENGTH];
        List<AnimationInstruction> instructions = List.of(new Call(3), new ShowFrame(SKIPPED_FRAME), new ShowFrame(RETURNED_FRAME),
                                                          new ShowFrame(CALLED_FRAME), new Wait(1), Return.INSTANCE, new Wait(1), End.INSTANCE);
        AnimationProgram program = new AnimationProgram(0, "native", instructions, List.of(new AnimationLabel("main", 0)),
                                                        new int[]{2, 6, 6, 4, 5, 6, 7, 8});
        AnimationSet set = new AnimationSet.Builder(0, "test").setAnimation(MAIN, program, 0).build();
        PcgRandom random = new PcgRandom(SEED, PcgRandom.DEFAULT_STREAM, PcgRandom.DEFAULT_SOURCE_COUNT);
        AnimationRunner runner = new AnimationRunner(new AnimationState(STATE_LENGTH, CALL_STACK_CAPACITY),
                                                     (image, frame) -> frames[image.getIndex()] = frame, new NoEffects(),
                                                     random, RANDOM_SOURCE, INSTRUCTION_LIMIT);

        Assertions.assertTrue(runner.start(OBJECT_IMAGE, set, MAIN));

        runner.update(OBJECT_IMAGE);

        Assertions.assertEquals(CALLED_FRAME, frames[OBJECT]);
        Assertions.assertEquals(2, runner.getState().getCallEntry(OBJECT, 0));

        runner.update(OBJECT_IMAGE);

        Assertions.assertEquals(RETURNED_FRAME, frames[OBJECT]);
        Assertions.assertEquals(0, runner.getState().getCallDepth(OBJECT));
    }
}
