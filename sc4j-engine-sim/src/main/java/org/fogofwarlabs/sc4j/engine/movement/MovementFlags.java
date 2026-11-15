package org.fogofwarlabs.sc4j.engine.movement;

import org.fogofwarlabs.sc4j.engine.entity.Unit;

/**
 * Names the bits of the movement flags of a moving object ({@link Unit#getMovementFlags()}) that the motion model ({@link MotionModel}) reads
 * and writes.
 */
public final class MovementFlags {
    /**
     * Set with every new destination; cleared once the object faces the direction of its waypoint.
     */
    public static final int ORDERED = 0x1;
    /**
     * The object is away from its destination and accelerating.
     */
    public static final int ACCELERATING = 0x2;
    /**
     * The object is braking; cleared by a new destination.
     */
    public static final int BRAKING = 0x4;
    /**
     * An attack animation is starting: the motion neither starts nor stops the walking animation, and an animation-controlled object does not
     * accelerate.
     */
    public static final int STARTING_ATTACK = 0x8;
    /**
     * The object follows a path step.
     */
    public static final int MOVING = 0x10;
    /**
     * The motion continues through the destination: the next order moves the object on, so it does not stop there.
     */
    public static final int PASSING_THROUGH = 0x20;
    /**
     * The object brakes on its next path step.
     */
    public static final int BRAKE_ON_PATH_STEP = 0x40;

    private MovementFlags() {
    }
}
