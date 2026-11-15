package org.fogofwarlabs.sc4j.engine.content.definition;

/**
 * Names the kinds of target a weapon type can attack. A weapon type table stores them as a mask of {@link #getMask()}
 * bits; bit positions are the declaration order and part of the state hash, so new targets are only appended.
 */
public enum WeaponTarget {
    /**
     * Units on the ground.
     */
    GROUND,
    /**
     * Flying units.
     */
    AIR,
    /**
     * Buildings.
     */
    BUILDING,
    /**
     * Units of the attacker's own player.
     */
    OWN_UNIT;

    /**
     * @return the bit of this target in a target mask
     */
    public int getMask() {
        return 1 << this.ordinal();
    }
}
