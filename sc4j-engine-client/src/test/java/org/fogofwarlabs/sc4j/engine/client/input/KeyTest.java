package org.fogofwarlabs.sc4j.engine.client.input;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the letter, digit, keypad digit and function key numbering of the engine keys.
 */
final class KeyTest {
    private static final int LAST_LETTER_INDEX = 25;
    private static final int LAST_DIGIT = 9;
    private static final int LAST_FUNCTION_NUMBER = 25;

    @Test
    void of_letter_should_name_the_letter_keys_in_either_case_and_reject_other_characters() {
        Assertions.assertEquals(Key.A, Key.ofLetter('a'));
        Assertions.assertEquals(Key.Z, Key.ofLetter('Z'));
        Assertions.assertEquals(Key.M, Key.ofLetter('m'));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Key.ofLetter('1'));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Key.ofLetter('é'));
    }

    @Test
    void of_digit_should_name_the_main_keyboard_digits_and_reject_other_values() {
        Assertions.assertEquals(Key.DIGIT_0, Key.ofDigit(0));
        Assertions.assertEquals(Key.DIGIT_9, Key.ofDigit(LAST_DIGIT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Key.ofDigit(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Key.ofDigit(Key.DIGIT_COUNT));
    }

    @Test
    void key_numbering_reads_back_letters_digits_keypad_digits_and_function_numbers() {
        Assertions.assertEquals(0, Key.A.getLetterIndex());
        Assertions.assertEquals(LAST_LETTER_INDEX, Key.Z.getLetterIndex());
        Assertions.assertEquals(-1, Key.DIGIT_1.getLetterIndex());
        Assertions.assertEquals(LAST_DIGIT, Key.DIGIT_9.getDigit());
        Assertions.assertEquals(-1, Key.KEYPAD_9.getDigit());
        Assertions.assertEquals(LAST_DIGIT, Key.KEYPAD_9.getKeypadDigit());
        Assertions.assertEquals(-1, Key.DIGIT_9.getKeypadDigit());
        Assertions.assertEquals(1, Key.F1.getFunctionNumber());
        Assertions.assertEquals(LAST_FUNCTION_NUMBER, Key.F25.getFunctionNumber());
        Assertions.assertEquals(-1, Key.ESCAPE.getFunctionNumber());

        for (Key key : Key.values()) {
            int letter = key.getLetterIndex();
            int digit = key.getDigit();

            if (letter >= 0) {
                Assertions.assertEquals(key, Key.ofLetter((char) ('a' + letter)));
            }

            if (digit >= 0) {
                Assertions.assertEquals(key, Key.ofDigit(digit));
            }
        }
    }
}
