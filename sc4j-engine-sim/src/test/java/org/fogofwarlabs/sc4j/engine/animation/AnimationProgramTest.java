package org.fogofwarlabs.sc4j.engine.animation;

import org.fogofwarlabs.sc4j.engine.animation.instruction.Call;
import org.fogofwarlabs.sc4j.engine.animation.instruction.End;
import org.fogofwarlabs.sc4j.engine.animation.instruction.Jump;
import org.fogofwarlabs.sc4j.engine.animation.instruction.ShowFrame;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the checks of the animation model: resolved labels, branch targets and successor tables of a program, the entries
 * of a set and the references of a library.
 */
final class AnimationProgramTest {
    private static final @NotNull AnimationKind IDLE = AnimationKind.of(0, "idle");
    private static final @NotNull AnimationKind WALK = AnimationKind.of(1, "walk");

    @Test
    void program_resolves_labels_and_keeps_branch_targets_inside() {
        AnimationProgram program = new AnimationProgram(0, "unit", List.of(new ShowFrame(0), new Jump(0), new ShowFrame(1), new Call(0)),
                                                        List.of(new AnimationLabel("idle", 0), new AnimationLabel("walk", 2)));
        AnimationLabel walk = program.findLabel("walk");

        Assertions.assertNotNull(walk);
        Assertions.assertEquals(2, walk.getPosition());
        Assertions.assertNull(program.findLabel("death"));
        Assertions.assertEquals(4, program.getLength());
        Assertions.assertTrue(program.contains(3));
        Assertions.assertFalse(program.contains(4));
    }

    @Test
    void program_rejects_targets_and_labels_outside_and_duplicate_labels() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimationProgram(0, "jump", List.of(new Jump(1)), List.of()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimationProgram(0, "call", List.of(End.INSTANCE, new Call(2)), List.of()));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new AnimationProgram(0, "label", List.of(End.INSTANCE), List.of(new AnimationLabel("late", 1))));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new AnimationProgram(0, "twice", List.of(End.INSTANCE, End.INSTANCE),
                                                           List.of(new AnimationLabel("a", 0), new AnimationLabel("a", 1))));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimationProgram(0, "empty", List.of(), List.of()));
    }

    @Test
    void get_next_position_should_follow_the_successor_table_or_the_following_position() {
        List<AnimationInstruction> instructions = List.of(new ShowFrame(0), new ShowFrame(1), End.INSTANCE);
        AnimationProgram table = new AnimationProgram(0, "native", instructions, List.of(), new int[]{2, 3, 3});
        AnimationProgram plain = new AnimationProgram(0, "plain", instructions, List.of());

        Assertions.assertTrue(table.hasSuccessorTable());
        Assertions.assertEquals(2, table.getNextPosition(0));
        Assertions.assertEquals(3, table.getNextPosition(1));
        Assertions.assertFalse(plain.hasSuccessorTable());
        Assertions.assertEquals(1, plain.getNextPosition(0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> plain.getNextPosition(3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimationProgram(0, "short", instructions, List.of(), new int[]{1, 2}));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimationProgram(0, "outside", instructions, List.of(), new int[]{1, 4, 3}));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimationProgram(0, "negative", instructions, List.of(), new int[]{-1, 2, 3}));
    }

    @Test
    void set_maps_kinds_to_program_entries() {
        AnimationProgram program = new AnimationProgram(0, "unit", List.of(new ShowFrame(0), End.INSTANCE, new ShowFrame(1), End.INSTANCE),
                                                        List.of());
        AnimationSet set = new AnimationSet.Builder(0, "unit").setAnimation(WALK, program, 2).build();

        Assertions.assertSame(program, set.getProgram(WALK));
        Assertions.assertEquals(2, set.getStartPosition(WALK));
        Assertions.assertNull(set.getProgram(IDLE));
        Assertions.assertEquals(AnimationSet.NO_START_POSITION, set.getStartPosition(IDLE));
        Assertions.assertFalse(set.contains(AnimationKind.of(1, "walk")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimationSet.Builder(0, "late").setAnimation(IDLE, program, 4));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new AnimationSet.Builder(0, "twice").setAnimation(IDLE, program, 0).setAnimation(IDLE, program, 2));
    }

    @Test
    void library_rejects_misplaced_elements_and_foreign_references() {
        AnimationProgram program = new AnimationProgram(0, "unit", List.of(End.INSTANCE), List.of());
        AnimationProgram foreign = new AnimationProgram(0, "foreign", List.of(End.INSTANCE), List.of());
        AnimationSet set = new AnimationSet.Builder(0, "unit").setAnimation(IDLE, program, 0).build();
        AnimationLibrary library = new AnimationLibrary(List.of(IDLE, WALK), List.of(program), List.of(set));

        Assertions.assertSame(WALK, library.findKind("walk"));
        Assertions.assertSame(set, library.findSet("unit"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimationLibrary(List.of(WALK), List.of(program), List.of()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimationLibrary(List.of(IDLE, WALK), List.of(foreign), List.of(set)));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new AnimationLibrary(List.of(AnimationKind.of(0, "idle")), List.of(program), List.of(set)));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new AnimationLibrary(List.of(IDLE, AnimationKind.of(1, "idle")), List.of(program), List.of()));
    }
}
