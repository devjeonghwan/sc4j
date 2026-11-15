package org.fogofwarlabs.sc4j.base.text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * Checks that names fold only their ASCII letters.
 */
final class AsciiCaseUtilsTest {
    @Test
    void to_lower_case_should_fold_ascii_upper_case_letters_only() {
        Assertions.assertEquals("game [1]@z_", AsciiCaseUtils.toLowerCase("GaME [1]@Z_"));
        Assertions.assertEquals("\u00c4\u212a\u0130", AsciiCaseUtils.toLowerCase("\u00c4\u212a\u0130"));
        Assertions.assertEquals('a', AsciiCaseUtils.toLowerCase('A'));
        Assertions.assertEquals('@', AsciiCaseUtils.toLowerCase('@'));
        Assertions.assertEquals('[', AsciiCaseUtils.toLowerCase('['));
        Assertions.assertEquals(0xC4, AsciiCaseUtils.toLowerCase(0xC4));
    }

    @Test
    void to_lower_case_should_return_a_text_without_upper_case_letters_unchanged() {
        String text = "pnm:a._example._udp.local";

        Assertions.assertSame(text, AsciiCaseUtils.toLowerCase(text));
    }

    @Test
    void equals_ignore_case_should_match_ascii_letters_across_case_only() {
        Assertions.assertTrue(AsciiCaseUtils.equalsIgnoreCase("Rush Hour", "rUSH hOUR"));
        Assertions.assertFalse(AsciiCaseUtils.equalsIgnoreCase("\u00c4", "\u00e4"));
        Assertions.assertFalse(AsciiCaseUtils.equalsIgnoreCase("\u212a", "k"));
        Assertions.assertFalse(AsciiCaseUtils.equalsIgnoreCase("[", "{"));
        Assertions.assertFalse(AsciiCaseUtils.equalsIgnoreCase("game", "games"));
    }

    @Test
    void equals_ignore_case_should_compare_bytes_folding_ascii_letters_only() {
        byte[] split = {'G', 'a', (byte) 0xED, (byte) 0x95};
        byte[] upper = "Rush Hour".getBytes(StandardCharsets.UTF_8);
        byte[] lower = "rUSH hOUR".getBytes(StandardCharsets.UTF_8);

        Assertions.assertTrue(AsciiCaseUtils.equalsIgnoreCase(split, new byte[]{'g', 'A', (byte) 0xED, (byte) 0x95}));
        Assertions.assertTrue(AsciiCaseUtils.equalsIgnoreCase(upper, lower));
        Assertions.assertFalse(AsciiCaseUtils.equalsIgnoreCase(split, "Ga\ufffd".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertFalse(AsciiCaseUtils.equalsIgnoreCase("\u00c4".getBytes(StandardCharsets.UTF_8), "\u00e4".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertFalse(AsciiCaseUtils.equalsIgnoreCase(new byte[]{'['}, new byte[]{'{'}));
    }
}
