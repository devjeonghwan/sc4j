package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.engine.client.input.DoublePressDetector;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.InputListener;
import org.jetbrains.annotations.NotNull;

/**
 * Feeds the input of a {@link HostWindow} to an {@link InputListener} as engine {@link InputEvent}s: keys, typed characters, pointer
 * movements, button presses and releases, and wheel movements, in the order the windowing library reports them while the host polls events
 * ({@link HostWindow#createInput}).
 * <p>
 * Pointer positions are converted from window coordinates to framebuffer pixels with the surface size last set ({@link #setSurfaceSize} or
 * {@link #refreshSurfaceSize}). A press of the left, right or middle button runs the {@link DoublePressDetector} at the floored window
 * coordinates of the cursor with the clock's time.
 */
public interface WindowInput {
    /**
     * @return the double-press detector of the button presses
     */
    @NotNull DoublePressDetector getDoublePresses();

    /**
     * Sets the surface size the pointer positions convert with.
     *
     * @param windowWidth       the window width in window coordinates
     * @param windowHeight      the window height in window coordinates
     * @param framebufferWidth  the framebuffer width in pixels
     * @param framebufferHeight the framebuffer height in pixels
     */
    void setSurfaceSize(int windowWidth, int windowHeight, int framebufferWidth, int framebufferHeight);


    /**
     * Reads the window's current window and framebuffer sizes as the surface size; a client calls it once per frame.
     */
    void refreshSurfaceSize();

    /**
     * Converts a window column to a framebuffer column.
     *
     * @param windowX the column in window coordinates
     * @return the framebuffer column
     */
    float toFramebufferX(double windowX);

    /**
     * Converts a window row to a framebuffer row.
     *
     * @param windowY the row in window coordinates
     * @return the framebuffer row
     */
    float toFramebufferY(double windowY);
}
