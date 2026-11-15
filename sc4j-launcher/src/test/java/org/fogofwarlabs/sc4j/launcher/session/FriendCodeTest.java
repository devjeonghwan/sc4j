package org.fogofwarlabs.sc4j.launcher.session;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Locale;
import java.util.SplittableRandom;

/**
 * Tests the friend code: the 32-byte player key in Base32 without padding (52 characters, shown in groups of four), read back from the text
 * a user pastes (any case, with spaces or hyphens), and refused when it is not a key.
 */
final class FriendCodeTest {
    private static final int KEY_LENGTH = 32;
    private static final int ROUNDS = 200;
    private static final long RANDOM_SEED = 20261113L;

    @Test
    void of_should_write_the_key_in_base32_without_padding() {
        byte[] zeros = new byte[KEY_LENGTH];
        byte[] ones = new byte[KEY_LENGTH];

        Arrays.fill(ones, (byte) 0xFF);

        Assertions.assertEquals("A".repeat(FriendCode.LENGTH), FriendCode.of(zeros).getText());
        // 256 bits of ones are 51 full characters and 1 bit padded with four zero bits: the last character is 10000 = Q.
        Assertions.assertEquals("7".repeat(FriendCode.LENGTH - 1) + "Q", FriendCode.of(ones).getText());
        Assertions.assertEquals(FriendCode.LENGTH, FriendCode.of(zeros).getText().length());
    }

    @Test
    void parse_should_read_back_every_written_code_in_any_case_and_grouping() {
        SplittableRandom random = new SplittableRandom(RANDOM_SEED);

        for (int round = 0; round < ROUNDS; round++) {
            byte[] key = new byte[KEY_LENGTH];

            random.nextBytes(key);

            FriendCode code = FriendCode.of(key);

            Assertions.assertArrayEquals(key, FriendCode.parse(code.getText()).getKey());
            Assertions.assertEquals(code, FriendCode.parse(code.getGroupedText()));
            Assertions.assertEquals(code, FriendCode.parse(" " + code.getText().toLowerCase(Locale.ROOT) + "\n"));
            Assertions.assertEquals(code, FriendCode.parse(code.getGroupedText().replace(' ', '-')));
        }
    }

    @Test
    void get_grouped_text_should_show_groups_of_four() {
        FriendCode code = FriendCode.of(new byte[KEY_LENGTH]);
        String[] groups = code.getGroupedText().split(" ");

        Assertions.assertEquals(FriendCode.LENGTH / FriendCode.GROUP_LENGTH, groups.length);

        for (String group : groups) {
            Assertions.assertEquals("AAAA", group);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ABC", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB",
                            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "Raynor#60bf07"})
    void parse_should_refuse_a_text_that_is_no_key(@NotNull String text) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> FriendCode.parse(text));
    }
}
