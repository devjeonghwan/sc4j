package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Names a rectangle of the map that standard trigger conditions count units in and standard trigger actions act in: a map location. Edges are
 * pixels and inclusive; a unit is in the area when its position is.
 */
public final class TriggerArea {
    /**
     * The divisor that gives the middle of two edges.
     */
    private static final int HALF = 2;

    private final @NotNull String name;
    private final int left;
    private final int top;
    private final int right;
    private final int bottom;

    /**
     * Creates a {@link TriggerArea} instance.
     *
     * @param name   the name of the area, as the map names it
     * @param left   the left edge
     * @param top    the top edge
     * @param right  the right edge, at least the left edge
     * @param bottom the bottom edge, at least the top edge
     * @throws IllegalArgumentException if an edge lies before its opposite edge
     */
    public TriggerArea(@NotNull String name, int left, int top, int right, int bottom) {
        if (right < left || bottom < top) {
            throw Exceptions.newIllegalArgumentException("The area {0} has its right or bottom edge before its left or top edge.", name);
        }

        this.name   = name;
        this.left   = left;
        this.top    = top;
        this.right  = right;
        this.bottom = bottom;
    }

    /**
     * @return the name of the area
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the left edge
     */
    public int getLeft() {
        return this.left;
    }

    /**
     * @return the top edge
     */
    public int getTop() {
        return this.top;
    }

    /**
     * @return the right edge, inclusive
     */
    public int getRight() {
        return this.right;
    }

    /**
     * @return the bottom edge, inclusive
     */
    public int getBottom() {
        return this.bottom;
    }

    /**
     * @return the x coordinate of the center
     */
    public int getCenterX() {
        return this.left + (this.right - this.left) / HALF;
    }

    /**
     * @return the y coordinate of the center
     */
    public int getCenterY() {
        return this.top + (this.bottom - this.top) / HALF;
    }

    /**
     * Checks whether a point lies in the area.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return {@code true} if the point lies within the edges, {@code false} otherwise
     */
    public boolean contains(int x, int y) {
        return x >= this.left && x <= this.right && y >= this.top && y <= this.bottom;
    }
}
