package org.fogofwarlabs.sc4j.engine.client.text;

import org.jetbrains.annotations.NotNull;

/**
 * Cuts a line of text that is wider than its room to the part that fits before {@value #CUT_MARK}: the shared cut of the engine's tables, lists
 * and HUD texts.
 */
public final class TextCutUtils {
    /**
     * The end of a text that was cut at its room's edge.
     */
    public static final @NotNull String CUT_MARK = "...";

    /**
     * Cuts a line in place to the part that fits a width in the font's default style followed by {@value #CUT_MARK}; a line that fits stays as
     * it is, and a room narrower than the mark leaves the mark alone.
     *
     * @param font  the font the line is drawn in
     * @param line  the line, without line breaks, cut in place
     * @param width the width the line must fit, in canvas units
     */
    public static void cut(@NotNull Font font, @NotNull StringBuilder line, float width) {
        if (font.measure(line) <= width) {
            return;
        }

        float markWidth = font.measure(CUT_MARK);

        while (!line.isEmpty() && font.measure(line) + markWidth > width) {
            line.setLength(line.length() - 1);
        }

        line.append(CUT_MARK);
    }

    private TextCutUtils() {
    }
}
