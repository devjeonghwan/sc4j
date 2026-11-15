package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.scene.SceneStack;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.text.TextStyle;
import org.fogofwarlabs.sc4j.engine.resource.dialog.ControlKind;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogDefinition;
import org.fogofwarlabs.sc4j.engine.resource.dialog.TextAlignment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Tests dialogs shown as modal scenes: they join the open dialogs while the scene stack holds them.
 */
final class DialogSceneTest {
    private static final int ROW_HEIGHT = 10;
    private static final int BUTTON = 1;
    private static final @NotNull DialogListener IGNORING_LISTENER = (dialog, control) -> {
    };

    /**
     * Measures nothing and draws nothing.
     */
    private static final class EmptyFont implements Font {
        @Override
        public int getLineHeight() {
            return ROW_HEIGHT;
        }

        @Override
        public @NotNull TextStyle getDefaultStyle() {
            return TextStyle.DEFAULT;
        }

        @Override
        public float measure(@NotNull CharSequence text, @NotNull TextStyle style) {
            return 0;
        }

        @Override
        public float draw(@NotNull Canvas canvas, @NotNull CharSequence text, float x, float y, int color, @NotNull TextStyle style) {
            return x;
        }
    }

    /**
     * Ignores scene switches.
     */
    private static final class IgnoringSceneListener implements SceneStack.Listener<DialogScene<Object>> {
        @Override
        public void onSwitched(@Nullable DialogScene<Object> previous, @NotNull DialogScene<Object> next) {
        }

        @Override
        public void onOpenFailed(@NotNull DialogScene<Object> scene, @NotNull Exception exception) {
        }
    }

    private static @NotNull Dialog createDialog() {
        DialogControl button = new DialogControl(BUTTON, ControlKind.BUTTON, 0, 0, 20, 20, "button", TextAlignment.CENTER, null, Map.of(), List.of());

        return new Dialog(new DialogDefinition("dialog", 0, 0, 50, 50, List.of(button)),
                          new DefaultControlFactory(ROW_HEIGHT), IGNORING_LISTENER, true);
    }

    @Test
    void dialog_scenes_join_the_open_dialogs_while_open_as_modal_scenes() throws IOException {
        DialogStack<Control, Dialog> dialogs = new DialogStack<>();
        Dialog popup = createDialog();
        DialogScene<Object> scene = new DialogScene<>(popup, DialogTheme.createDefault(new EmptyFont()), dialogs);
        SceneStack<Object, Canvas, DialogScene<Object>> scenes = new SceneStack<>(new Object(), new IgnoringSceneListener());

        scenes.pushModal(scene);

        Assertions.assertSame(popup, dialogs.getModal());
        Assertions.assertSame(scene, scenes.getTopScene());

        scenes.popModal();

        Assertions.assertTrue(dialogs.getLayers().isEmpty());
    }
}
