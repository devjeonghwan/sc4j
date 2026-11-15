package org.fogofwarlabs.sc4j.engine.content.registry;

/**
 * Names the per-game type tables, the kinds a mod adds extension columns to. The declaration order is the order in
 * which a game creates its tables and mixes them into the state hash.
 */
public enum TableKind {
    /**
     * The unit type table.
     */
    UNIT,
    /**
     * The weapon type table.
     */
    WEAPON,
    /**
     * The motion profile table.
     */
    MOTION_PROFILE,
    /**
     * The sprite type table.
     */
    SPRITE,
    /**
     * The image type table.
     */
    IMAGE,
    /**
     * The upgrade type table.
     */
    UPGRADE,
    /**
     * The tech type table.
     */
    TECH,
    /**
     * The order type table.
     */
    ORDER,
    /**
     * The sound type table.
     */
    SOUND
}
