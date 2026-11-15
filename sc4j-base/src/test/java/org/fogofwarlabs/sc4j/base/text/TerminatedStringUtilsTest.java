package org.fogofwarlabs.sc4j.base.text;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Checks the terminator search, the cut, the put into a buffer, the copies without control bytes and the text of
 * zero-terminated byte strings in fixed-size buffers.
 */
final class TerminatedStringUtilsTest {
    /**
     * The size of a saved game name buffer, terminator included.
     */
    private static final int SAVED_GAME_SIZE = 0x1C;
    /**
     * The size of a character name buffer, terminator included.
     */
    private static final int CHARACTER_NAME_SIZE = 0x60;
    /**
     * The size of a player record's name buffer, terminator included.
     */
    private static final int RECORD_NAME_SIZE = 0x19;
    /**
     * A character of three UTF-8 bytes.
     */
    private static final @NotNull String HANGUL = "\ud55c";
    /**
     * The UTF-8 byte count of {@link #HANGUL}.
     */
    private static final int HANGUL_BYTES = 3;
    /**
     * The first two UTF-8 bytes of {@link #HANGUL}, which a cut inside it keeps.
     */
    private static final byte @NotNull [] HANGUL_HEAD = {(byte) 0xED, (byte) 0x95};
    /**
     * The character a {@link String} read from the bytes holds for a split character.
     */
    private static final char REPLACEMENT = '\ufffd';
    /**
     * A byte the tests fill buffers with, which a put leaves outside the text and its terminator.
     */
    private static final byte FILL = 0x7E;
    /**
     * A short size: a buffer of three bytes, or a limit of three bytes.
     */
    private static final int SHORT_SIZE = 3;

    private static byte @NotNull [] concatenate(byte @NotNull [] first, byte @NotNull [] second) {
        byte[] bytes = Arrays.copyOf(first, first.length + second.length);

        System.arraycopy(second, 0, bytes, first.length, second.length);

        return bytes;
    }

    private static byte @NotNull [] encode(@NotNull String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void get_terminated_length_should_count_the_terminator_within_the_limit() {
        byte[] text = {'x', 'a', 'b', 0, 'c'};

        Assertions.assertEquals(3, TerminatedStringUtils.getTerminatedLength(text, 1, 3));
        Assertions.assertEquals(0, TerminatedStringUtils.getTerminatedLength(text, 1, 2));
        Assertions.assertEquals(0, TerminatedStringUtils.getTerminatedLength(text, 4, 8));
        Assertions.assertEquals(0, TerminatedStringUtils.getTerminatedLength(new byte[]{'a', 0x1F, 0}, 0, 3));
        Assertions.assertEquals(1, TerminatedStringUtils.getTerminatedLength(new byte[]{0}, 0, 1));
    }

    @Test
    void get_text_length_should_stop_at_the_first_terminator_or_the_buffer_end() {
        Assertions.assertEquals(2, TerminatedStringUtils.getTextLength(new byte[]{'a', 'b', 0, 'c', 0}));
        Assertions.assertEquals(3, TerminatedStringUtils.getTextLength(encode("abc")));
        Assertions.assertEquals(0, TerminatedStringUtils.getTextLength(new byte[CHARACTER_NAME_SIZE]));
    }

    @Test
    void cut_text_should_keep_the_first_bytes_of_a_character_split_at_the_buffer_end() {
        byte[] mixed = TerminatedStringUtils.cutText("a" + HANGUL.repeat(SAVED_GAME_SIZE / HANGUL_BYTES), SAVED_GAME_SIZE);
        byte[] single = TerminatedStringUtils.cutText("x".repeat(SAVED_GAME_SIZE - 2) + HANGUL, SAVED_GAME_SIZE);
        byte[] characterName = TerminatedStringUtils.cutText(HANGUL.repeat(CHARACTER_NAME_SIZE / HANGUL_BYTES), CHARACTER_NAME_SIZE);

        Assertions.assertArrayEquals(encode("x".repeat(SAVED_GAME_SIZE - 1)),
                                     TerminatedStringUtils.cutText("x".repeat(SAVED_GAME_SIZE), SAVED_GAME_SIZE));
        Assertions.assertArrayEquals(encode("abc"), TerminatedStringUtils.cutText("abc", SAVED_GAME_SIZE));
        Assertions.assertArrayEquals(concatenate(encode("a" + HANGUL.repeat(SAVED_GAME_SIZE / HANGUL_BYTES - 1)), HANGUL_HEAD), mixed);
        Assertions.assertArrayEquals(concatenate(encode("x".repeat(SAVED_GAME_SIZE - 2)), new byte[]{HANGUL_HEAD[0]}), single);
        Assertions.assertArrayEquals(concatenate(encode(HANGUL.repeat(CHARACTER_NAME_SIZE / HANGUL_BYTES - 1)), HANGUL_HEAD), characterName);
        // A second cut keeps the split bytes.
        Assertions.assertArrayEquals(mixed, TerminatedStringUtils.cutText(mixed, SAVED_GAME_SIZE));
        Assertions.assertArrayEquals(characterName, TerminatedStringUtils.cutText(characterName, CHARACTER_NAME_SIZE));
    }

    @Test
    void put_text_should_put_the_cut_bytes_and_the_terminator_into_the_buffer() {
        byte[] text = encode("a" + HANGUL.repeat(SAVED_GAME_SIZE / HANGUL_BYTES));
        byte[] buffer = new byte[SAVED_GAME_SIZE + 2];
        byte[] expected = new byte[buffer.length];

        Arrays.fill(buffer, FILL);
        Arrays.fill(expected, FILL);
        System.arraycopy(TerminatedStringUtils.cutText(text, SAVED_GAME_SIZE), 0, expected, 1, SAVED_GAME_SIZE - 1);
        expected[SAVED_GAME_SIZE] = 0;

        TerminatedStringUtils.putText(text, 0, text.length, buffer, 1, SAVED_GAME_SIZE);

        Assertions.assertArrayEquals(expected, buffer);

        Arrays.fill(buffer, FILL);
        Arrays.fill(expected, FILL);
        expected[0] = 'b';
        expected[1] = 'c';
        expected[2] = 0;

        TerminatedStringUtils.putText(encode("abcd"), 1, 2, buffer, 0, SAVED_GAME_SIZE);

        Assertions.assertArrayEquals(expected, buffer);
    }

    @Test
    void put_whole_characters_should_end_the_text_at_the_lead_byte_of_a_split_last_character() {
        byte[] buffer = new byte[RECORD_NAME_SIZE + 1];
        byte[] expected = new byte[buffer.length];
        byte[] text = encode("x".repeat(RECORD_NAME_SIZE - HANGUL_BYTES) + HANGUL);

        Arrays.fill(buffer, FILL);
        Arrays.fill(expected, FILL);
        System.arraycopy(text, 0, expected, 0, RECORD_NAME_SIZE - 1);
        // The lead byte of the split character is the terminator, its continuation byte stays after it.
        expected[RECORD_NAME_SIZE - HANGUL_BYTES] = 0;
        expected[RECORD_NAME_SIZE - 1]            = 0;

        TerminatedStringUtils.putWholeCharacters(text, 0, text.length, buffer, 0, RECORD_NAME_SIZE);

        Assertions.assertArrayEquals(expected, buffer);
    }

    @Test
    void cut_whole_characters_should_keep_whole_characters_and_cut_bytes_otherwise() {
        byte[] whole = encode(HANGUL.repeat((RECORD_NAME_SIZE - 1) / HANGUL_BYTES));
        byte[] longer = encode(HANGUL.repeat(RECORD_NAME_SIZE));
        byte[] invalid = {'a', (byte) 0xC0, (byte) 0xC0, (byte) 0xC0};

        Assertions.assertArrayEquals(whole, TerminatedStringUtils.cutWholeCharacters(whole, RECORD_NAME_SIZE));
        Assertions.assertArrayEquals(whole, TerminatedStringUtils.cutWholeCharacters(longer, RECORD_NAME_SIZE));
        Assertions.assertArrayEquals(encode("x".repeat(RECORD_NAME_SIZE - 2)),
                                     TerminatedStringUtils.cutWholeCharacters(encode("x".repeat(RECORD_NAME_SIZE - 2) + HANGUL), RECORD_NAME_SIZE));
        Assertions.assertArrayEquals(encode("abc"), TerminatedStringUtils.cutWholeCharacters(encode("abc"), RECORD_NAME_SIZE));
        // A text ending in bytes that start no character is cut by its bytes only.
        Assertions.assertArrayEquals(Arrays.copyOf(invalid, SHORT_SIZE - 1), TerminatedStringUtils.cutWholeCharacters(invalid, SHORT_SIZE));
        Assertions.assertArrayEquals(new byte[0], TerminatedStringUtils.cutWholeCharacters(invalid, 1));
    }

    @Test
    void get_text_length_should_count_from_an_offset_to_the_first_terminator_after_it() {
        byte[] records = {'a', 'b', 'c', 'd', 'e', 0, 'f'};

        Assertions.assertEquals(3, TerminatedStringUtils.getTextLength(records, 2));
        Assertions.assertEquals(0, TerminatedStringUtils.getTextLength(records, 5));
        Assertions.assertEquals(1, TerminatedStringUtils.getTextLength(records, 6));
    }

    @Test
    void cut_text_without_control_bytes_should_keep_tab_line_feed_and_carriage_return_only() {
        byte[] name = {'a', 0x01, '\t', 0x03, '\n', 0x1B, '\r', 0x1F, 'b', (byte) 0xED, 0, 'c'};

        Assertions.assertArrayEquals(new byte[]{'a', '\t', '\n', '\r', 'b', (byte) 0xED},
                                     TerminatedStringUtils.cutTextWithoutControlBytes(name, CHARACTER_NAME_SIZE));
        // A removed byte does not count toward the size; the size keeps one byte for the terminator.
        Assertions.assertArrayEquals(new byte[]{'a', '\t'}, TerminatedStringUtils.cutTextWithoutControlBytes(name, SHORT_SIZE));
        Assertions.assertArrayEquals(new byte[0], TerminatedStringUtils.cutTextWithoutControlBytes(name, 1));
        Assertions.assertArrayEquals(new byte[0], TerminatedStringUtils.cutTextWithoutControlBytes(name, 0));
    }

    @Test
    void copy_printable_text_should_skip_every_byte_below_space_and_keep_the_limit() {
        byte[] name = {0x02, 'a', '\t', 'b', '\n', 0x1F, 'c', (byte) 0xED, 0, 'd'};

        Assertions.assertArrayEquals(new byte[]{'a', 'b', 'c', (byte) 0xED}, TerminatedStringUtils.copyPrintableText(name, CHARACTER_NAME_SIZE));
        // The limit counts the kept bytes and leaves none for a terminator.
        Assertions.assertArrayEquals(new byte[]{'a', 'b', 'c'}, TerminatedStringUtils.copyPrintableText(name, SHORT_SIZE));
        Assertions.assertArrayEquals(new byte[0], TerminatedStringUtils.copyPrintableText(name, 0));
    }

    @Test
    void read_text_should_read_the_bytes_before_the_terminator() {
        byte[] characterName = TerminatedStringUtils.cutText(HANGUL.repeat(CHARACTER_NAME_SIZE / HANGUL_BYTES), CHARACTER_NAME_SIZE);

        Assertions.assertEquals("ab", TerminatedStringUtils.readText(new byte[]{'a', 'b', 0, 'c'}));
        Assertions.assertEquals(HANGUL.repeat(2), TerminatedStringUtils.readText(encode(HANGUL.repeat(2))));
        Assertions.assertEquals(HANGUL.repeat(CHARACTER_NAME_SIZE / HANGUL_BYTES - 1) + REPLACEMENT, TerminatedStringUtils.readText(characterName));
    }
}
