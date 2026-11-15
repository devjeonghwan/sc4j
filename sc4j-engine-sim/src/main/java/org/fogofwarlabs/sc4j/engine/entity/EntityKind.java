package org.fogofwarlabs.sc4j.engine.entity;

/**
 * Names the entity pools every game has. A mod registers extension columns and the slot policy per kind.
 */
public enum EntityKind {
    /**
     * The units: everything a player owns or that acts on the map, buildings and resources included.
     */
    UNIT,
    /**
     * The sprites: the positioned visual objects that hold images.
     */
    SPRITE,
    /**
     * The images: the animated pictures of the sprites.
     */
    IMAGE,
    /**
     * The projectiles: the weapon projectiles in flight.
     */
    PROJECTILE
}
