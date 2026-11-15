package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.resource.dialog.ControlKind;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogDefinition;
import org.fogofwarlabs.sc4j.engine.resource.dialog.TextAlignment;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests the routing of pointer and key input through the open dialogs.
 */
final class DialogStackTest {
    private static final int ROW_HEIGHT = 10;
    private static final int BUTTON = 1;
    private static final @NotNull DialogListener IGNORING_LISTENER = (dialog, control) -> {
    };

    private static @NotNull Dialog createDialog(int left, int top, boolean modal) {
        DialogControl button = new DialogControl(BUTTON, ControlKind.BUTTON, 0, 0, 20, 20, "button", TextAlignment.CENTER, null, Map.of(), List.of());

        return new Dialog(new DialogDefinition("dialog", left, top, 50, 50, List.of(button)),
                          new DefaultControlFactory(ROW_HEIGHT), IGNORING_LISTENER, modal);
    }

    @Test
    void pointer_input_reaches_the_top_dialog_with_a_control_or_the_first_modal_one() {
        DialogStack<Control, Dialog> stack = new DialogStack<>();
        Dialog screen = createDialog(0, 0, false);
        Dialog panel = createDialog(100, 0, false);

        stack.push(screen);
        stack.push(panel);

        DialogStack.Target<Control, Dialog> target = stack.findPointerTarget(105, 5);

        Assertions.assertNotNull(target);
        Assertions.assertSame(panel, target.getLayer());
        Assertions.assertSame(panel.getControls().getFirst(), target.getControl());
        Assertions.assertSame(screen, stack.findPointerTarget(5, 5).getLayer());
        Assertions.assertNull(stack.findPointerTarget(60, 60));

        Dialog popup = createDialog(200, 200, true);

        stack.push(popup);

        DialogStack.Target<Control, Dialog> blocked = stack.press(5, 5);

        Assertions.assertNotNull(blocked);
        Assertions.assertSame(popup, blocked.getLayer());
        Assertions.assertNull(blocked.getControl());
        Assertions.assertSame(popup, stack.findKeyTarget());
        Assertions.assertSame(popup, stack.getTop());

        stack.press(205, 205);

        DialogStack.Target<Control, Dialog> released = stack.release(205, 205);

        Assertions.assertNotNull(released);
        Assertions.assertSame(popup.getControls().getFirst(), released.getControl());

        stack.updateHover(205, 205);

        Assertions.assertSame(popup.getControls().getFirst(), popup.getHovered());
        Assertions.assertNull(screen.getHovered());

        stack.remove(popup);

        Assertions.assertSame(panel, stack.findKeyTarget());
    }
}
