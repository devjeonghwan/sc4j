package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * Tests the engine text edit model: typing, editing keys, selection, clipboard operations, undo/redo, limits and code point steps.
 */
final class TextEditModelTest {
    private static final int TYPED_MAXIMUM_LENGTH = 78;
    private static final int PASTE_MAXIMUM_LENGTH = 8;
    /**
     * A maximum length above the acceptance test's limit.
     */
    private static final int ACCEPTANCE_MAXIMUM_LENGTH = 120;
    /**
     * The acceptance test's byte limit: two Hangul syllables of three bytes and one more byte.
     */
    private static final int ACCEPTED_BYTES = 7;
    /**
     * A character outside the Basic Multilingual Plane: two UTF-16 units, one code point.
     */
    private static final @NotNull String SUPPLEMENTARY = "\uD83D\uDE00";
    private static final int SURROGATE_LIMIT = 3;

    /**
     * A clipboard in memory.
     */
    private static final class MemoryClipboard implements TextClipboard {
        private @Nullable String text;

        private MemoryClipboard(@Nullable String text) {
            this.text = text;
        }

        /**
         * Reads the clipboard text.
         *
         * @return the clipboard text, or {@code null} when it holds none
         */
        @Override
        public @Nullable String read() {
            return this.text;
        }

        /**
         * Replaces the clipboard text.
         *
         * @param text the text
         */
        @Override
        public void write(@NotNull String text) {
            this.text = text;
        }
    }

    /**
     * Types the supplied characters into an empty model.
     *
     * @param text the characters to type
     * @return the populated model
     */
    private static @NotNull TextEditModel createTypedModel(@NotNull String text) {
        TextEditModel edit = new TextEditModel("", TYPED_MAXIMUM_LENGTH, "");

        text.codePoints().forEach(edit::type);

        return edit;
    }

    @Test
    void get_maximum_length_should_return_the_constructor_limit() {
        Assertions.assertEquals(PASTE_MAXIMUM_LENGTH, new TextEditModel("abc", PASTE_MAXIMUM_LENGTH, "").getMaximumLength());
        Assertions.assertEquals(TextEditModel.UNLIMITED, new TextEditModel("", TextEditModel.UNLIMITED, "").getMaximumLength());
    }

    @Test
    void select_all_copies_and_cuts_whole_text() {
        TextEditModel edit = createTypedModel("gg wp");
        MemoryClipboard clipboard = new MemoryClipboard(null);

        Assertions.assertFalse(edit.apply(TextEditOperation.COPY, clipboard), "Nothing is selected yet.");
        Assertions.assertNull(clipboard.read());

        edit.apply(TextEditOperation.SELECT_ALL, clipboard);

        Assertions.assertEquals(0, edit.getSelectionStart());
        Assertions.assertEquals(5, edit.getSelectionEnd());

        edit.apply(TextEditOperation.COPY, clipboard);

        Assertions.assertEquals("gg wp", clipboard.read());
        Assertions.assertTrue(edit.apply(TextEditOperation.CUT, clipboard));
        Assertions.assertEquals("", edit.getText());
        Assertions.assertEquals(0, edit.getCaret());
    }

    @Test
    void paste_replaces_selection_and_keeps_typing_rules() {
        TextEditModel edit = new TextEditModel("abc", PASTE_MAXIMUM_LENGTH, "*");

        edit.apply(TextEditOperation.SELECT_ALL, new MemoryClipboard(null));

        // Line breaks and the rejected set are skipped; the paste stops at the maximum length.
        Assertions.assertTrue(edit.apply(TextEditOperation.PASTE, new MemoryClipboard("x\r\ny*z0123456789")));
        Assertions.assertEquals("xyz01234", edit.getText());
        Assertions.assertEquals(8, edit.getCaret());
    }

    @Test
    void typing_and_deleting_replace_selection() {
        TextEditModel edit = createTypedModel("hello");

        edit.apply(TextEditOperation.SELECT_ALL, new MemoryClipboard(null));
        edit.type('j');

        Assertions.assertEquals("j", edit.getText());

        edit = createTypedModel("hello");
        edit.apply(TextEditOperation.SELECT_ALL, new MemoryClipboard(null));

        Assertions.assertTrue(edit.press(TextEditKey.BACKSPACE));
        Assertions.assertEquals("", edit.getText());
    }

    @Test
    void undo_and_redo_step_over_typed_runs() {
        TextEditModel edit = createTypedModel("abc");

        edit.press(TextEditKey.BACKSPACE);

        Assertions.assertEquals("ab", edit.getText());

        MemoryClipboard clipboard = new MemoryClipboard(null);

        Assertions.assertTrue(edit.apply(TextEditOperation.UNDO, clipboard));
        Assertions.assertEquals("abc", edit.getText());

        // The typed run "abc" is one step.
        Assertions.assertTrue(edit.apply(TextEditOperation.UNDO, clipboard));
        Assertions.assertEquals("", edit.getText());
        Assertions.assertFalse(edit.apply(TextEditOperation.UNDO, clipboard));
        Assertions.assertTrue(edit.apply(TextEditOperation.REDO, clipboard));
        Assertions.assertEquals("abc", edit.getText());
        Assertions.assertEquals(3, edit.getCaret());

        edit.type('d');

        Assertions.assertFalse(edit.apply(TextEditOperation.REDO, clipboard), "A new change drops the redo steps.");
    }

    @Test
    void acceptance_test_limits_typing_and_pasting() {
        // An owner's byte limit, for example of a chat line: a Hangul syllable takes three bytes.
        TextEditModel edit = new TextEditModel("", ACCEPTANCE_MAXIMUM_LENGTH, "");

        edit.setAcceptance(next -> next.getBytes(StandardCharsets.UTF_8).length <= ACCEPTED_BYTES);
        edit.apply(TextEditOperation.PASTE, new MemoryClipboard("안녕하세요"));

        Assertions.assertEquals("안녕", edit.getText());
        Assertions.assertTrue(edit.type('a'));
        Assertions.assertFalse(edit.type('b'));
        Assertions.assertEquals("안녕a", edit.getText());
    }

    @Test
    void editing_keys_step_over_whole_code_points() {
        TextEditModel edit = createTypedModel("a" + SUPPLEMENTARY + "b");

        Assertions.assertFalse(edit.press(TextEditKey.LEFT));
        Assertions.assertEquals(3, edit.getCaret());

        edit.press(TextEditKey.LEFT);

        Assertions.assertEquals(1, edit.getCaret());

        edit.press(TextEditKey.RIGHT);

        Assertions.assertEquals(3, edit.getCaret());
        Assertions.assertTrue(edit.press(TextEditKey.BACKSPACE));
        Assertions.assertEquals("ab", edit.getText());
        Assertions.assertEquals(1, edit.getCaret());

        edit.setText(SUPPLEMENTARY + "c");
        edit.press(TextEditKey.HOME);

        Assertions.assertTrue(edit.press(TextEditKey.DELETE));
        Assertions.assertEquals("c", edit.getText());
    }

    @Test
    void length_limit_counts_code_points() {
        TextEditModel edit = new TextEditModel("", SURROGATE_LIMIT, "");

        Assertions.assertTrue(edit.type(SUPPLEMENTARY.codePointAt(0)));
        Assertions.assertTrue(edit.type(SUPPLEMENTARY.codePointAt(0)));
        Assertions.assertTrue(edit.type('x'));
        Assertions.assertFalse(edit.type('y'));
        Assertions.assertEquals(SUPPLEMENTARY + SUPPLEMENTARY + "x", edit.getText());

        edit.setText("");

        Assertions.assertTrue(edit.apply(TextEditOperation.PASTE, new MemoryClipboard(SUPPLEMENTARY + SUPPLEMENTARY + SUPPLEMENTARY + "z")));
        Assertions.assertEquals(SUPPLEMENTARY + SUPPLEMENTARY + SUPPLEMENTARY, edit.getText());
    }

    @Test
    void set_text_should_move_the_caret_to_the_end_and_forget_the_history() {
        TextEditModel edit = createTypedModel("abc");

        edit.setText("xy");

        Assertions.assertEquals(2, edit.getCaret());
        Assertions.assertFalse(edit.apply(TextEditOperation.UNDO, new MemoryClipboard(null)));
    }
}
