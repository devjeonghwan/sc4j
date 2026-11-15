package org.fogofwarlabs.sc4j.engine.client.text;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the shared cut of a line: a line that fits stays, a wider one ends in the cut mark within its width.
 */
final class TextCutUtilsTest {
    private static final int CHARACTER_WIDTH = 5;

    /**
     * Measures each character as one width and draws nothing.
     */
    private static final class FixedFont implements Font {
        @Override
        public int getLineHeight() {
            return CHARACTER_WIDTH;
        }

        @Override
        public @NotNull TextStyle getDefaultStyle() {
            return TextStyle.DEFAULT;
        }

        @Override
        public float measure(@NotNull CharSequence text, @NotNull TextStyle style) {
            return text.length() * CHARACTER_WIDTH;
        }

        @Override
        public float draw(@NotNull Canvas canvas, @NotNull CharSequence text, float x, float y, int color, @NotNull TextStyle style) {
            return x + this.measure(text, style);
        }
    }

    @Test
    void cut_should_keep_a_fitting_line_and_end_a_wider_one_in_the_cut_mark() {
        Font font = new FixedFont();
        StringBuilder fitting = new StringBuilder("Hearthstead");
        StringBuilder wide = new StringBuilder("Gathering Gold Mine");
        StringBuilder narrow = new StringBuilder("Gold");

        TextCutUtils.cut(font, fitting, fitting.length() * CHARACTER_WIDTH);
        TextCutUtils.cut(font, wide, 10 * CHARACTER_WIDTH);
        TextCutUtils.cut(font, narrow, CHARACTER_WIDTH);

        Assertions.assertEquals("Hearthstead", fitting.toString());
        Assertions.assertEquals("Gatheri" + TextCutUtils.CUT_MARK, wide.toString(), "the cut line fills its width");
        Assertions.assertEquals(TextCutUtils.CUT_MARK, narrow.toString(), "a room narrower than the mark keeps the mark alone");
    }
}
