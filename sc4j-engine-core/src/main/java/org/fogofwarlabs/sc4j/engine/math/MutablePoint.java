package org.fogofwarlabs.sc4j.engine.math;

/**
 * Holds a position that a query writes into, so that a query returning a position allocates nothing: the caller keeps
 * one instance and passes it to every call.
 */
public final class MutablePoint {
    private int x;
    private int y;

    /**
     * Creates a {@link MutablePoint} instance at {@code (0, 0)}.
     */
    public MutablePoint() {
        this.x = 0;
        this.y = 0;
    }

    /**
     * @return the x coordinate
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return the y coordinate
     */
    public int getY() {
        return this.y;
    }

    /**
     * Sets both coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
