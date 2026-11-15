package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.host.HostWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Reads and writes the system clipboard of a client window ({@link HostWindow#getClipboardText}) on the window's thread; without a window
 * it holds nothing and keeps nothing.
 */
public final class WindowClipboard implements TextClipboard {
    private final @NotNull Supplier<@Nullable HostWindow> window;

    /**
     * Creates a {@link WindowClipboard} instance.
     *
     * @param window the supplier of the client window, which supplies {@code null} while the window does not exist
     */
    public WindowClipboard(@NotNull Supplier<@Nullable HostWindow> window) {
        this.window = window;
    }

    /**
     * Reads the clipboard text.
     *
     * @return the clipboard text, or {@code null} when it holds none
     */
    @Override
    public @Nullable String read() {
        HostWindow current = this.window.get();

        return current == null ? null : current.getClipboardText();
    }

    /**
     * Replaces the clipboard text.
     *
     * @param text the text
     */
    @Override
    public void write(@NotNull String text) {
        HostWindow current = this.window.get();

        if (current != null) {
            current.setClipboardText(text);
        }
    }
}
