package org.fogofwarlabs.sc4j.engine.movement;

/**
 * Receives the prospective values of one movement step ({@link MotionModel#update}): the position the object would reach, its speed,
 * whether the step starts or stops its walking animation, and its movement flags before and after the step. The movement implementation
 * decides whether and how the step is committed.
 * <p>
 * Positions are map pixels; exact positions and the speed are fixed-point values ({@link MotionModel#FRACTION_BITS}).
 */
public interface MotionStep {
    /**
     * Sets the prospective horizontal position.
     *
     * @param positionX the position in pixels
     */
    void setPositionX(int positionX);

    /**
     * Sets the prospective vertical position.
     *
     * @param positionY the position in pixels
     */
    void setPositionY(int positionY);

    /**
     * @return the prospective horizontal exact position, fixed-point
     */
    int getFixedPositionX();

    /**
     * Sets the prospective horizontal exact position.
     *
     * @param fixedPositionX the fixed-point position
     */
    void setFixedPositionX(int fixedPositionX);

    /**
     * @return the prospective vertical exact position, fixed-point
     */
    int getFixedPositionY();

    /**
     * Sets the prospective vertical exact position.
     *
     * @param fixedPositionY the fixed-point position
     */
    void setFixedPositionY(int fixedPositionY);

    /**
     * Sets the distance the step covers.
     *
     * @param speed the fixed-point distance
     */
    void setSpeed(int speed);

    /**
     * Sets whether the step starts the walking animation.
     *
     * @param starting whether the object starts to move
     */
    void setStartingMovement(boolean starting);

    /**
     * Sets whether the step ends the walking animation.
     *
     * @param stopping whether the object stops moving
     */
    void setStoppingMovement(boolean stopping);

    /**
     * Sets the movement flags before the step.
     *
     * @param movementFlags the flags ({@link MovementFlags})
     */
    void setPreMovementFlags(int movementFlags);

    /**
     * Sets the movement flags after the step, applied when the step is committed.
     *
     * @param movementFlags the flags ({@link MovementFlags})
     */
    void setPostMovementFlags(int movementFlags);
}
