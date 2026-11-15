package org.fogofwarlabs.sc4j.engine.client.input;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the input event factories, their neutral values and the modifier masks.
 */
final class InputEventTest {
    private static final float POINTER_X = 12.5f;
    private static final float POINTER_Y = 7.25f;
    private static final double WHEEL_OFFSET = -2;
    private static final int CODE_POINT = 0xAC00;

    @Test
    void create_key_should_hold_key_and_modifiers_and_reject_other_types() {
        int modifiers = KeyModifier.SHIFT.getMask() | KeyModifier.ALT.getMask();
        InputEvent event = InputEvent.createKey(InputEvent.Type.KEY_REPEATED, Key.F5, modifiers);

        Assertions.assertEquals(InputEvent.Type.KEY_REPEATED, event.getType());
        Assertions.assertEquals(Key.F5, event.getKey());
        Assertions.assertTrue(event.isKeyEvent());
        Assertions.assertTrue(event.isKeyDown());
        Assertions.assertTrue(event.isModifierDown(KeyModifier.SHIFT));
        Assertions.assertTrue(event.isModifierDown(KeyModifier.ALT));
        Assertions.assertFalse(event.isModifierDown(KeyModifier.CONTROL));
        Assertions.assertNull(event.getButton());
        Assertions.assertFalse(InputEvent.createKey(InputEvent.Type.KEY_RELEASED, Key.F5, 0).isKeyDown());
        Assertions.assertThrows(IllegalArgumentException.class, () -> InputEvent.createKey(InputEvent.Type.POINTER_MOVED, Key.F5, 0));
    }

    @Test
    void pointer_character_and_wheel_events_hold_their_values_and_neutral_others() {
        InputEvent character = InputEvent.createCharacterTyped(CODE_POINT);
        InputEvent press = InputEvent.createButtonPressed(PointerButton.RIGHT, KeyModifier.CONTROL.getMask(), POINTER_X, POINTER_Y, true);
        InputEvent wheel = InputEvent.createWheelScrolled(0, WHEEL_OFFSET, POINTER_X, POINTER_Y);

        Assertions.assertEquals(CODE_POINT, character.getCodePoint());
        Assertions.assertEquals(Key.UNKNOWN, character.getKey());
        Assertions.assertFalse(character.isKeyEvent());
        Assertions.assertEquals(PointerButton.RIGHT, press.getButton());
        Assertions.assertEquals(POINTER_X, press.getX());
        Assertions.assertEquals(POINTER_Y, press.getY());
        Assertions.assertTrue(press.isDoublePress());
        Assertions.assertTrue(press.isModifierDown(KeyModifier.CONTROL));
        Assertions.assertFalse(InputEvent.createButtonReleased(PointerButton.RIGHT, 0, POINTER_X, POINTER_Y).isDoublePress());
        Assertions.assertEquals(WHEEL_OFFSET, wheel.getVerticalOffset());
        Assertions.assertEquals(0, wheel.getHorizontalOffset());
        Assertions.assertEquals(POINTER_X, InputEvent.createPointerMoved(POINTER_X, POINTER_Y).getX());
    }

    @Test
    void create_at_should_copy_the_event_at_another_position() {
        InputEvent press = InputEvent.createButtonPressed(PointerButton.LEFT, KeyModifier.SHIFT.getMask(), POINTER_X, POINTER_Y, true);
        InputEvent moved = press.createAt(POINTER_Y, POINTER_X);

        Assertions.assertTrue(press.isPointerEvent());
        Assertions.assertFalse(InputEvent.createCharacterTyped(CODE_POINT).isPointerEvent());
        Assertions.assertEquals(InputEvent.Type.BUTTON_PRESSED, moved.getType());
        Assertions.assertEquals(PointerButton.LEFT, moved.getButton());
        Assertions.assertTrue(moved.isModifierDown(KeyModifier.SHIFT));
        Assertions.assertTrue(moved.isDoublePress());
        Assertions.assertEquals(POINTER_Y, moved.getX());
        Assertions.assertEquals(POINTER_X, moved.getY());
    }

    @Test
    void modifier_masks_follow_the_declaration_order() {
        int all = 0;

        for (KeyModifier modifier : KeyModifier.values()) {
            Assertions.assertEquals(1 << modifier.ordinal(), modifier.getMask());
            Assertions.assertTrue(modifier.isIn(KeyModifier.ALL_MASK));

            all |= modifier.getMask();
        }

        Assertions.assertEquals(KeyModifier.ALL_MASK, all);
    }
}
