package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the composition an input method shows while it composes text ({@link TextCompositionListener#onCompositionChanged}): the text, its
 * target clause (the part the next conversion changes) and the cursor, all in UTF-16 characters from the start of the text.
 * <p>
 * The target clause is the first clause of unconverted input, else the clause holding the cursor; {@code clauseStart == clauseEnd} when the
 * input method reports no clauses.
 */
public final class TextComposition {
    private final @NotNull String text;

    private final int clauseStart;
    private final int clauseEnd;
    private final int cursor;

    /**
     * Creates a {@link TextComposition} instance.
     *
     * @param text        the composed text
     * @param clauseStart the first character of the target clause
     * @param clauseEnd   the character after the target clause
     * @param cursor      the cursor position
     * @throws IllegalArgumentException if the clause or the cursor lies outside the text or the clause ends before it starts
     */
    public TextComposition(@NotNull String text, int clauseStart, int clauseEnd, int cursor) {
        if (clauseStart < 0 || clauseEnd < clauseStart || clauseEnd > text.length() || cursor < 0 || cursor > text.length()) {
            throw Exceptions.newIllegalArgumentException("Composition clause {0}..{1} or cursor {2} outside text of {3} characters",
                                                         clauseStart, clauseEnd, cursor, text.length());
        }

        this.text = text;

        this.clauseStart = clauseStart;
        this.clauseEnd   = clauseEnd;
        this.cursor      = cursor;
    }

    /**
     * @return the composed text
     */
    public @NotNull String getText() {
        return this.text;
    }

    /**
     * @return the first character of the target clause
     */
    public int getClauseStart() {
        return this.clauseStart;
    }

    /**
     * @return the character after the target clause
     */
    public int getClauseEnd() {
        return this.clauseEnd;
    }

    /**
     * @return the cursor position
     */
    public int getCursor() {
        return this.cursor;
    }
}
