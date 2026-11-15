package org.fogofwarlabs.sc4j.engine.animation.system;

import org.fogofwarlabs.sc4j.engine.animation.*;
import org.fogofwarlabs.sc4j.engine.animation.instruction.*;
import org.fogofwarlabs.sc4j.engine.content.type.ImageType;
import org.fogofwarlabs.sc4j.engine.content.type.SoundType;
import org.fogofwarlabs.sc4j.engine.entity.Image;
import org.fogofwarlabs.sc4j.engine.random.PcgRandom;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests running animation programs: wait timing, calls and returns, the end, the failure limits, random draws, an
 * instruction a mod adds, successor tables, bounded updates and nested updates.
 */
final class AnimationRunnerTest {
    private static final int OBJECT = 1;
    private static final int OTHER_OBJECT = 0;
    private static final int STATE_LENGTH = 2;
    private static final int RANDOM_SOURCE = 3;
    private static final long SEED = 42L;
    private static final int INSTRUCTION_LIMIT = 100;
    private static final int NO_FRAME = -1;
    private static final @NotNull AnimationKind MAIN = AnimationKind.of(0, "main");
    private static final @NotNull Image OBJECT_IMAGE = new Image(OBJECT);
    private static final @NotNull Image OTHER_IMAGE = new Image(OTHER_OBJECT);

    /**
     * Records the frame each object shows.
     */
    private static final class FrameTarget implements AnimationTarget {
        private final int @NotNull [] frames;

        private FrameTarget(int length) {
            this.frames = new int[length];

            Arrays.fill(this.frames, NO_FRAME);
        }

        @Override
        public void showFrame(@NotNull Image image, int frame) {
            this.frames[image.getIndex()] = frame;
        }
    }

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

    /**
     * A mod instruction with single-slot call semantics: it stores the next position in call stack entry {@code 0},
     * overwriting it, and jumps.
     */
    private static final class SlotCall extends AnimationInstruction {
        private final int targetPosition;

        private SlotCall(int targetPosition) {
            this.targetPosition = targetPosition;
        }

        @Override
        public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
            context.setCallEntry(0, context.getPosition() + 1);
            context.setCallDepth(1);
            context.jump(this.targetPosition);

            return AnimationStep.CONTINUE;
        }
    }

    /**
     * A mod instruction that returns to call stack entry {@code 0} without popping it.
     */
    private static final class SlotReturn extends AnimationInstruction {
        @Override
        public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
            context.jump(context.getCallEntry(0));

            return AnimationStep.CONTINUE;
        }
    }

    /**
     * A mod instruction that shows the frame of its target's own counter, reaching the target through the context.
     */
    private static final class ShowNextFrame extends AnimationInstruction {
        @Override
        public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
            FrameTarget target = (FrameTarget) context.getTarget();

            target.showFrame(context.getImage(), target.frames[context.getImage().getIndex()] + 1);

            return AnimationStep.CONTINUE;
        }
    }

    @Test
    void update_should_resume_in_the_update_wait_ticks_later() {
        FrameTarget target = new FrameTarget(STATE_LENGTH);
        AnimationRunner runner = createRunner(target, List.of(new ShowFrame(1), new Wait(3), new ShowFrame(2), End.INSTANCE));
        int[] frames = new int[5];

        for (int update = 0; update < frames.length; update++) {
            runner.update(OBJECT_IMAGE);
            frames[update] = target.frames[OBJECT];
        }

        Assertions.assertArrayEquals(new int[]{1, 1, 1, 2, 2}, frames);
        Assertions.assertFalse(runner.getState().isRunning(OBJECT));
        Assertions.assertEquals(4, runner.getState().getPosition(OBJECT));
    }

    @Test
    void call_and_return_should_resume_after_the_call() {
        FrameTarget target = new FrameTarget(STATE_LENGTH);
        AnimationRunner runner = createRunner(target, List.of(new Call(4), new ShowFrame(2), new Wait(1), new Jump(0),
                                                              new ShowFrame(1), new Wait(1), Return.INSTANCE));

        runner.update(OBJECT_IMAGE);

        Assertions.assertEquals(1, target.frames[OBJECT]);
        Assertions.assertEquals(1, runner.getState().getCallDepth(OBJECT));
        Assertions.assertEquals(1, runner.getState().getCallEntry(OBJECT, 0));

        runner.update(OBJECT_IMAGE);

        Assertions.assertEquals(2, target.frames[OBJECT]);
        Assertions.assertEquals(0, runner.getState().getCallDepth(OBJECT));
    }

    @Test
    void call_should_fail_when_the_call_stack_is_full() {
        AnimationRunner runner = createRunner(new FrameTarget(STATE_LENGTH), List.of(new Call(1), new Call(0)));

        Assertions.assertThrows(IllegalStateException.class, () -> runner.update(OBJECT_IMAGE));
    }

    @Test
    void return_should_fail_when_the_call_stack_is_empty() {
        AnimationRunner runner = createRunner(new FrameTarget(STATE_LENGTH), List.of(Return.INSTANCE));

        Assertions.assertThrows(IllegalStateException.class, () -> runner.update(OBJECT_IMAGE));
    }

    @Test
    void update_should_fail_at_the_instruction_limit_and_past_the_end() {
        AnimationRunner looping = createRunner(new FrameTarget(STATE_LENGTH), List.of(new ShowFrame(0), new Jump(0)));
        AnimationRunner falling = createRunner(new FrameTarget(STATE_LENGTH), List.of(new ShowFrame(0)));

        Assertions.assertThrows(IllegalStateException.class, () -> looping.update(OBJECT_IMAGE));
        Assertions.assertEquals(0, looping.getState().getPosition(OBJECT));
        Assertions.assertThrows(IllegalStateException.class, () -> falling.update(OBJECT_IMAGE));
    }

    @Test
    void random_instructions_draw_once_each_from_the_animation_source() {
        PcgRandom random = new PcgRandom(SEED, PcgRandom.DEFAULT_STREAM, PcgRandom.DEFAULT_SOURCE_COUNT);
        AnimationRunner runner = createRunner(new FrameTarget(STATE_LENGTH), random,
                                              List.of(new RandomJump(0, 0), new RandomJump(RandomJump.CHANCE_RANGE, 2), new ShowFrame(0),
                                                      new WaitRandom(2, 5), End.INSTANCE));

        random.setActive(true);
        runner.update(OBJECT_IMAGE);

        int wait = runner.getState().getWait(OBJECT);

        Assertions.assertEquals(3, random.getCallCount(RANDOM_SOURCE));
        Assertions.assertTrue(wait >= 1 && wait <= 4, () -> "wait timer " + wait);
        Assertions.assertEquals(4, runner.getState().getPosition(OBJECT));
    }

    @Test
    void mod_instructions_use_the_call_stack_and_target_through_the_context() {
        FrameTarget target = new FrameTarget(STATE_LENGTH);
        AnimationRunner runner = createRunner(target, List.of(new ShowFrame(4), new SlotCall(5), new SlotCall(5), new Wait(1), End.INSTANCE,
                                                              new ShowNextFrame(), new SlotReturn()));

        runner.update(OBJECT_IMAGE);

        Assertions.assertEquals(6, target.frames[OBJECT]);
        Assertions.assertEquals(1, runner.getState().getCallDepth(OBJECT));
        Assertions.assertEquals(3, runner.getState().getCallEntry(OBJECT, 0));
        Assertions.assertEquals(4, runner.getState().getPosition(OBJECT));
    }

    @Test
    void update_should_go_on_at_the_successor_table_positions() {
        FrameTarget target = new FrameTarget(STATE_LENGTH);
        AnimationProgram program = new AnimationProgram(0, "native", List.of(new ShowFrame(1), new ShowFrame(9), new Wait(1), End.INSTANCE),
                                                        List.of(), new int[]{2, 3, 3, 4});
        AnimationRunner runner = createRunner(target, new PcgRandom(SEED, PcgRandom.DEFAULT_STREAM, PcgRandom.DEFAULT_SOURCE_COUNT), program);

        runner.update(OBJECT_IMAGE);

        Assertions.assertEquals(1, target.frames[OBJECT]);
        Assertions.assertEquals(3, runner.getState().getPosition(OBJECT));
        Assertions.assertTrue(runner.getState().isRunning(OBJECT));
    }

    @Test
    void bounded_update_should_store_each_position_and_fail_before_the_instruction_past_its_budget() {
        FrameTarget target = new FrameTarget(STATE_LENGTH);
        AnimationRunner runner = createRunner(target, List.of(new ShowFrame(1), new ShowFrame(2), new Wait(1), End.INSTANCE));
        InstructionBudget budget = new InstructionBudget(2);

        InstructionBudgetExhaustedException failure = Assertions.assertThrows(InstructionBudgetExhaustedException.class,
                                                                              () -> runner.update(OBJECT_IMAGE, budget));

        Assertions.assertEquals(OBJECT, failure.getIndex());
        Assertions.assertEquals(2, failure.getPosition());
        Assertions.assertEquals(2, runner.getState().getPosition(OBJECT));
        Assertions.assertEquals(2, target.frames[OBJECT]);
        Assertions.assertEquals(0, budget.getRemaining());

        budget.reset(1);
        runner.update(OBJECT_IMAGE, budget);

        Assertions.assertEquals(3, runner.getState().getPosition(OBJECT));
        Assertions.assertEquals(0, budget.getRemaining());
    }

    @Test
    void nested_updates_keep_their_own_positions_and_share_the_budget() {
        FrameTarget target = new FrameTarget(STATE_LENGTH);
        AnimationRunner[] holder = new AnimationRunner[1];
        AnimationInstruction updateOther = new AnimationInstruction() {
            @Override
            public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
                InstructionBudget shared = context.getInstructionBudget();

                Assertions.assertNotNull(shared);
                holder[0].update(OTHER_IMAGE, shared);

                return AnimationStep.CONTINUE;
            }
        };
        AnimationRunner runner = createRunner(target, List.of(updateOther, new ShowFrame(3), new Wait(1), End.INSTANCE));
        InstructionBudget budget = new InstructionBudget(INSTRUCTION_LIMIT);

        AnimationSet set = runner.getState().getSet(OBJECT);
        AnimationProgram program = runner.getState().getProgram(OBJECT);

        Assertions.assertNotNull(set);
        Assertions.assertNotNull(program);

        holder[0] = runner;
        runner.getState().start(OTHER_OBJECT, set, MAIN.getIndex(), program, 1);
        runner.update(OBJECT_IMAGE, budget);

        Assertions.assertEquals(3, target.frames[OBJECT]);
        Assertions.assertEquals(3, target.frames[OTHER_OBJECT]);
        Assertions.assertEquals(3, runner.getState().getPosition(OBJECT));
        Assertions.assertEquals(3, runner.getState().getPosition(OTHER_OBJECT));
        Assertions.assertEquals(INSTRUCTION_LIMIT - 5, budget.getRemaining());
    }

    @Test
    void count_down_should_count_only_a_waiting_running_object_down() {
        AnimationRunner runner = createRunner(new FrameTarget(STATE_LENGTH), List.of(new Wait(3), End.INSTANCE));

        Assertions.assertFalse(runner.countDown(OBJECT_IMAGE));

        runner.update(OBJECT_IMAGE);

        Assertions.assertTrue(runner.countDown(OBJECT_IMAGE));
        Assertions.assertTrue(runner.countDown(OBJECT_IMAGE));
        Assertions.assertEquals(0, runner.getState().getWait(OBJECT));
        Assertions.assertFalse(runner.countDown(OBJECT_IMAGE));
        Assertions.assertFalse(runner.countDown(OTHER_IMAGE));
    }

    @Test
    void start_should_report_a_kind_the_set_does_not_have() {
        AnimationRunner runner = createRunner(new FrameTarget(STATE_LENGTH), List.of(End.INSTANCE));
        AnimationSet empty = new AnimationSet.Builder(1, "empty").build();

        Assertions.assertFalse(runner.start(OBJECT_IMAGE, empty, MAIN));
        Assertions.assertTrue(runner.getState().isRunning(OBJECT));
    }

    private static @NotNull AnimationRunner createRunner(@NotNull FrameTarget target, @NotNull List<@NotNull AnimationInstruction> instructions) {
        return createRunner(target, new PcgRandom(SEED, PcgRandom.DEFAULT_STREAM, PcgRandom.DEFAULT_SOURCE_COUNT), instructions);
    }

    private static @NotNull AnimationRunner createRunner(@NotNull FrameTarget target, @NotNull PcgRandom random,
                                                         @NotNull List<@NotNull AnimationInstruction> instructions) {
        return createRunner(target, random, new AnimationProgram(0, "test", instructions, List.of(new AnimationLabel("main", 0))));
    }

    private static @NotNull AnimationRunner createRunner(@NotNull FrameTarget target, @NotNull PcgRandom random, @NotNull AnimationProgram program) {
        AnimationSet set = new AnimationSet.Builder(0, "test").setAnimation(MAIN, program, 0).build();
        AnimationRunner runner = new AnimationRunner(new AnimationState(STATE_LENGTH, 1), target, new NoEffects(),
                                                     random, RANDOM_SOURCE, INSTRUCTION_LIMIT);

        Assertions.assertTrue(runner.start(OBJECT_IMAGE, set, MAIN));

        return runner;
    }
}
