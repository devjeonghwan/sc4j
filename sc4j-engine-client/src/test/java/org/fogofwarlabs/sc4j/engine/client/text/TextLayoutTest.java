package org.fogofwarlabs.sc4j.engine.client.text;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.resource.dialog.TextAlignment;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests line breaking at spaces, line feeds and inside long words, and the alignment of drawn lines.
 */
final class TextLayoutTest {
    private static final int LINE_HEIGHT = 10;
    private static final float WIDTH = 10;
    private static final float SCALE = 2;

    /**
     * Advances every character by one unit and records the drawn lines and their left edges.
     */
    private static final class UnitFont implements Font {
        private final @NotNull List<String> drawnLines;
        private final @NotNull List<Float> drawnLefts;

        private UnitFont() {
            this.drawnLines = new ArrayList<>();
            this.drawnLefts = new ArrayList<>();
        }

        @Override
        public int getLineHeight() {
            return LINE_HEIGHT;
        }

        @Override
        public @NotNull TextStyle getDefaultStyle() {
            return TextStyle.DEFAULT;
        }

        @Override
        public float measure(@NotNull CharSequence text, @NotNull TextStyle style) {
            return text.length() * style.getScale();
        }

        @Override
        public float draw(@NotNull Canvas canvas, @NotNull CharSequence text, float x, float y, int color, @NotNull TextStyle style) {
            this.drawnLines.add(text.toString());
            this.drawnLefts.add(x);

            return x + this.measure(text, style);
        }
    }

    @Test
    void wrap_should_break_at_spaces_that_fit_the_width() {
        TextLayout layout = new TextLayout(new UnitFont(), WIDTH);

        Assertions.assertEquals(List.of("one two", "three four", "five"), layout.wrap("one two three four five"));
    }

    @Test
    void wrap_should_end_lines_at_line_feeds() {
        TextLayout layout = new TextLayout(new UnitFont(), WIDTH);

        Assertions.assertEquals(List.of("one", "", "two"), layout.wrap("one\r\n\ntwo"));
        Assertions.assertEquals(List.of(""), layout.wrap(""));
    }

    @Test
    void wrap_should_break_words_wider_than_the_width_between_characters() {
        TextLayout layout = new TextLayout(new UnitFont(), WIDTH);

        Assertions.assertEquals(List.of("ab", "0123456789", "abcdef"), layout.wrap("ab 0123456789abcdef"));
    }

    @Test
    void draw_should_align_each_line_in_the_width() {
        UnitFont font = new UnitFont();
        TextLayout layout = new TextLayout(font, WIDTH);

        Canvas canvas = (Canvas) Proxy.newProxyInstance(Canvas.class.getClassLoader(), new Class<?>[]{Canvas.class},
                                                        (proxy, method, arguments) -> null);
        float height = layout.draw(canvas, "abcd ef", 0, 0, TextAlignment.RIGHT, 0);

        Assertions.assertEquals(LINE_HEIGHT, height);
        Assertions.assertEquals(List.of("abcd ef"), font.drawnLines);
        Assertions.assertEquals(List.of(3f), font.drawnLefts);
    }

    @Test
    void draw_should_measure_and_step_lines_in_the_style_scale() {
        UnitFont font = new UnitFont();
        TextStyle style = TextStyle.builder().setScale(SCALE).build();
        TextLayout layout = new TextLayout(font, style, WIDTH * SCALE);

        Canvas canvas = (Canvas) Proxy.newProxyInstance(Canvas.class.getClassLoader(), new Class<?>[]{Canvas.class},
                                                        (proxy, method, arguments) -> null);
        float height = layout.draw(canvas, "one two three", 0, 0, TextAlignment.RIGHT, 0);

        Assertions.assertEquals(List.of("one two", "three"), font.drawnLines);
        Assertions.assertEquals(List.of((WIDTH - 7) * SCALE, (WIDTH - 5) * SCALE), font.drawnLefts);
        Assertions.assertEquals(2 * LINE_HEIGHT * SCALE, height);
    }
}
