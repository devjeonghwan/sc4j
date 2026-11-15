/**
 * Defines the pooled simulation objects: {@link org.fogofwarlabs.sc4j.engine.entity.Entity} and the engine kinds
 * {@link org.fogofwarlabs.sc4j.engine.entity.Unit}, {@link org.fogofwarlabs.sc4j.engine.entity.Sprite},
 * {@link org.fogofwarlabs.sc4j.engine.entity.Image} and {@link org.fogofwarlabs.sc4j.engine.entity.Projectile}.
 * <p>
 * An entity is one persistent slot of its pool ({@code entity.pool}): the game creates every slot object when it is
 * created and reuses the slots, so the simulation allocates nothing per frame. An entity holds only the engine fields of
 * its kind; what a mod adds lives in the pool's extension columns ({@code entity.column}), arrays indexed by the
 * entity index.
 */
package org.fogofwarlabs.sc4j.engine.entity;
