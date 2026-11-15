package org.fogofwarlabs.sc4j.engine.client.input;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the key binding table: exact compared modifiers, ignored other modifiers, repeats and replaced bindings.
 */
final class KeyBindingTableTest {
    private static final int SHIFT = KeyModifier.SHIFT.getMask();
    private static final int CONTROL = KeyModifier.CONTROL.getMask();
    private static final int CAPS_LOCK = KeyModifier.CAPS_LOCK.getMask();

    private static final @NotNull String STOP = "stop";
    private static final @NotNull String SELECT_ALL = "select all";
    private static final @NotNull String GROUP = "group";
    private static final @NotNull String HOLD = "hold";

    private static @NotNull String find(@NotNull KeyBindingTable<String> table, @NotNull Key key, int modifiers, boolean repeat) {
        KeyBinding<String> binding = table.find(key, modifiers, repeat);

        Assertions.assertNotNull(binding);

        return binding.getAction();
    }

    @Test
    void find_should_match_the_compared_modifiers_exactly() {
        KeyBindingTable<String> table = new KeyBindingTable<>();

        table.bind(Key.S, 0, STOP, false);
        table.bind(Key.A, CONTROL, SELECT_ALL, false);

        Assertions.assertEquals(STOP, find(table, Key.S, 0, false));
        Assertions.assertEquals(STOP, find(table, Key.S, CAPS_LOCK, false), "Caps Lock is not compared");
        Assertions.assertNull(table.find(Key.S, SHIFT, false));
        Assertions.assertNull(table.find(Key.A, 0, false));
        Assertions.assertEquals(SELECT_ALL, find(table, Key.A, CONTROL, false));
        Assertions.assertNull(table.find(Key.A, CONTROL | SHIFT, false));
        Assertions.assertEquals(SELECT_ALL, table.find(InputEvent.createKey(InputEvent.Type.KEY_PRESSED, Key.A, CONTROL)).getAction());
        Assertions.assertNull(table.find(InputEvent.createKey(InputEvent.Type.KEY_RELEASED, Key.A, CONTROL)), "a release runs nothing");
    }

    @Test
    void find_should_match_repeats_only_of_repeatable_bindings_and_bind_should_replace() {
        KeyBindingTable<String> table = new KeyBindingTable<>(SHIFT);

        table.bind(Key.DIGIT_1, 0, GROUP, true);
        table.bind(Key.H, 0, STOP, false);

        Assertions.assertEquals(GROUP, find(table, Key.DIGIT_1, CONTROL, true), "only Shift is compared");
        Assertions.assertNull(table.find(Key.H, 0, true));

        table.bind(Key.H, CONTROL, HOLD, false);

        Assertions.assertEquals(HOLD, find(table, Key.H, 0, false));
        Assertions.assertEquals(List.of(GROUP, HOLD), table.getBindings().stream().map(KeyBinding::getAction).toList());
        Assertions.assertEquals(0, table.getBindings().getLast().getModifiers(), "the binding keeps the compared modifiers only");
        Assertions.assertThrows(IllegalArgumentException.class, () -> table.bind(Key.UNKNOWN, 0, HOLD, false));
    }

    @Test
    void get_compared_modifiers_should_return_the_mask_within_all_modifiers() {
        Assertions.assertEquals(KeyBindingTable.DEFAULT_COMPARED_MODIFIERS, new KeyBindingTable<String>().getComparedModifiers());
        Assertions.assertEquals(SHIFT | CAPS_LOCK, new KeyBindingTable<String>(SHIFT | CAPS_LOCK).getComparedModifiers());
        Assertions.assertEquals(KeyModifier.ALL_MASK, new KeyBindingTable<String>(-1).getComparedModifiers());
    }
}
