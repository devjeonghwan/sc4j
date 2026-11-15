package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exchanges the text of {@link TextEditOperation#CUT}, {@link TextEditOperation#COPY} and {@link TextEditOperation#PASTE}; a client uses
 * the system clipboard of its window ({@link WindowClipboard}).
 */
public interface TextClipboard {
    /**
     * Reads the clipboard text.
     *
     * @return the clipboard text, or {@code null} when it holds none
     */
    @Nullable String read();

    /**
     * Replaces the clipboard text.
     *
     * @param text the text
     */
    void write(@NotNull String text);
}
