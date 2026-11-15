package org.fogofwarlabs.sc4j.engine.entity;

import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Holds the fields of an entity that a motion model ({@link org.fogofwarlabs.sc4j.engine.movement.MotionModel}) moves: its position in pixels,
 * its motion profile and its motion state (movement control and flags, heading, directions, turn rate, speeds, acceleration, velocity,
 * fixed-point position, destination, waypoint and direction target). Units and projectiles are moving entities; one model moves both.
 * <p>
 * The fields keep every value they are set to; directions are in the direction steps of the model that moves the entity, and a mod whose
 * motion keeps narrower fields narrows them in its subclass.
 */
public abstract class MovingEntity extends Entity {
    private int positionX;
    private int positionY;

    private @Nullable MotionProfile motionProfile;
    private int movementControl;
    private int movementFlags;
    private int heading;
    private int targetHeading;
    private int turnRate;
    private int movementDirection;
    private int desiredMovementDirection;
    private int topSpeed;
    private int currentSpeed;
    private int nextSpeed;
    private int acceleration;
    private int velocityX;
    private int velocityY;
    private int fixedPositionX;
    private int fixedPositionY;

    private int movementDestinationX;
    private int movementDestinationY;
    private int movementWaypointX;
    private int movementWaypointY;
    private int directionTargetX;
    private int directionTargetY;

    /**
     * Creates a {@link MovingEntity} instance for one pool slot at {@code (0, 0)} with the motion fields at their initial values
     * ({@link #resetMotion}).
     *
     * @param index the slot index, {@code 1} up to the pool capacity
     */
    protected MovingEntity(int index) {
        super(index);

        this.positionX = 0;
        this.positionY = 0;

        this.motionProfile            = null;
        this.movementControl          = 0;
        this.movementFlags            = 0;
        this.heading                  = 0;
        this.targetHeading            = 0;
        this.turnRate                 = 0;
        this.movementDirection        = 0;
        this.desiredMovementDirection = 0;
        this.topSpeed                 = 0;
        this.currentSpeed             = 0;
        this.nextSpeed                = 0;
        this.acceleration             = 0;
        this.velocityX                = 0;
        this.velocityY                = 0;
        this.fixedPositionX           = 0;
        this.fixedPositionY           = 0;

        this.movementDestinationX = 0;
        this.movementDestinationY = 0;
        this.movementWaypointX    = 0;
        this.movementWaypointY    = 0;
        this.directionTargetX     = 0;
        this.directionTargetY     = 0;
    }

    /**
     * @return the x coordinate in pixels
     */
    public int getPositionX() {
        return this.positionX;
    }

    /**
     * @return the y coordinate in pixels
     */
    public int getPositionY() {
        return this.positionY;
    }

    /**
     * Sets the position.
     *
     * @param positionX the x coordinate in pixels
     * @param positionY the y coordinate in pixels
     */
    public void setPosition(int positionX, int positionY) {
        this.positionX = positionX;
        this.positionY = positionY;
    }

    /**
     * @return the motion profile that drives the object's motion, or {@code null} for none
     */
    public @Nullable MotionProfile getMotionProfile() {
        return this.motionProfile;
    }

    /**
     * Sets the motion profile that drives the object's motion.
     *
     * @param motionProfile the motion profile, or {@code null} for none
     */
    public void setMotionProfile(@Nullable MotionProfile motionProfile) {
        this.motionProfile = motionProfile;
    }

    /**
     * @return the movement control of the motion ({@link org.fogofwarlabs.sc4j.engine.movement.MotionModel#CONTROL_MOTION_PROFILE} and the like)
     */
    public int getMovementControl() {
        return this.movementControl;
    }

    /**
     * Sets the movement control.
     *
     * @param movementControl the movement control
     */
    public void setMovementControl(int movementControl) {
        this.movementControl = movementControl;
    }

    /**
     * @return the movement flags ({@link org.fogofwarlabs.sc4j.engine.movement.MovementFlags})
     */
    public int getMovementFlags() {
        return this.movementFlags;
    }

    /**
     * Sets the movement flags.
     *
     * @param movementFlags the movement flags
     */
    public void setMovementFlags(int movementFlags) {
        this.movementFlags = movementFlags;
    }

    /**
     * @return the direction the object faces, in direction steps clockwise from up
     */
    public int getHeading() {
        return this.heading;
    }

    /**
     * Sets the direction the object faces.
     *
     * @param heading the direction
     */
    public void setHeading(int heading) {
        this.heading = heading;
    }

    /**
     * @return the direction the object turns to
     */
    public int getTargetHeading() {
        return this.targetHeading;
    }

    /**
     * Sets the direction the object turns to.
     *
     * @param targetHeading the direction
     */
    public void setTargetHeading(int targetHeading) {
        this.targetHeading = targetHeading;
    }

    /**
     * @return the turn rate in direction steps per frame
     */
    public int getTurnRate() {
        return this.turnRate;
    }

    /**
     * Sets the turn rate.
     *
     * @param turnRate the turn rate
     */
    public void setTurnRate(int turnRate) {
        this.turnRate = turnRate;
    }

    /**
     * @return the direction the object moves in
     */
    public int getMovementDirection() {
        return this.movementDirection;
    }

    /**
     * Sets the direction the object moves in.
     *
     * @param movementDirection the direction
     */
    public void setMovementDirection(int movementDirection) {
        this.movementDirection = movementDirection;
    }

    /**
     * @return the direction toward the current waypoint
     */
    public int getDesiredMovementDirection() {
        return this.desiredMovementDirection;
    }

    /**
     * Sets the direction toward the current waypoint.
     *
     * @param desiredMovementDirection the direction
     */
    public void setDesiredMovementDirection(int desiredMovementDirection) {
        this.desiredMovementDirection = desiredMovementDirection;
    }

    /**
     * @return the top speed, fixed-point with eight fraction bits
     */
    public int getTopSpeed() {
        return this.topSpeed;
    }

    /**
     * Sets the top speed.
     *
     * @param topSpeed the top speed, fixed-point with eight fraction bits
     */
    public void setTopSpeed(int topSpeed) {
        this.topSpeed = topSpeed;
    }

    /**
     * @return the current speed, fixed-point with eight fraction bits
     */
    public int getCurrentSpeed() {
        return this.currentSpeed;
    }

    /**
     * Sets the current speed.
     *
     * @param currentSpeed the speed, fixed-point with eight fraction bits
     */
    public void setCurrentSpeed(int currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    /**
     * @return the speed the motion accelerates to, fixed-point with eight fraction bits
     */
    public int getNextSpeed() {
        return this.nextSpeed;
    }

    /**
     * Sets the speed the motion accelerates to.
     *
     * @param nextSpeed the speed, fixed-point with eight fraction bits
     */
    public void setNextSpeed(int nextSpeed) {
        this.nextSpeed = nextSpeed;
    }

    /**
     * @return the acceleration per frame, fixed-point with eight fraction bits
     */
    public int getAcceleration() {
        return this.acceleration;
    }

    /**
     * Sets the acceleration per frame.
     *
     * @param acceleration the acceleration, fixed-point with eight fraction bits
     */
    public void setAcceleration(int acceleration) {
        this.acceleration = acceleration;
    }

    /**
     * @return the horizontal velocity, fixed-point with eight fraction bits
     */
    public int getVelocityX() {
        return this.velocityX;
    }

    /**
     * Sets the horizontal velocity.
     *
     * @param velocityX the velocity, fixed-point with eight fraction bits
     */
    public void setVelocityX(int velocityX) {
        this.velocityX = velocityX;
    }

    /**
     * @return the vertical velocity, fixed-point with eight fraction bits
     */
    public int getVelocityY() {
        return this.velocityY;
    }

    /**
     * Sets the vertical velocity.
     *
     * @param velocityY the velocity, fixed-point with eight fraction bits
     */
    public void setVelocityY(int velocityY) {
        this.velocityY = velocityY;
    }

    /**
     * @return the horizontal position, fixed-point with eight fraction bits
     */
    public int getFixedPositionX() {
        return this.fixedPositionX;
    }

    /**
     * Sets the horizontal position with eight fraction bits.
     *
     * @param fixedPositionX the fixed-point position
     */
    public void setFixedPositionX(int fixedPositionX) {
        this.fixedPositionX = fixedPositionX;
    }

    /**
     * @return the vertical position, fixed-point with eight fraction bits
     */
    public int getFixedPositionY() {
        return this.fixedPositionY;
    }

    /**
     * Sets the vertical position with eight fraction bits.
     *
     * @param fixedPositionY the fixed-point position
     */
    public void setFixedPositionY(int fixedPositionY) {
        this.fixedPositionY = fixedPositionY;
    }

    /**
     * @return the horizontal map position of the movement destination
     */
    public int getMovementDestinationX() {
        return this.movementDestinationX;
    }

    /**
     * Sets the horizontal map position of the movement destination.
     *
     * @param movementDestinationX the position
     */
    public void setMovementDestinationX(int movementDestinationX) {
        this.movementDestinationX = movementDestinationX;
    }

    /**
     * @return the vertical map position of the movement destination
     */
    public int getMovementDestinationY() {
        return this.movementDestinationY;
    }

    /**
     * Sets the vertical map position of the movement destination.
     *
     * @param movementDestinationY the position
     */
    public void setMovementDestinationY(int movementDestinationY) {
        this.movementDestinationY = movementDestinationY;
    }

    /**
     * @return the horizontal map position of the current waypoint
     */
    public int getMovementWaypointX() {
        return this.movementWaypointX;
    }

    /**
     * Sets the horizontal map position of the current waypoint.
     *
     * @param movementWaypointX the position
     */
    public void setMovementWaypointX(int movementWaypointX) {
        this.movementWaypointX = movementWaypointX;
    }

    /**
     * @return the vertical map position of the current waypoint
     */
    public int getMovementWaypointY() {
        return this.movementWaypointY;
    }

    /**
     * Sets the vertical map position of the current waypoint.
     *
     * @param movementWaypointY the position
     */
    public void setMovementWaypointY(int movementWaypointY) {
        this.movementWaypointY = movementWaypointY;
    }

    /**
     * @return the horizontal map position the object turns to when it stands on its waypoint
     */
    public int getDirectionTargetX() {
        return this.directionTargetX;
    }

    /**
     * Sets the horizontal map position the object turns to when it stands on its waypoint.
     *
     * @param directionTargetX the position
     */
    public void setDirectionTargetX(int directionTargetX) {
        this.directionTargetX = directionTargetX;
    }

    /**
     * @return the vertical map position the object turns to when it stands on its waypoint
     */
    public int getDirectionTargetY() {
        return this.directionTargetY;
    }

    /**
     * Sets the vertical map position the object turns to when it stands on its waypoint.
     *
     * @param directionTargetY the position
     */
    public void setDirectionTargetY(int directionTargetY) {
        this.directionTargetY = directionTargetY;
    }

    /**
     * Resets every motion field to its initial value: no motion profile, every direction, speed, flag and coordinate {@code 0}, the position stays.
     */
    public void resetMotion() {
        this.motionProfile            = null;
        this.movementControl          = 0;
        this.movementFlags            = 0;
        this.heading                  = 0;
        this.targetHeading            = 0;
        this.turnRate                 = 0;
        this.movementDirection        = 0;
        this.desiredMovementDirection = 0;
        this.topSpeed                 = 0;
        this.currentSpeed             = 0;
        this.nextSpeed                = 0;
        this.acceleration             = 0;
        this.velocityX                = 0;
        this.velocityY                = 0;
        this.fixedPositionX           = 0;
        this.fixedPositionY           = 0;

        this.movementDestinationX = 0;
        this.movementDestinationY = 0;
        this.movementWaypointX    = 0;
        this.movementWaypointY    = 0;
        this.directionTargetX     = 0;
        this.directionTargetY     = 0;
    }

    /**
     * Mixes the position, the motion profile index and the motion fields into a hash, in declaration order.
     *
     * @param hash the hash being computed
     */
    protected final void contributeMotionStateHash(@NotNull StateHash hash) {
        hash.mix(this.positionX);
        hash.mix(this.positionY);
        hash.mix(ContentType.indexOf(this.motionProfile));
        hash.mix(this.movementControl);
        hash.mix(this.movementFlags);
        hash.mix(this.heading);
        hash.mix(this.targetHeading);
        hash.mix(this.turnRate);
        hash.mix(this.movementDirection);
        hash.mix(this.desiredMovementDirection);
        hash.mix(this.topSpeed);
        hash.mix(this.currentSpeed);
        hash.mix(this.nextSpeed);
        hash.mix(this.acceleration);
        hash.mix(this.velocityX);
        hash.mix(this.velocityY);
        hash.mix(this.fixedPositionX);
        hash.mix(this.fixedPositionY);
        hash.mix(this.movementDestinationX);
        hash.mix(this.movementDestinationY);
        hash.mix(this.movementWaypointX);
        hash.mix(this.movementWaypointY);
        hash.mix(this.directionTargetX);
        hash.mix(this.directionTargetY);
    }

    /**
     * Writes the position, the motion profile index and the motion fields, in declaration order.
     *
     * @param writer the writer of the snapshot
     */
    protected final void writeMotionSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInt(this.positionX);
        writer.writeInt(this.positionY);
        writer.writeType(this.motionProfile);
        writer.writeInt(this.movementControl);
        writer.writeInt(this.movementFlags);
        writer.writeInt(this.heading);
        writer.writeInt(this.targetHeading);
        writer.writeInt(this.turnRate);
        writer.writeInt(this.movementDirection);
        writer.writeInt(this.desiredMovementDirection);
        writer.writeInt(this.topSpeed);
        writer.writeInt(this.currentSpeed);
        writer.writeInt(this.nextSpeed);
        writer.writeInt(this.acceleration);
        writer.writeInt(this.velocityX);
        writer.writeInt(this.velocityY);
        writer.writeInt(this.fixedPositionX);
        writer.writeInt(this.fixedPositionY);
        writer.writeInt(this.movementDestinationX);
        writer.writeInt(this.movementDestinationY);
        writer.writeInt(this.movementWaypointX);
        writer.writeInt(this.movementWaypointY);
        writer.writeInt(this.directionTargetX);
        writer.writeInt(this.directionTargetY);
    }

    /**
     * Reads the position, the motion profile and the motion fields back, in declaration order.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or names no motion profile of the game
     */
    protected final void readMotionSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        this.positionX                = reader.readInt();
        this.positionY                = reader.readInt();
        this.motionProfile            = reader.readType(reader.getGame().getContent().getDefinition().getMotionProfiles().getTypes());
        this.movementControl          = reader.readInt();
        this.movementFlags            = reader.readInt();
        this.heading                  = reader.readInt();
        this.targetHeading            = reader.readInt();
        this.turnRate                 = reader.readInt();
        this.movementDirection        = reader.readInt();
        this.desiredMovementDirection = reader.readInt();
        this.topSpeed                 = reader.readInt();
        this.currentSpeed             = reader.readInt();
        this.nextSpeed                = reader.readInt();
        this.acceleration             = reader.readInt();
        this.velocityX                = reader.readInt();
        this.velocityY                = reader.readInt();
        this.fixedPositionX           = reader.readInt();
        this.fixedPositionY           = reader.readInt();
        this.movementDestinationX     = reader.readInt();
        this.movementDestinationY     = reader.readInt();
        this.movementWaypointX        = reader.readInt();
        this.movementWaypointY        = reader.readInt();
        this.directionTargetX         = reader.readInt();
        this.directionTargetY         = reader.readInt();
    }
}
