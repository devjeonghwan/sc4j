package org.fogofwarlabs.sc4j.engine.client.text;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.resource.dialog.TextAlignment;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Breaks text into lines of a {@link Font} that fit a width and draws them aligned in it.
 * <p>
 * Line feeds end lines. Within a paragraph lines break at spaces: a line takes words while they fit, and the spaces at a break
 * are dropped. A word wider than the whole width breaks between characters. Colour codes take part in the text unchanged, so a
 * {@link Font} that measures them as nothing keeps them across breaks. The layout measures and draws in one {@link TextStyle}, so
 * its width and the line steps are in canvas units at the style's scale.
 */
public final class TextLayout {
    private static final char LINE_FEED = '\n';
    private static final char CARRIAGE_RETURN = '\r';
    private static final char SPACE = ' ';
    private static final float HALF = 2;

    private final @NotNull Font font;
    private final @NotNull TextStyle style;
    private final float width;

    /**
     * Creates a {@link TextLayout} instance in the font's {@linkplain Font#getDefaultStyle() default style}.
     *
     * @param font  the font of the text
     * @param width the width the lines must fit, in canvas units
     */
    public TextLayout(@NotNull Font font, float width) {
        this(font, font.getDefaultStyle(), width);
    }

    /**
     * Creates a {@link TextLayout} instance.
     *
     * @param font  the font of the text
     * @param style the style the lines are measured and drawn in
     * @param width the width the lines must fit, in canvas units
     */
    public TextLayout(@NotNull Font font, @NotNull TextStyle style, float width) {
        this.font  = font;
        this.style = style;
        this.width = width;
    }

    /**
     * @return the font of the text
     */
    public @NotNull Font getFont() {
        return this.font;
    }

    /**
     * @return the style the lines are measured and drawn in
     */
    public @NotNull TextStyle getStyle() {
        return this.style;
    }

    /**
     * @return the width the lines must fit, in canvas units
     */
    public float getWidth() {
        return this.width;
    }


    /**
     * Breaks a text into lines.
     *
     * @param text the text, with line feeds ({@code \n} or {@code \r\n}) between paragraphs
     * @return the lines in order, at least one
     */
    public @NotNull List<@NotNull String> wrap(@NotNull String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;

        while (true) {
            int end = text.indexOf(LINE_FEED, start);
            int paragraphEnd = end < 0 ? text.length() : end;

            if (paragraphEnd > start && text.charAt(paragraphEnd - 1) == CARRIAGE_RETURN) {
                paragraphEnd--;
            }

            this.wrapParagraph(text.substring(start, paragraphEnd), lines);

            if (end < 0) {
                return lines;
            }

            start = end + 1;
        }
    }

    private void wrapParagraph(@NotNull String paragraph, @NotNull List<String> lines) {
        StringBuilder line = new StringBuilder();
        int index = 0;

        while (index < paragraph.length()) {
            int wordEnd = paragraph.indexOf(SPACE, index);

            if (wordEnd < 0) {
                wordEnd = paragraph.length();
            }

            String word = paragraph.substring(index, wordEnd);
            String joined = line.isEmpty() ? word : line + String.valueOf(SPACE) + word;

            if (this.font.measure(joined, this.style) <= this.width) {
                line.setLength(0);
                line.append(joined);
            } else {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }

                this.breakWord(word, line, lines);
            }

            index = wordEnd + 1;
        }

        lines.add(line.toString());
    }

    /**
     * Appends a word to an empty line, breaking it between characters while it is wider than the width.
     *
     * @param word  the word
     * @param line  the empty current line, left holding the last part of the word
     * @param lines the finished lines
     */
    private void breakWord(@NotNull String word, @NotNull StringBuilder line, @NotNull List<String> lines) {
        for (int index = 0; index < word.length(); ) {
            int codePoint = word.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            String part = line + word.substring(index, next);

            if (!line.isEmpty() && this.font.measure(part, this.style) > this.width) {
                lines.add(line.toString());
                line.setLength(0);
            }

            line.append(word, index, next);
            index = next;
        }
    }

    /**
     * Breaks a text into lines and draws them from a top edge down, each aligned in the width.
     *
     * @param canvas    the canvas that draws, between its {@link Canvas#begin} and {@link Canvas#end}
     * @param text      the text
     * @param x         the left edge of the width
     * @param y         the top of the first line
     * @param alignment the horizontal alignment of each line
     * @param color     the packed {@code 0xRRGGBBAA} colour each line starts in
     * @return the height of the drawn lines
     */
    public float draw(@NotNull Canvas canvas, @NotNull String text, float x, float y, @NotNull TextAlignment alignment, int color) {
        List<String> lines = this.wrap(text);
        float top = y;

        for (String line : lines) {
            float left = switch (alignment) {
                case LEFT -> x;
                case CENTER -> x + (this.width - this.font.measure(line, this.style)) / HALF;
                case RIGHT -> x + this.width - this.font.measure(line, this.style);
            };

            this.font.draw(canvas, line, left, top, color, this.style);
            top += this.font.getLineHeight() * this.style.getScale();
        }

        return top - y;
    }
}
