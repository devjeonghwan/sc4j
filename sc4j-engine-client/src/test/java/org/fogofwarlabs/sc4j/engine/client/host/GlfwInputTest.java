package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.engine.client.input.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the GLFW input adapter on a hidden window: the installed callbacks turn GLFW calls into engine input events with framebuffer positions.
 */
final class GlfwInputTest {
    private static final @NotNull String RENDER_SMOKE_PROPERTY = "sc4j.render.smoke";
    private static final @NotNull String RENDER_SMOKE_MESSAGE = "Pass -Psc4j.render.smoke=true to run the GLFW check.";
    private static final @NotNull String TITLE = "engine input test";
    private static final int WIDTH = 64;
    private static final int HEIGHT = 48;
    private static final int SCALE = 2;
    private static final double WINDOW_X = 10.5;
    private static final double WINDOW_Y = 4.25;
    private static final double WHEEL_OFFSET = -1;
    private static final long PRESS_TIME = 1000;
    private static final int SCAN_CODE = 0;

    @Test
    void installed_callbacks_feed_engine_events_in_framebuffer_pixels() {
        Assumptions.assumeTrue(Boolean.getBoolean(RENDER_SMOKE_PROPERTY), RENDER_SMOKE_MESSAGE);

        try (ClientHost host = ClientHost.initialize()) {
            GlfwHostWindow window = (GlfwHostWindow) host.createWindow(WindowSettings.builder(TITLE, WIDTH, HEIGHT).setDecorated(false).build());
            List<InputEvent> events = new ArrayList<>();
            DoublePressDetector doublePresses = new DoublePressDetector();
            GlfwInput input = new GlfwInput(window, events::add, doublePresses, () -> PRESS_TIME);
            long handle = window.getHandle();

            Assertions.assertSame(doublePresses, input.getDoublePresses());

            input.setSurfaceSize(WIDTH, HEIGHT, WIDTH * SCALE, HEIGHT * SCALE);

            // Taking the installed callbacks out of the window hands them back; calling them is what GLFW does while it polls.
            GLFWKeyCallback key = GLFW.glfwSetKeyCallback(handle, null);
            GLFWCharCallback character = GLFW.glfwSetCharCallback(handle, null);
            GLFWCursorPosCallback cursor = GLFW.glfwSetCursorPosCallback(handle, null);
            GLFWMouseButtonCallback button = GLFW.glfwSetMouseButtonCallback(handle, null);
            GLFWScrollCallback scroll = GLFW.glfwSetScrollCallback(handle, null);

            Assertions.assertNotNull(key);
            Assertions.assertNotNull(character);
            Assertions.assertNotNull(cursor);
            Assertions.assertNotNull(button);
            Assertions.assertNotNull(scroll);

            try {
                key.invoke(handle, GLFW.GLFW_KEY_ESCAPE, SCAN_CODE, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_SHIFT);
                character.invoke(handle, 'z');
                cursor.invoke(handle, WINDOW_X, WINDOW_Y);
                button.invoke(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_PRESS, 0);
                button.invoke(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_PRESS, 0);
                scroll.invoke(handle, 0, WHEEL_OFFSET);
            } finally {
                key.free();
                character.free();
                cursor.free();
                button.free();
                scroll.free();
            }

            Assertions.assertEquals(List.of(InputEvent.Type.KEY_PRESSED, InputEvent.Type.CHARACTER_TYPED, InputEvent.Type.POINTER_MOVED,
                                            InputEvent.Type.BUTTON_PRESSED, InputEvent.Type.BUTTON_PRESSED, InputEvent.Type.WHEEL_SCROLLED),
                                    events.stream().map(InputEvent::getType).toList());
            Assertions.assertEquals(Key.ESCAPE, events.getFirst().getKey());
            Assertions.assertTrue(events.getFirst().isModifierDown(KeyModifier.SHIFT));
            Assertions.assertEquals('z', events.get(1).getCodePoint());
            Assertions.assertEquals((float) (WINDOW_X * SCALE), events.get(2).getX());
            Assertions.assertEquals((float) (WINDOW_Y * SCALE), events.get(2).getY());
            Assertions.assertEquals(PointerButton.RIGHT, events.get(3).getButton());
            Assertions.assertFalse(events.get(3).isDoublePress());
            Assertions.assertTrue(events.get(4).isDoublePress(), "two presses at one time and pixel");
            Assertions.assertEquals(WHEEL_OFFSET, events.get(5).getVerticalOffset());
            Assertions.assertEquals((float) (WINDOW_X * SCALE), events.get(5).getX(), "the wheel at the last pointer position");
            Assertions.assertEquals(WINDOW_X * SCALE, input.toFramebufferX(WINDOW_X));

            input.refreshSurfaceSize();

            Assertions.assertEquals((float) WINDOW_Y * window.getFramebufferHeight() / HEIGHT, input.toFramebufferY(WINDOW_Y));
        }
    }
}
