package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.client.input.KeyModifier;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.Set;

/**
 * Tests the mapping between the GLFW key, button and modifier codes and the engine input types (the default host key codes).
 */
final class GlfwKeyCodesTest {
    private static final @NotNull String KEY_PREFIX = "GLFW_KEY_";
    private static final @NotNull String LAST_KEY = "GLFW_KEY_LAST";
    private static final @NotNull String UNKNOWN_KEY = "GLFW_KEY_UNKNOWN";

    @Test
    void every_glfw_key_has_its_own_engine_key_and_returns_to_its_code() throws IllegalAccessException {
        Set<Key> named = EnumSet.noneOf(Key.class);

        for (Field field : GLFW.class.getFields()) {
            String name = field.getName();

            if (!name.startsWith(KEY_PREFIX) || name.equals(LAST_KEY) || name.equals(UNKNOWN_KEY) || !Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            int code = field.getInt(null);
            Key key = GlfwKeyCodes.getInstance().getKey(code);

            Assertions.assertNotEquals(Key.UNKNOWN, key, name);
            Assertions.assertEquals(code, GlfwKeyCodes.getInstance().getCode(key), name);
            Assertions.assertTrue(named.add(key), name);
        }

        Assertions.assertEquals(Key.values().length - 1, named.size(), "every engine key but UNKNOWN is a GLFW key");
        Assertions.assertEquals(Key.UNKNOWN, GlfwKeyCodes.getInstance().getKey(GLFW.GLFW_KEY_UNKNOWN));
        Assertions.assertEquals(Key.UNKNOWN, GlfwKeyCodes.getInstance().getKey(GLFW.GLFW_KEY_LAST + 1));
        Assertions.assertEquals(GLFW.GLFW_KEY_UNKNOWN, GlfwKeyCodes.getInstance().getCode(Key.UNKNOWN));
    }

    @Test
    void buttons_and_modifiers_return_to_their_glfw_codes() {
        Assertions.assertEquals(PointerButton.LEFT, GlfwKeyCodes.getInstance().getButton(GLFW.GLFW_MOUSE_BUTTON_LEFT));
        Assertions.assertEquals(PointerButton.RIGHT, GlfwKeyCodes.getInstance().getButton(GLFW.GLFW_MOUSE_BUTTON_RIGHT));
        Assertions.assertEquals(PointerButton.MIDDLE, GlfwKeyCodes.getInstance().getButton(GLFW.GLFW_MOUSE_BUTTON_MIDDLE));
        Assertions.assertNull(GlfwKeyCodes.getInstance().getButton(GLFW.GLFW_MOUSE_BUTTON_LAST + 1));

        for (PointerButton button : PointerButton.values()) {
            Assertions.assertEquals(button, GlfwKeyCodes.getInstance().getButton(GlfwKeyCodes.getInstance().getButtonCode(button)));
        }

        int bits = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_NUM_LOCK;
        int modifiers = GlfwKeyCodes.getInstance().getModifiers(bits);

        Assertions.assertEquals(KeyModifier.SHIFT.getMask() | KeyModifier.ALT.getMask() | KeyModifier.NUM_LOCK.getMask(), modifiers);
        Assertions.assertEquals(bits, GlfwKeyCodes.getInstance().getModifierBits(modifiers));
    }

    @Test
    void create_key_event_should_map_action_key_and_modifiers() {
        InputEvent event = GlfwKeyCodes.getInstance().createKeyEvent(GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_REPEAT, GLFW.GLFW_MOD_CONTROL);

        Assertions.assertEquals(InputEvent.Type.KEY_REPEATED, event.getType());
        Assertions.assertEquals(Key.KEYPAD_ENTER, event.getKey());
        Assertions.assertTrue(event.isModifierDown(KeyModifier.CONTROL));
        Assertions.assertEquals(InputEvent.Type.KEY_PRESSED,
                                GlfwKeyCodes.getInstance().createKeyEvent(GLFW.GLFW_KEY_A, GLFW.GLFW_PRESS, 0).getType());
        Assertions.assertEquals(InputEvent.Type.KEY_RELEASED,
                                GlfwKeyCodes.getInstance().createKeyEvent(GLFW.GLFW_KEY_A, GLFW.GLFW_RELEASE, 0).getType());
    }
}
