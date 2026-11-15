package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.client.input.KeyModifier;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

/**
 * Maps the GLFW key, button and modifier codes to the engine input types and back, for the engine's GLFW classes. Every GLFW key has an engine
 * {@link Key}, so a GLFW key survives the round trip {@link #getCode(Key)} of {@link #getKey(int)}; a code GLFW does not name
 * ({@link GLFW#GLFW_KEY_UNKNOWN}) is {@link Key#UNKNOWN} and returns as {@link GLFW#GLFW_KEY_UNKNOWN}.
 * <p>
 * The int codes are the GLFW library's own: they cross only between the GLFW callbacks and the engine input types.
 */
public final class GlfwKeyCodes {
    /**
     * The GLFW key of each engine key, by {@link Key#ordinal()}.
     */
    private static final int @NotNull [] CODES = new int[Key.values().length];
    /**
     * The engine key of each GLFW key code, {@code 0} to {@link GLFW#GLFW_KEY_LAST}.
     */
    private static final @NotNull Key @NotNull [] KEYS = new Key[GLFW.GLFW_KEY_LAST + 1];
    /**
     * The GLFW modifier bit of each engine modifier, by {@link KeyModifier#ordinal()}.
     */
    private static final int @NotNull [] MODIFIER_BITS = {GLFW.GLFW_MOD_SHIFT, GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_MOD_ALT, GLFW.GLFW_MOD_SUPER,
                                                          GLFW.GLFW_MOD_CAPS_LOCK, GLFW.GLFW_MOD_NUM_LOCK};
    /**
     * The engine buttons in GLFW button order, {@link GLFW#GLFW_MOUSE_BUTTON_1} first.
     */
    private static final @NotNull PointerButton @NotNull [] BUTTONS = PointerButton.values();
    private static final @NotNull GlfwKeyCodes INSTANCE = new GlfwKeyCodes();

    static {
        Arrays.fill(KEYS, Key.UNKNOWN);

        CODES[Key.UNKNOWN.ordinal()] = GLFW.GLFW_KEY_UNKNOWN;
        put(Key.SPACE, GLFW.GLFW_KEY_SPACE);
        put(Key.APOSTROPHE, GLFW.GLFW_KEY_APOSTROPHE);
        put(Key.COMMA, GLFW.GLFW_KEY_COMMA);
        put(Key.MINUS, GLFW.GLFW_KEY_MINUS);
        put(Key.PERIOD, GLFW.GLFW_KEY_PERIOD);
        put(Key.SLASH, GLFW.GLFW_KEY_SLASH);
        put(Key.DIGIT_0, GLFW.GLFW_KEY_0);
        put(Key.DIGIT_1, GLFW.GLFW_KEY_1);
        put(Key.DIGIT_2, GLFW.GLFW_KEY_2);
        put(Key.DIGIT_3, GLFW.GLFW_KEY_3);
        put(Key.DIGIT_4, GLFW.GLFW_KEY_4);
        put(Key.DIGIT_5, GLFW.GLFW_KEY_5);
        put(Key.DIGIT_6, GLFW.GLFW_KEY_6);
        put(Key.DIGIT_7, GLFW.GLFW_KEY_7);
        put(Key.DIGIT_8, GLFW.GLFW_KEY_8);
        put(Key.DIGIT_9, GLFW.GLFW_KEY_9);
        put(Key.SEMICOLON, GLFW.GLFW_KEY_SEMICOLON);
        put(Key.EQUAL, GLFW.GLFW_KEY_EQUAL);
        put(Key.A, GLFW.GLFW_KEY_A);
        put(Key.B, GLFW.GLFW_KEY_B);
        put(Key.C, GLFW.GLFW_KEY_C);
        put(Key.D, GLFW.GLFW_KEY_D);
        put(Key.E, GLFW.GLFW_KEY_E);
        put(Key.F, GLFW.GLFW_KEY_F);
        put(Key.G, GLFW.GLFW_KEY_G);
        put(Key.H, GLFW.GLFW_KEY_H);
        put(Key.I, GLFW.GLFW_KEY_I);
        put(Key.J, GLFW.GLFW_KEY_J);
        put(Key.K, GLFW.GLFW_KEY_K);
        put(Key.L, GLFW.GLFW_KEY_L);
        put(Key.M, GLFW.GLFW_KEY_M);
        put(Key.N, GLFW.GLFW_KEY_N);
        put(Key.O, GLFW.GLFW_KEY_O);
        put(Key.P, GLFW.GLFW_KEY_P);
        put(Key.Q, GLFW.GLFW_KEY_Q);
        put(Key.R, GLFW.GLFW_KEY_R);
        put(Key.S, GLFW.GLFW_KEY_S);
        put(Key.T, GLFW.GLFW_KEY_T);
        put(Key.U, GLFW.GLFW_KEY_U);
        put(Key.V, GLFW.GLFW_KEY_V);
        put(Key.W, GLFW.GLFW_KEY_W);
        put(Key.X, GLFW.GLFW_KEY_X);
        put(Key.Y, GLFW.GLFW_KEY_Y);
        put(Key.Z, GLFW.GLFW_KEY_Z);
        put(Key.LEFT_BRACKET, GLFW.GLFW_KEY_LEFT_BRACKET);
        put(Key.BACKSLASH, GLFW.GLFW_KEY_BACKSLASH);
        put(Key.RIGHT_BRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET);
        put(Key.GRAVE_ACCENT, GLFW.GLFW_KEY_GRAVE_ACCENT);
        put(Key.WORLD_1, GLFW.GLFW_KEY_WORLD_1);
        put(Key.WORLD_2, GLFW.GLFW_KEY_WORLD_2);
        put(Key.ESCAPE, GLFW.GLFW_KEY_ESCAPE);
        put(Key.ENTER, GLFW.GLFW_KEY_ENTER);
        put(Key.TAB, GLFW.GLFW_KEY_TAB);
        put(Key.BACKSPACE, GLFW.GLFW_KEY_BACKSPACE);
        put(Key.INSERT, GLFW.GLFW_KEY_INSERT);
        put(Key.DELETE, GLFW.GLFW_KEY_DELETE);
        put(Key.RIGHT, GLFW.GLFW_KEY_RIGHT);
        put(Key.LEFT, GLFW.GLFW_KEY_LEFT);
        put(Key.DOWN, GLFW.GLFW_KEY_DOWN);
        put(Key.UP, GLFW.GLFW_KEY_UP);
        put(Key.PAGE_UP, GLFW.GLFW_KEY_PAGE_UP);
        put(Key.PAGE_DOWN, GLFW.GLFW_KEY_PAGE_DOWN);
        put(Key.HOME, GLFW.GLFW_KEY_HOME);
        put(Key.END, GLFW.GLFW_KEY_END);
        put(Key.CAPS_LOCK, GLFW.GLFW_KEY_CAPS_LOCK);
        put(Key.SCROLL_LOCK, GLFW.GLFW_KEY_SCROLL_LOCK);
        put(Key.NUM_LOCK, GLFW.GLFW_KEY_NUM_LOCK);
        put(Key.PRINT_SCREEN, GLFW.GLFW_KEY_PRINT_SCREEN);
        put(Key.PAUSE, GLFW.GLFW_KEY_PAUSE);
        put(Key.F1, GLFW.GLFW_KEY_F1);
        put(Key.F2, GLFW.GLFW_KEY_F2);
        put(Key.F3, GLFW.GLFW_KEY_F3);
        put(Key.F4, GLFW.GLFW_KEY_F4);
        put(Key.F5, GLFW.GLFW_KEY_F5);
        put(Key.F6, GLFW.GLFW_KEY_F6);
        put(Key.F7, GLFW.GLFW_KEY_F7);
        put(Key.F8, GLFW.GLFW_KEY_F8);
        put(Key.F9, GLFW.GLFW_KEY_F9);
        put(Key.F10, GLFW.GLFW_KEY_F10);
        put(Key.F11, GLFW.GLFW_KEY_F11);
        put(Key.F12, GLFW.GLFW_KEY_F12);
        put(Key.F13, GLFW.GLFW_KEY_F13);
        put(Key.F14, GLFW.GLFW_KEY_F14);
        put(Key.F15, GLFW.GLFW_KEY_F15);
        put(Key.F16, GLFW.GLFW_KEY_F16);
        put(Key.F17, GLFW.GLFW_KEY_F17);
        put(Key.F18, GLFW.GLFW_KEY_F18);
        put(Key.F19, GLFW.GLFW_KEY_F19);
        put(Key.F20, GLFW.GLFW_KEY_F20);
        put(Key.F21, GLFW.GLFW_KEY_F21);
        put(Key.F22, GLFW.GLFW_KEY_F22);
        put(Key.F23, GLFW.GLFW_KEY_F23);
        put(Key.F24, GLFW.GLFW_KEY_F24);
        put(Key.F25, GLFW.GLFW_KEY_F25);
        put(Key.KEYPAD_0, GLFW.GLFW_KEY_KP_0);
        put(Key.KEYPAD_1, GLFW.GLFW_KEY_KP_1);
        put(Key.KEYPAD_2, GLFW.GLFW_KEY_KP_2);
        put(Key.KEYPAD_3, GLFW.GLFW_KEY_KP_3);
        put(Key.KEYPAD_4, GLFW.GLFW_KEY_KP_4);
        put(Key.KEYPAD_5, GLFW.GLFW_KEY_KP_5);
        put(Key.KEYPAD_6, GLFW.GLFW_KEY_KP_6);
        put(Key.KEYPAD_7, GLFW.GLFW_KEY_KP_7);
        put(Key.KEYPAD_8, GLFW.GLFW_KEY_KP_8);
        put(Key.KEYPAD_9, GLFW.GLFW_KEY_KP_9);
        put(Key.KEYPAD_DECIMAL, GLFW.GLFW_KEY_KP_DECIMAL);
        put(Key.KEYPAD_DIVIDE, GLFW.GLFW_KEY_KP_DIVIDE);
        put(Key.KEYPAD_MULTIPLY, GLFW.GLFW_KEY_KP_MULTIPLY);
        put(Key.KEYPAD_SUBTRACT, GLFW.GLFW_KEY_KP_SUBTRACT);
        put(Key.KEYPAD_ADD, GLFW.GLFW_KEY_KP_ADD);
        put(Key.KEYPAD_ENTER, GLFW.GLFW_KEY_KP_ENTER);
        put(Key.KEYPAD_EQUAL, GLFW.GLFW_KEY_KP_EQUAL);
        put(Key.LEFT_SHIFT, GLFW.GLFW_KEY_LEFT_SHIFT);
        put(Key.LEFT_CONTROL, GLFW.GLFW_KEY_LEFT_CONTROL);
        put(Key.LEFT_ALT, GLFW.GLFW_KEY_LEFT_ALT);
        put(Key.LEFT_SUPER, GLFW.GLFW_KEY_LEFT_SUPER);
        put(Key.RIGHT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT);
        put(Key.RIGHT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL);
        put(Key.RIGHT_ALT, GLFW.GLFW_KEY_RIGHT_ALT);
        put(Key.RIGHT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER);
        put(Key.MENU, GLFW.GLFW_KEY_MENU);
    }

    private static void put(@NotNull Key key, int code) {
        CODES[key.ordinal()] = code;
        KEYS[code]           = key;
    }

    /**
     * @return the shared codes
     */
    public static @NotNull GlfwKeyCodes getInstance() {
        return INSTANCE;
    }

    private GlfwKeyCodes() {
    }

    /**
     * Returns the engine key of a GLFW key code.
     *
     * @param code the GLFW key code, any value
     * @return the key, or {@link Key#UNKNOWN} if GLFW names no key by the code
     */
    public @NotNull Key getKey(int code) {
        return code >= 0 && code < KEYS.length ? KEYS[code] : Key.UNKNOWN;
    }

    /**
     * Returns the GLFW key code of an engine key.
     *
     * @param key the key
     * @return the GLFW key code, {@link GLFW#GLFW_KEY_UNKNOWN} for {@link Key#UNKNOWN}
     */
    public int getCode(@NotNull Key key) {
        return CODES[key.ordinal()];
    }

    /**
     * Returns the engine button of a GLFW mouse button.
     *
     * @param code the GLFW mouse button, any value
     * @return the button, or {@code null} if GLFW names no button by the code
     */
    public @Nullable PointerButton getButton(int code) {
        return code >= 0 && code < BUTTONS.length ? BUTTONS[code] : null;
    }

    /**
     * Returns the GLFW mouse button of an engine button.
     *
     * @param button the button
     * @return the GLFW mouse button
     */
    public int getButtonCode(@NotNull PointerButton button) {
        return button.ordinal();
    }

    /**
     * Returns the engine modifier mask of GLFW modifier bits.
     *
     * @param bits the GLFW modifier bits
     * @return the engine modifier mask ({@link KeyModifier#getMask()} bits)
     */
    public int getModifiers(int bits) {
        int modifiers = 0;

        for (KeyModifier modifier : KeyModifier.values()) {
            if ((bits & MODIFIER_BITS[modifier.ordinal()]) != 0) {
                modifiers |= modifier.getMask();
            }
        }

        return modifiers;
    }

    /**
     * Returns the GLFW modifier bits of an engine modifier mask.
     *
     * @param modifiers the engine modifier mask ({@link KeyModifier#getMask()} bits)
     * @return the GLFW modifier bits
     */
    public int getModifierBits(int modifiers) {
        int bits = 0;

        for (KeyModifier modifier : KeyModifier.values()) {
            if (modifier.isIn(modifiers)) {
                bits |= MODIFIER_BITS[modifier.ordinal()];
            }
        }

        return bits;
    }

    /**
     * Creates an {@link InputEvent} instance of a GLFW key event.
     *
     * @param key       the GLFW key code
     * @param action    the GLFW action: {@link GLFW#GLFW_PRESS}, {@link GLFW#GLFW_REPEAT} or {@link GLFW#GLFW_RELEASE}
     * @param modifiers the GLFW modifier bits
     * @return the created {@link InputEvent} instance
     */
    public @NotNull InputEvent createKeyEvent(int key, int action, int modifiers) {
        InputEvent.Type type = action == GLFW.GLFW_PRESS ? InputEvent.Type.KEY_PRESSED
                                                         : action == GLFW.GLFW_REPEAT ? InputEvent.Type.KEY_REPEATED : InputEvent.Type.KEY_RELEASED;

        return InputEvent.createKey(type, this.getKey(key), this.getModifiers(modifiers));
    }
}
