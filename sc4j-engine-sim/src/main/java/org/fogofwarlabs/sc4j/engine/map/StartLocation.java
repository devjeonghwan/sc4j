package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;

/**
 * Names where the player of one map slot starts. Positions are map pixels.
 */
public final class StartLocation {
    private final int slotIndex;
    private final int x;
    private final int y;

    /**
     * Creates a {@link StartLocation} instance.
     *
     * @param slotIndex the map player slot, at least {@code 0}
     * @param x         the x coordinate in pixels
     * @param y         the y coordinate in pixels
     * @throws IllegalArgumentException if the slot is negative
     */
    public StartLocation(int slotIndex, int x, int y) {
        if (slotIndex < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid start location slot: {0}", slotIndex);
        }

        this.slotIndex = slotIndex;
        this.x         = x;
        this.y         = y;
    }

    /**
     * @return the map player slot
     */
    public int getSlotIndex() {
        return this.slotIndex;
    }

    /**
     * @return the x coordinate in pixels
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return the y coordinate in pixels
     */
    public int getY() {
        return this.y;
    }
}
