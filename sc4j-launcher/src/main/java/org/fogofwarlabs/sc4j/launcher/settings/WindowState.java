package org.fogofwarlabs.sc4j.launcher.settings;

/**
 * Holds the launcher window's position and size as {@code userdata/launcher.json} stores them, in JavaFX screen coordinates (logical units,
 * which cover several screen pixels on a scaled display).
 */
public final class WindowState {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final boolean maximized;

    /**
     * Creates a {@link WindowState} instance.
     *
     * @param x         the left edge of the window
     * @param y         the top edge of the window
     * @param width     the window width
     * @param height    the window height
     * @param maximized whether the window is maximized; the bounds are then those it returns to
     */
    public WindowState(int x, int y, int width, int height, boolean maximized) {
        this.x         = x;
        this.y         = y;
        this.width     = width;
        this.height    = height;
        this.maximized = maximized;
    }

    /**
     * @return the left edge of the window
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return the top edge of the window
     */
    public int getY() {
        return this.y;
    }

    /**
     * @return the window width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the window height
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return {@code true} if the window is maximized, {@code false} otherwise
     */
    public boolean isMaximized() {
        return this.maximized;
    }
}
