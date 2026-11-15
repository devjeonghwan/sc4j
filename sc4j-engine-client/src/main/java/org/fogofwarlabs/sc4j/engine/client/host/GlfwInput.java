package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.engine.client.input.DoublePressDetector;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.InputListener;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.LongSupplier;

/**
 * Feeds the GLFW input callbacks of a {@link HostWindow} to an {@link InputListener} as engine {@link InputEvent}s: the engine default
 * {@link WindowInput}. It reports keys, typed characters, pointer movements, button presses and releases, and wheel movements, in the order
 * GLFW reports them while the host polls events.
 * <p>
 * Pointer positions are converted from window coordinates to framebuffer pixels with the surface size last set ({@link #setSurfaceSize} or
 * {@link #refreshSurfaceSize}). A press of the left, right or middle button runs the {@link DoublePressDetector} at the floored window
 * coordinates of the cursor with the clock's time. The callbacks stay installed until the window closes, which frees them.
 */
@ApiStatus.Internal
public final class GlfwInput implements WindowInput {
    private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;
    private static final @NotNull GlfwKeyCodes KEY_CODES = GlfwKeyCodes.getInstance();

    private final @NotNull GlfwHostWindow window;
    private final @NotNull InputListener listener;
    private final @NotNull DoublePressDetector doublePresses;
    private final @NotNull LongSupplier clock;

    private final double @NotNull [] cursorX;
    private final double @NotNull [] cursorY;
    private final int @NotNull [] sizeWidth;
    private final int @NotNull [] sizeHeight;

    private int windowWidth;
    private int windowHeight;
    private int framebufferWidth;
    private int framebufferHeight;
    private float pointerX;
    private float pointerY;

    /**
     * Creates a {@link GlfwInput} instance with a {@link DoublePressDetector} of the default interval and the system's monotonic clock, reads
     * the window's surface size and installs the callbacks.
     *
     * @param window   the window
     * @param listener the receiver of the events
     */
    public GlfwInput(@NotNull GlfwHostWindow window, @NotNull InputListener listener) {
        this(window, listener, new DoublePressDetector(), () -> System.nanoTime() / NANOSECONDS_PER_MILLISECOND);

        this.refreshSurfaceSize();
    }

    /**
     * Creates a {@link GlfwInput} instance and installs the callbacks; the surface size starts empty (a window width and height of {@code 0}
     * convert as {@code 1}) until it is set.
     *
     * @param window        the window
     * @param listener      the receiver of the events
     * @param doublePresses the detector that pairs button presses
     * @param clock         the clock of the button presses, in milliseconds
     */
    public GlfwInput(@NotNull GlfwHostWindow window,
                     @NotNull InputListener listener,
                     @NotNull DoublePressDetector doublePresses,
                     @NotNull LongSupplier clock) {
        this.window        = window;
        this.listener      = listener;
        this.doublePresses = doublePresses;
        this.clock         = clock;

        this.cursorX    = new double[1];
        this.cursorY    = new double[1];
        this.sizeWidth  = new int[1];
        this.sizeHeight = new int[1];

        this.windowWidth       = 0;
        this.windowHeight      = 0;
        this.framebufferWidth  = 0;
        this.framebufferHeight = 0;
        this.pointerX          = 0;
        this.pointerY          = 0;

        this.install();
    }

    /**
     * @return the double-press detector of the button presses
     */
    @Override
    public @NotNull DoublePressDetector getDoublePresses() {
        return this.doublePresses;
    }

    /**
     * Sets the surface size the pointer positions convert with.
     *
     * @param windowWidth       the window width in window coordinates
     * @param windowHeight      the window height in window coordinates
     * @param framebufferWidth  the framebuffer width in pixels
     * @param framebufferHeight the framebuffer height in pixels
     */
    @Override
    public void setSurfaceSize(int windowWidth, int windowHeight, int framebufferWidth, int framebufferHeight) {
        this.windowWidth       = windowWidth;
        this.windowHeight      = windowHeight;
        this.framebufferWidth  = framebufferWidth;
        this.framebufferHeight = framebufferHeight;
    }


    /**
     * Reads the window's current window and framebuffer sizes as the surface size; a client calls it once per frame.
     */
    @Override
    public void refreshSurfaceSize() {
        GLFW.glfwGetWindowSize(this.window.getHandle(), this.sizeWidth, this.sizeHeight);

        int width = this.sizeWidth[0];
        int height = this.sizeHeight[0];

        GLFW.glfwGetFramebufferSize(this.window.getHandle(), this.sizeWidth, this.sizeHeight);
        this.setSurfaceSize(width, height, this.sizeWidth[0], this.sizeHeight[0]);
    }

    /**
     * Converts a window column to a framebuffer column.
     *
     * @param windowX the column in window coordinates
     * @return the framebuffer column
     */
    @Override
    public float toFramebufferX(double windowX) {
        return (float) windowX * this.framebufferWidth / Math.max(1, this.windowWidth);
    }

    /**
     * Converts a window row to a framebuffer row.
     *
     * @param windowY the row in window coordinates
     * @return the framebuffer row
     */
    @Override
    public float toFramebufferY(double windowY) {
        return (float) windowY * this.framebufferHeight / Math.max(1, this.windowHeight);
    }

    private void install() {
        long handle = this.window.getHandle();

        GLFW.glfwSetKeyCallback(handle, (targetWindow, key, scanCode, action, modifiers) -> {
            this.listener.onInput(KEY_CODES.createKeyEvent(key, action, modifiers));
        });
        GLFW.glfwSetCharCallback(handle, (targetWindow, codePoint) -> this.listener.onInput(InputEvent.createCharacterTyped(codePoint)));
        GLFW.glfwSetCursorPosCallback(handle, (targetWindow, horizontal, vertical) -> {
            this.pointerX = this.toFramebufferX(horizontal);
            this.pointerY = this.toFramebufferY(vertical);
            this.listener.onInput(InputEvent.createPointerMoved(this.pointerX, this.pointerY));
        });
        GLFW.glfwSetMouseButtonCallback(handle, (targetWindow, button, action, modifiers) -> this.onButton(button, action, modifiers));
        GLFW.glfwSetScrollCallback(handle, (targetWindow, horizontalOffset, verticalOffset) -> {
            this.listener.onInput(InputEvent.createWheelScrolled(horizontalOffset, verticalOffset, this.pointerX, this.pointerY));
        });
    }

    private void onButton(int code, int action, int modifiers) {
        PointerButton button = KEY_CODES.getButton(code);

        if (button == null) {
            return;
        }

        GLFW.glfwGetCursorPos(this.window.getHandle(), this.cursorX, this.cursorY);

        float x = this.toFramebufferX(this.cursorX[0]);
        float y = this.toFramebufferY(this.cursorY[0]);
        int engineModifiers = KEY_CODES.getModifiers(modifiers);

        if (action == GLFW.GLFW_PRESS) {
            boolean doublePress = this.doublePresses.press(button, this.clock.getAsLong(),
                                                           (int) Math.floor(this.cursorX[0]), (int) Math.floor(this.cursorY[0]));

            this.listener.onInput(InputEvent.createButtonPressed(button, engineModifiers, x, y, doublePress));
        } else {
            this.listener.onInput(InputEvent.createButtonReleased(button, engineModifiers, x, y));
        }
    }
}
