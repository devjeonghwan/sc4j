package org.fogofwarlabs.sc4j.engine.client.world;

/**
 * Says what a unit's picture shows, from its {@link org.fogofwarlabs.sc4j.engine.client.view.UnitView}: a {@link UnitSprite} names the
 * frame columns of its sheet for each pose.
 */
public enum UnitPose {
    /**
     * The unit stands.
     */
    IDLE,
    /**
     * The unit moves.
     */
    MOVING,
    /**
     * The unit attacks a target in its range.
     */
    ATTACKING,
    /**
     * The unit is a building under construction; its columns show the construction stages in order.
     */
    CONSTRUCTING
}
