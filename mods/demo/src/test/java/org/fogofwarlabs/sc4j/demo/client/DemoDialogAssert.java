package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.engine.client.app.SceneClient;
import org.fogofwarlabs.sc4j.engine.client.dialog.Dialog;
import org.fogofwarlabs.sc4j.engine.client.display.UiSurface;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

/**
 * Checks that a dialog of the demo shows whole on the client's last frame: inside the logical area, which is at least the engine minimum.
 */
final class DemoDialogAssert {
    /**
     * Asserts that a dialog lies inside the logical area of the client's last frame and that the area is at least the engine minimum.
     *
     * @param client the client
     * @param dialog the dialog, drawn in the last frame
     * @param name   the dialog's name for the failure message
     */
    static void assertShownWhole(@NotNull SceneClient client, @NotNull Dialog dialog, @NotNull String name) {
        UiSurface surface = client.getSurface();
        float right = dialog.getLeft() + dialog.getDefinition().getWidth();
        float bottom = dialog.getTop() + dialog.getDefinition().getHeight();

        Assertions.assertNotNull(surface, "the client drew a frame");
        Assertions.assertTrue(surface.getWidth() >= UiSurface.MINIMUM_WIDTH && surface.getHeight() >= UiSurface.MINIMUM_HEIGHT,
                              "the logical area " + surface + " is at least the engine minimum");
        Assertions.assertTrue(dialog.getLeft() >= 0 && dialog.getTop() >= 0 && right <= surface.getWidth() && bottom <= surface.getHeight(),
                              () -> name + " at " + dialog.getLeft() + "," + dialog.getTop() + " to " + right + "," + bottom + " lies inside " +
                                    surface);
    }

    private DemoDialogAssert() {
    }
}
