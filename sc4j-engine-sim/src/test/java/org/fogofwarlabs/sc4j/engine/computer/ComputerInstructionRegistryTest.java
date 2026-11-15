package org.fogofwarlabs.sc4j.engine.computer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the computer instruction registry: one instruction per opcode, replacement before the freeze, lookups of any opcode and the freeze.
 */
final class ComputerInstructionRegistryTest {
    private static final int OPCODE_COUNT = 4;

    @Test
    void find_should_return_the_registered_instruction_and_null_outside_the_table() {
        ComputerInstructionRegistry<ComputerInstruction<ComputerScriptContext>> registry = new ComputerInstructionRegistry<>(OPCODE_COUNT);
        ComputerInstruction<ComputerScriptContext> first = context -> true;
        ComputerInstruction<ComputerScriptContext> replaced = context -> false;
        ComputerInstruction<ComputerScriptContext> last = context -> true;

        registry.register(1, first);
        registry.register(1, replaced);
        registry.register(OPCODE_COUNT - 1, last);

        Assertions.assertEquals(OPCODE_COUNT, registry.getCount());
        Assertions.assertSame(replaced, registry.find(1));
        Assertions.assertSame(last, registry.find(OPCODE_COUNT - 1));
        Assertions.assertNull(registry.find(0));
        Assertions.assertNull(registry.find(-1));
        Assertions.assertNull(registry.find(OPCODE_COUNT));
    }

    @Test
    void register_should_reject_opcodes_outside_the_table_and_a_frozen_registry() {
        ComputerInstructionRegistry<ComputerInstruction<ComputerScriptContext>> registry = new ComputerInstructionRegistry<>(OPCODE_COUNT);
        ComputerInstruction<ComputerScriptContext> kept = context -> true;

        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.register(OPCODE_COUNT, kept));
        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.register(-1, kept));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ComputerInstructionRegistry<ComputerInstruction<ComputerScriptContext>>(-1));

        registry.register(0, kept);
        registry.freeze();
        registry.freeze();

        Assertions.assertTrue(registry.isFrozen());
        Assertions.assertThrows(IllegalStateException.class, () -> registry.register(0, context -> false));
        Assertions.assertSame(kept, registry.find(0));
    }
}
