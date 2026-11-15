package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a unit the map places before the game starts: its type, the map player slot that owns it and its position in map pixels. The mod's
 * own unit properties (hit points, resources, states) stay in its map data.
 */
public final class PreplacedUnit {
    private final @NotNull UnitType type;
    private final int ownerSlotIndex;
    private final int x;
    private final int y;

    /**
     * Creates a {@link PreplacedUnit} instance.
     *
     * @param type           the unit type
     * @param ownerSlotIndex the map player slot that owns the unit, at least {@code 0}
     * @param x              the x coordinate in pixels
     * @param y              the y coordinate in pixels
     * @throws IllegalArgumentException if the slot is negative
     */
    public PreplacedUnit(@NotNull UnitType type, int ownerSlotIndex, int x, int y) {
        if (ownerSlotIndex < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid preplaced unit owner slot: {0}", ownerSlotIndex);
        }

        this.type           = type;
        this.ownerSlotIndex = ownerSlotIndex;
        this.x              = x;
        this.y              = y;
    }

    /**
     * @return the unit type
     */
    public @NotNull UnitType getType() {
        return this.type;
    }

    /**
     * @return the map player slot that owns the unit
     */
    public int getOwnerSlotIndex() {
        return this.ownerSlotIndex;
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
