package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the window clipboard without a window.
 */
final class WindowClipboardTest {
    @Test
    void window_clipboard_holds_nothing_without_a_window() {
        WindowClipboard clipboard = new WindowClipboard(() -> null);

        clipboard.write("kept nowhere");

        Assertions.assertNull(clipboard.read());
    }
}
