package org.fogofwarlabs.sc4j.engine.client.input;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Names the keys of a keyboard by their position on a US layout, independent of the platform's key codes; a host input adapter maps
 * its key codes to these keys ({@link org.fogofwarlabs.sc4j.engine.client.host.WindowInput} of the host). A key the adapter
 * cannot name is {@link #UNKNOWN}. Typed text arrives as characters ({@link InputEvent.Type#CHARACTER_TYPED}), not as keys.
 */
public enum Key {
    /**
     * A key without a name here.
     */
    UNKNOWN,
    /**
     * The space bar.
     */
    SPACE,
    /**
     * The apostrophe key.
     */
    APOSTROPHE,
    /**
     * The comma key.
     */
    COMMA,
    /**
     * The minus key.
     */
    MINUS,
    /**
     * The period key.
     */
    PERIOD,
    /**
     * The slash key.
     */
    SLASH,
    /**
     * The digit 0 of the main keyboard.
     */
    DIGIT_0,
    /**
     * The digit 1 of the main keyboard.
     */
    DIGIT_1,
    /**
     * The digit 2 of the main keyboard.
     */
    DIGIT_2,
    /**
     * The digit 3 of the main keyboard.
     */
    DIGIT_3,
    /**
     * The digit 4 of the main keyboard.
     */
    DIGIT_4,
    /**
     * The digit 5 of the main keyboard.
     */
    DIGIT_5,
    /**
     * The digit 6 of the main keyboard.
     */
    DIGIT_6,
    /**
     * The digit 7 of the main keyboard.
     */
    DIGIT_7,
    /**
     * The digit 8 of the main keyboard.
     */
    DIGIT_8,
    /**
     * The digit 9 of the main keyboard.
     */
    DIGIT_9,
    /**
     * The semicolon key.
     */
    SEMICOLON,
    /**
     * The equal sign key.
     */
    EQUAL,
    /**
     * The letter A.
     */
    A,
    /**
     * The letter B.
     */
    B,
    /**
     * The letter C.
     */
    C,
    /**
     * The letter D.
     */
    D,
    /**
     * The letter E.
     */
    E,
    /**
     * The letter F.
     */
    F,
    /**
     * The letter G.
     */
    G,
    /**
     * The letter H.
     */
    H,
    /**
     * The letter I.
     */
    I,
    /**
     * The letter J.
     */
    J,
    /**
     * The letter K.
     */
    K,
    /**
     * The letter L.
     */
    L,
    /**
     * The letter M.
     */
    M,
    /**
     * The letter N.
     */
    N,
    /**
     * The letter O.
     */
    O,
    /**
     * The letter P.
     */
    P,
    /**
     * The letter Q.
     */
    Q,
    /**
     * The letter R.
     */
    R,
    /**
     * The letter S.
     */
    S,
    /**
     * The letter T.
     */
    T,
    /**
     * The letter U.
     */
    U,
    /**
     * The letter V.
     */
    V,
    /**
     * The letter W.
     */
    W,
    /**
     * The letter X.
     */
    X,
    /**
     * The letter Y.
     */
    Y,
    /**
     * The letter Z.
     */
    Z,
    /**
     * The left bracket key.
     */
    LEFT_BRACKET,
    /**
     * The backslash key.
     */
    BACKSLASH,
    /**
     * The right bracket key.
     */
    RIGHT_BRACKET,
    /**
     * The grave accent key.
     */
    GRAVE_ACCENT,
    /**
     * The first non-US key.
     */
    WORLD_1,
    /**
     * The second non-US key.
     */
    WORLD_2,
    /**
     * Escape.
     */
    ESCAPE,
    /**
     * Enter of the main keyboard.
     */
    ENTER,
    /**
     * Tab.
     */
    TAB,
    /**
     * Backspace.
     */
    BACKSPACE,
    /**
     * Insert.
     */
    INSERT,
    /**
     * Delete.
     */
    DELETE,
    /**
     * The right arrow.
     */
    RIGHT,
    /**
     * The left arrow.
     */
    LEFT,
    /**
     * The down arrow.
     */
    DOWN,
    /**
     * The up arrow.
     */
    UP,
    /**
     * Page Up.
     */
    PAGE_UP,
    /**
     * Page Down.
     */
    PAGE_DOWN,
    /**
     * Home.
     */
    HOME,
    /**
     * End.
     */
    END,
    /**
     * Caps Lock.
     */
    CAPS_LOCK,
    /**
     * Scroll Lock.
     */
    SCROLL_LOCK,
    /**
     * Num Lock.
     */
    NUM_LOCK,
    /**
     * Print Screen.
     */
    PRINT_SCREEN,
    /**
     * Pause.
     */
    PAUSE,
    /**
     * The function key F1.
     */
    F1,
    /**
     * The function key F2.
     */
    F2,
    /**
     * The function key F3.
     */
    F3,
    /**
     * The function key F4.
     */
    F4,
    /**
     * The function key F5.
     */
    F5,
    /**
     * The function key F6.
     */
    F6,
    /**
     * The function key F7.
     */
    F7,
    /**
     * The function key F8.
     */
    F8,
    /**
     * The function key F9.
     */
    F9,
    /**
     * The function key F10.
     */
    F10,
    /**
     * The function key F11.
     */
    F11,
    /**
     * The function key F12.
     */
    F12,
    /**
     * The function key F13.
     */
    F13,
    /**
     * The function key F14.
     */
    F14,
    /**
     * The function key F15.
     */
    F15,
    /**
     * The function key F16.
     */
    F16,
    /**
     * The function key F17.
     */
    F17,
    /**
     * The function key F18.
     */
    F18,
    /**
     * The function key F19.
     */
    F19,
    /**
     * The function key F20.
     */
    F20,
    /**
     * The function key F21.
     */
    F21,
    /**
     * The function key F22.
     */
    F22,
    /**
     * The function key F23.
     */
    F23,
    /**
     * The function key F24.
     */
    F24,
    /**
     * The function key F25.
     */
    F25,
    /**
     * The digit 0 of the keypad.
     */
    KEYPAD_0,
    /**
     * The digit 1 of the keypad.
     */
    KEYPAD_1,
    /**
     * The digit 2 of the keypad.
     */
    KEYPAD_2,
    /**
     * The digit 3 of the keypad.
     */
    KEYPAD_3,
    /**
     * The digit 4 of the keypad.
     */
    KEYPAD_4,
    /**
     * The digit 5 of the keypad.
     */
    KEYPAD_5,
    /**
     * The digit 6 of the keypad.
     */
    KEYPAD_6,
    /**
     * The digit 7 of the keypad.
     */
    KEYPAD_7,
    /**
     * The digit 8 of the keypad.
     */
    KEYPAD_8,
    /**
     * The digit 9 of the keypad.
     */
    KEYPAD_9,
    /**
     * The decimal key of the keypad.
     */
    KEYPAD_DECIMAL,
    /**
     * The divide key of the keypad.
     */
    KEYPAD_DIVIDE,
    /**
     * The multiply key of the keypad.
     */
    KEYPAD_MULTIPLY,
    /**
     * The subtract key of the keypad.
     */
    KEYPAD_SUBTRACT,
    /**
     * The add key of the keypad.
     */
    KEYPAD_ADD,
    /**
     * Enter of the keypad.
     */
    KEYPAD_ENTER,
    /**
     * The equal sign key of the keypad.
     */
    KEYPAD_EQUAL,
    /**
     * The left Shift.
     */
    LEFT_SHIFT,
    /**
     * The left Control.
     */
    LEFT_CONTROL,
    /**
     * The left Alt.
     */
    LEFT_ALT,
    /**
     * The left Super (Windows, Command) key.
     */
    LEFT_SUPER,
    /**
     * The right Shift.
     */
    RIGHT_SHIFT,
    /**
     * The right Control.
     */
    RIGHT_CONTROL,
    /**
     * The right Alt.
     */
    RIGHT_ALT,
    /**
     * The right Super (Windows, Command) key.
     */
    RIGHT_SUPER,
    /**
     * The menu key.
     */
    MENU;

    /**
     * The number of digit keys of the main keyboard or of the keypad.
     */
    public static final int DIGIT_COUNT = 10;

    /**
     * Returns the letter key of a Latin letter.
     *
     * @param letter the letter, {@code a} to {@code z} in either case
     * @return the key
     * @throws IllegalArgumentException if the character is no Latin letter
     */
    public static @NotNull Key ofLetter(char letter) {
        char lowerCase = Character.toLowerCase(letter);

        if (lowerCase < 'a' || lowerCase > 'z') {
            throw Exceptions.newIllegalArgumentException("Not a letter key: {0}", letter);
        }

        return values()[A.ordinal() + lowerCase - 'a'];
    }

    /**
     * Returns the digit key of the main keyboard.
     *
     * @param digit the digit, {@code 0} to {@code 9}
     * @return the key
     * @throws IllegalArgumentException if the value is no digit
     */
    public static @NotNull Key ofDigit(int digit) {
        if (digit < 0 || digit >= DIGIT_COUNT) {
            throw Exceptions.newIllegalArgumentException("Not a digit key: {0}", digit);
        }

        return values()[DIGIT_0.ordinal() + digit];
    }

    /**
     * Returns the letter of a letter key.
     *
     * @return the index of the letter from {@code 0} ({@link #A}) to {@code 25} ({@link #Z}), or {@code -1} if the key is no letter key
     */
    public int getLetterIndex() {
        return this.ordinal() >= A.ordinal() && this.ordinal() <= Z.ordinal() ? this.ordinal() - A.ordinal() : -1;
    }

    /**
     * Returns the digit of a digit key of the main keyboard.
     *
     * @return the digit from {@code 0} to {@code 9}, or {@code -1} if the key is no digit key of the main keyboard
     */
    public int getDigit() {
        return this.ordinal() >= DIGIT_0.ordinal() && this.ordinal() <= DIGIT_9.ordinal() ? this.ordinal() - DIGIT_0.ordinal() : -1;
    }

    /**
     * Returns the digit of a digit key of the keypad.
     *
     * @return the digit from {@code 0} to {@code 9}, or {@code -1} if the key is no digit key of the keypad
     */
    public int getKeypadDigit() {
        return this.ordinal() >= KEYPAD_0.ordinal() && this.ordinal() <= KEYPAD_9.ordinal() ? this.ordinal() - KEYPAD_0.ordinal() : -1;
    }

    /**
     * Returns the number of a function key.
     *
     * @return the number from {@code 1} ({@link #F1}) to {@code 25} ({@link #F25}), or {@code -1} if the key is no function key
     */
    public int getFunctionNumber() {
        return this.ordinal() >= F1.ordinal() && this.ordinal() <= F25.ordinal() ? this.ordinal() - F1.ordinal() + 1 : -1;
    }
}
