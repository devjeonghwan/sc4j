package org.fogofwarlabs.sc4j.engine.content.definition;

/**
 * Names the traits of a unit type that engine systems read. A unit type table stores the traits of a type as a mask of
 * {@link #getMask()} bits; bit positions are the declaration order and part of the state hash, so new traits are only
 * appended. Traits that only a mod reads are an extension column of the mod.
 */
public enum UnitTrait {
    /**
     * The unit type is a building: it is placed on the grid and does not move.
     */
    BUILDING,
    /**
     * The unit type gathers resources and constructs buildings.
     */
    WORKER,
    /**
     * The unit type flies: it ignores ground collision and terrain.
     */
    FLYER,
    /**
     * The unit type accepts gathered resources.
     */
    RESOURCE_DEPOT,
    /**
     * The unit type holds resources that workers gather.
     */
    RESOURCE_CONTAINER,
    /**
     * The unit type reveals hidden units in its sight range.
     */
    DETECTOR,
    /**
     * The unit type has energy and casts abilities.
     */
    SPELLCASTER;

    /**
     * @return the bit of this trait in a trait mask
     */
    public int getMask() {
        return 1 << this.ordinal();
    }
}
