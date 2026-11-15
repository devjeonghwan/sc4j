package org.fogofwarlabs.sc4j.engine.animation.system;

import org.fogofwarlabs.sc4j.engine.animation.AnimationProgram;
import org.fogofwarlabs.sc4j.engine.animation.instruction.End;
import org.fogofwarlabs.sc4j.engine.animation.instruction.ShowFrame;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests restoring the animation state one value at a time: positions kept before the program, the program check and the
 * kind index.
 */
final class AnimationStateTest {
    private static final int OBJECT = 1;
    private static final int STATE_LENGTH = 2;
    private static final int KIND_INDEX = 200;

    @Test
    void set_position_should_keep_a_position_until_the_program_is_set() {
        AnimationState state = new AnimationState(STATE_LENGTH, 1);
        AnimationProgram program = new AnimationProgram(0, "test", List.of(new ShowFrame(0), new ShowFrame(1), End.INSTANCE), List.of());

        state.setPosition(OBJECT, 2);

        Assertions.assertEquals(2, state.getPosition(OBJECT));
        Assertions.assertNull(state.getProgram(OBJECT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> state.setPosition(OBJECT, -1));

        state.setProgram(OBJECT, program);

        Assertions.assertSame(program, state.getProgram(OBJECT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> state.setPosition(OBJECT, 3));

        state.clear(OBJECT);
        state.setPosition(OBJECT, 5);

        Assertions.assertThrows(IllegalArgumentException.class, () -> state.setProgram(OBJECT, program));
    }

    @Test
    void set_kind_index_should_store_any_kind_index_from_no_kind() {
        AnimationState state = new AnimationState(STATE_LENGTH, 1);

        Assertions.assertEquals(AnimationState.NO_KIND, state.getKindIndex(OBJECT));

        state.setKindIndex(OBJECT, KIND_INDEX);

        Assertions.assertEquals(KIND_INDEX, state.getKindIndex(OBJECT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> state.setKindIndex(OBJECT, AnimationState.NO_KIND - 1));
    }
}
