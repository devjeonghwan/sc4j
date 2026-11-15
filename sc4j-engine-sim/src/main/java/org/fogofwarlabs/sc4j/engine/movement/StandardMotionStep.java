package org.fogofwarlabs.sc4j.engine.movement;

import org.fogofwarlabs.sc4j.engine.entity.MovingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the prospective values of one motion step of a motion model and commits them: the step of the engine movement and projectile
 * defaults, and of a mod that moves its objects by the plain motion profile rules. One instance is reused for every step.
 * {@snippet :
 * motionModel.update(step, object, properties);
 * step.commit(object);
 * motionModel.updateFacing(object, properties);
 *}
 * <p>
 * The step keeps the prospective position, exact position and movement flags after the step; the distance and the animation requests are not
 * needed by a plain moving entity and are dropped.
 */
public final class StandardMotionStep implements MotionStep {
    private int positionX;
    private int positionY;
    private int fixedPositionX;
    private int fixedPositionY;
    private int postMovementFlags;

    /**
     * Creates a {@link StandardMotionStep} instance with every value {@code 0}.
     */
    public StandardMotionStep() {
        this.positionX         = 0;
        this.positionY         = 0;
        this.fixedPositionX    = 0;
        this.fixedPositionY    = 0;
        this.postMovementFlags = 0;
    }

    /**
     * @return the prospective horizontal position in pixels
     */
    public int getPositionX() {
        return this.positionX;
    }

    /**
     * Sets the prospective horizontal position.
     *
     * @param positionX the position in pixels
     */
    @Override
    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    /**
     * @return the prospective vertical position in pixels
     */
    public int getPositionY() {
        return this.positionY;
    }

    /**
     * Sets the prospective vertical position.
     *
     * @param positionY the position in pixels
     */
    @Override
    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    /**
     * @return the prospective horizontal exact position, fixed-point
     */
    @Override
    public int getFixedPositionX() {
        return this.fixedPositionX;
    }

    /**
     * Sets the prospective horizontal exact position.
     *
     * @param fixedPositionX the fixed-point position
     */
    @Override
    public void setFixedPositionX(int fixedPositionX) {
        this.fixedPositionX = fixedPositionX;
    }

    /**
     * @return the prospective vertical exact position, fixed-point
     */
    @Override
    public int getFixedPositionY() {
        return this.fixedPositionY;
    }

    /**
     * Sets the prospective vertical exact position.
     *
     * @param fixedPositionY the fixed-point position
     */
    @Override
    public void setFixedPositionY(int fixedPositionY) {
        this.fixedPositionY = fixedPositionY;
    }

    /**
     * Sets the distance the step covers.
     * <p>
     * The standard step commits the position only.
     *
     * @param speed the fixed-point distance
     */
    @Override
    public void setSpeed(int speed) {
    }

    /**
     * Sets whether the step starts the walking animation.
     * <p>
     * The standard step plays no walking animation.
     *
     * @param starting whether the object starts to move
     */
    @Override
    public void setStartingMovement(boolean starting) {
    }

    /**
     * Sets whether the step ends the walking animation.
     * <p>
     * The standard step plays no walking animation.
     *
     * @param stopping whether the object stops moving
     */
    @Override
    public void setStoppingMovement(boolean stopping) {
    }

    /**
     * Sets the movement flags before the step.
     * <p>
     * The standard step commits the flags after the step only.
     *
     * @param movementFlags the flags ({@link MovementFlags})
     */
    @Override
    public void setPreMovementFlags(int movementFlags) {
    }

    /**
     * Sets the movement flags after the step, applied when the step is committed.
     *
     * @param movementFlags the flags ({@link MovementFlags})
     */
    @Override
    public void setPostMovementFlags(int movementFlags) {
        this.postMovementFlags = movementFlags;
    }

    /**
     * Commits the step calculated for an object ({@link MotionModel#update}): the target heading becomes the movement direction, the
     * movement flags those after the step, the next speed the current speed, the position and the exact position those of the step. The
     * object turns afterwards ({@link MotionModel#updateFacing}).
     *
     * @param object the moving object
     */
    public void commit(@NotNull MovingEntity object) {
        object.setTargetHeading(object.getMovementDirection());
        object.setMovementFlags(this.postMovementFlags);
        object.setNextSpeed(object.getCurrentSpeed());
        object.setPosition(this.positionX, this.positionY);
        object.setFixedPositionX(this.fixedPositionX);
        object.setFixedPositionY(this.fixedPositionY);
    }
}
