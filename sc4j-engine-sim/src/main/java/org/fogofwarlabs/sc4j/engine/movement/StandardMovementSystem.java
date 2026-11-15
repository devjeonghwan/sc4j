package org.fogofwarlabs.sc4j.engine.movement;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.modifier.Stat;
import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.table.MotionProfileTable;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.MovingEntity;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhase;
import org.fogofwarlabs.sc4j.engine.hook.HookList;
import org.fogofwarlabs.sc4j.engine.math.MutablePoint;
import org.fogofwarlabs.sc4j.engine.path.PathfindingSystem;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.spatial.SpatialIndex;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Moves the units of the game's unit pool by a motion model ({@link MotionModel}) the mod declares: the engine default {@link MovementSystem}. A
 * mod registers it under {@link MovementSystem#KEY} and lists it as a frame phase; it reads the motion profile values from the game's motion
 * profile table ({@link TableMotionProfileProperties}).
 * {@snippet :
 * MotionModel motionModel = new MotionModel.Builder(64).build();
 *
 * builder.useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(motionModel))
 *        .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement);
 *}
 * <p>
 * Without a path finding system a unit moves straight to its destination: the waypoint and the direction target are the destination. With a
 * path finding system ({@link PathfindingSystem#KEY}) a ground unit (a unit whose type is not a {@link UnitTrait#FLYER}) follows its path: before
 * each motion step away from its destination the unit's path is brought up to date ({@link PathfindingSystem#updatePath}) and its waypoint and
 * direction target become the path's current waypoint ({@link PathfindingSystem#getWaypoint}); the unit heads through a waypoint short of its
 * destination without braking ({@link MovementFlags#MOVING}) and brakes for its destination. A ground unit whose destination cannot be reached
 * stops where it is ({@link #stop}). Flying units move straight.
 * <p>
 * The motion step of a frame follows a followed unit's position, follows the path, resynchronizes the exact position of a unit that was put
 * elsewhere since its last step, calculates the prospective step and commits it: the target heading becomes the movement direction, the movement
 * flags and the position those of the step, the speed it accelerates from its current speed; then the unit turns, and a moved unit is relocated
 * in the game's spatial index, if it has one (moving units must be indexed there). A unit stops at its destination by the motion model's rules.
 * <p>
 * Determinism contract: the motion step runs once per running frame for every unit of the pool in slot order, draws no random value and
 * allocates nothing; the path updates run in that order too. The operations change only the given unit and its path, run the
 * {@link MovementSystem#TRANSPORT_PICKUP} hooks in registration order until one answers, and allocate nothing. The followed units are mixed into
 * the state hash in slot order; the motion fields are part of the unit hash and the paths part of the path finding system's hash.
 */
public final class StandardMovementSystem implements MovementSystem, FramePhase, StateHashContributor, SnapshotContributor {
    /**
     * The distance in pixels of the direction target {@link #turnTo} sets ahead of a unit.
     */
    private static final int TURN_TARGET_DISTANCE = 256;

    private static <T> @NotNull T requireAttached(@Nullable T value) {
        if (value == null) {
            throw Exceptions.newIllegalStateException("The standard movement system is not attached.");
        }

        return value;
    }

    private final @NotNull MotionModel motionModel;
    private final @NotNull StandardMotionStep step;
    private final @NotNull MutablePoint waypoint;

    private @Nullable EntityPool<Unit> units;
    private @Nullable GameContent content;
    private @Nullable MotionProfileProperties<MovingEntity> properties;
    private @Nullable HookList<TransportPickupHook> pickupHooks;
    private @Nullable SpatialIndex spatialIndex;
    private @Nullable PathfindingSystem pathfinding;

    private @Nullable Unit @NotNull [] targetUnits;

    /**
     * Creates a {@link StandardMovementSystem} instance; it moves nothing until it is attached.
     *
     * @param motionModel the motion model that moves the units
     */
    public StandardMovementSystem(@NotNull MotionModel motionModel) {
        this.motionModel = motionModel;
        this.step        = new StandardMotionStep();
        this.waypoint    = new MutablePoint();

        this.units        = null;
        this.content      = null;
        this.properties   = null;
        this.pickupHooks  = null;
        this.spatialIndex = null;
        this.pathfinding  = null;

        this.targetUnits = new Unit[0];
    }

    /**
     * @return the number of direction steps of a full turn of the directions and headings of this system's units, clockwise from up
     */
    @Override
    public int getDirectionCount() {
        return this.motionModel.getDirectionCount();
    }


    /**
     * Resolves what this system uses from the game. Called once, after every system exists.
     * <p>
     * Binds the unit pool, the motion profile table, the {@link MovementSystem#TRANSPORT_PICKUP} hooks, the spatial index and the path finding
     * system if the game has them, and one followed-unit slot per pool slot.
     *
     * @param game the game
     */
    @Override
    public void attach(@NotNull Game game) {
        this.units        = game.getUnits();
        this.content      = game.getContent();
        this.properties   = new TableMotionProfileProperties(game.getContent().getMotionProfiles());
        this.pickupHooks  = game.getHooks(MovementSystem.TRANSPORT_PICKUP);
        this.spatialIndex = game.hasSystem(SpatialIndex.KEY) ? game.getSpatialIndex() : null;
        this.pathfinding  = game.hasSystem(PathfindingSystem.KEY) ? game.getPathfinding() : null;

        this.targetUnits = new Unit[game.getUnits().getCapacity() + 1];
    }

    /**
     * Sets the point a unit moves to; the motion starts in its next update.
     * <p>
     * A transport pickup hook may take the unit instead; otherwise the unit stops following, the destination, the waypoint and the direction
     * target become the point, the ordered flag is set, the braking flag cleared and the unit is no longer immovable.
     *
     * @param unit the unit
     * @param x    the x coordinate of the destination
     * @param y    the y coordinate of the destination
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public void setDestination(@NotNull Unit unit, int x, int y) {
        HookList<TransportPickupHook> hooks = requireAttached(this.pickupHooks);

        for (int index = 0; index < hooks.size(); index++) {
            if (hooks.get(index).requiresPickup(unit, x, y)) {
                return;
            }
        }

        this.targetUnits[unit.getIndex()] = null;

        this.moveTo(unit, x, y);
    }

    /**
     * Makes a unit follow another unit: the destination follows the target's position in each update.
     *
     * @param unit       the unit
     * @param targetUnit the unit to follow
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public void setTargetUnit(@NotNull Unit unit, @NotNull Unit targetUnit) {
        requireAttached(this.units);

        this.targetUnits[unit.getIndex()] = targetUnit;

        this.moveTo(unit, targetUnit.getPositionX(), targetUnit.getPositionY());
    }

    /**
     * Stops a unit where it is: the destination becomes its position and its speed becomes {@code 0}.
     * <p>
     * The unit stops following, its waypoint becomes its position, its next speed becomes {@code 0} too, it neither accelerates nor brakes nor
     * heads through a path waypoint, and its path is released.
     *
     * @param unit the unit
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public void stop(@NotNull Unit unit) {
        requireAttached(this.units);

        this.targetUnits[unit.getIndex()] = null;

        unit.setMovementDestinationX(unit.getPositionX());
        unit.setMovementDestinationY(unit.getPositionY());
        unit.setMovementWaypointX(unit.getPositionX());
        unit.setMovementWaypointY(unit.getPositionY());
        unit.setMovementFlags(unit.getMovementFlags() & ~(MovementFlags.ACCELERATING | MovementFlags.BRAKING | MovementFlags.MOVING));
        unit.setNextSpeed(0);
        this.motionModel.setSpeed(unit, 0);

        PathfindingSystem attachedPathfinding = this.pathfinding;

        if (attachedPathfinding != null) {
            attachedPathfinding.releasePath(unit);
        }
    }

    /**
     * Turns a unit towards a direction without moving it.
     * <p>
     * The direction becomes the target heading and the direction target a point ahead of the unit in that direction; the unit turns at its turn
     * rate while it stands on its waypoint.
     *
     * @param unit      the unit
     * @param direction the direction, {@code 0} up to the direction count less one ({@link #getDirectionCount})
     */
    @Override
    public void turnTo(@NotNull Unit unit, int direction) {
        unit.setTargetHeading(this.motionModel.normalizeDirection(direction));
        unit.setDirectionTargetX(unit.getPositionX() + this.motionModel.getVectorX(direction, TURN_TARGET_DISTANCE));
        unit.setDirectionTargetY(unit.getPositionY() + this.motionModel.getVectorY(direction, TURN_TARGET_DISTANCE));
        unit.setMovementFlags(unit.getMovementFlags() | MovementFlags.ORDERED);
    }

    /**
     * Checks whether a unit stands at its destination.
     *
     * @param unit the unit
     * @return {@code true} if the unit reached its destination and does not move, {@code false} otherwise
     */
    @Override
    public boolean isAtDestination(@NotNull Unit unit) {
        return this.motionModel.isAtMovementDestination(unit) && unit.getCurrentSpeed() == 0;
    }

    /**
     * Recalculates the speed, acceleration and turn rate of a unit after its type or its stat modifiers changed. A movement whose speed comes
     * from the unit's walking animation runs that program to measure it.
     * <p>
     * The standard movement takes the values of the unit's motion profile: the top speed and the acceleration after the owner's
     * {@link Stat#TOP_SPEED} and {@link Stat#ACCELERATION} modifiers, the turn rate as it is. A unit without a motion profile gets {@code 0} for all
     * of them; it runs no animation program.
     *
     * @param unit             the unit
     * @param instructionLimit the instruction limit of each animation program run the recalculation starts
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public void recalculateSpeed(@NotNull Unit unit, int instructionLimit) {
        GameContent attachedContent = requireAttached(this.content);
        MotionProfile motionProfile = unit.getMotionProfile();
        int topSpeed = 0;
        int acceleration = 0;
        int turnRate = 0;

        if (motionProfile != null) {
            MotionProfileTable motionProfiles = attachedContent.getMotionProfiles();
            Player owner = unit.getOwner();

            topSpeed     = motionProfiles.getTopSpeed(motionProfile);
            acceleration = motionProfiles.getAcceleration(motionProfile);
            turnRate     = motionProfiles.getTurnRate(motionProfile);

            if (owner != null) {
                topSpeed     = attachedContent.applyModifiers(Stat.TOP_SPEED, owner, unit.getType(), null, topSpeed);
                acceleration = attachedContent.applyModifiers(Stat.ACCELERATION, owner, unit.getType(), null, acceleration);
            }
        }

        unit.setTopSpeed(topSpeed);
        unit.setAcceleration(acceleration);
        unit.setTurnRate(turnRate);
    }

    /**
     * Runs this phase of the current frame.
     * <p>
     * Runs the motion step of every unit of the pool in slot order.
     *
     * @param game the game
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public void run(@NotNull Game game) {
        EntityPool<Unit> pool = requireAttached(this.units);
        MotionProfileProperties<MovingEntity> attachedProperties = requireAttached(this.properties);
        SlotList used = pool.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            this.update(pool.get(index), attachedProperties);
        }
    }

    /**
     * Mixes this part of the simulation state into the hash: the slot index of each unit's followed unit, {@code 0} for none, in slot order.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        for (Unit targetUnit : this.targetUnits) {
            hash.mix(targetUnit != null ? targetUnit.getIndex() : 0);
        }
    }

    /**
     * Writes this part of the simulation state: the number of unit slots and the followed unit of every slot.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInt(this.targetUnits.length);

        for (Unit targetUnit : this.targetUnits) {
            writer.writeEntity(targetUnit);
        }
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early, holds another number of slots or names no unit of the game
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readLength(this.targetUnits.length);

        for (int slot = 0; slot < this.targetUnits.length; slot++) {
            this.targetUnits[slot] = reader.readEntity(reader.getGame().getUnits());
        }
    }

    private void moveTo(@NotNull Unit unit, int x, int y) {
        unit.setMovementDestinationX(x);
        unit.setMovementDestinationY(y);
        unit.setMovementWaypointX(x);
        unit.setMovementWaypointY(y);
        unit.setDirectionTargetX(x);
        unit.setDirectionTargetY(y);
        unit.setMovementFlags((unit.getMovementFlags() | MovementFlags.ORDERED) & ~MovementFlags.BRAKING);
        unit.setImmovable(false);
    }

    private void update(@NotNull Unit unit, @NotNull MotionProfileProperties<MovingEntity> attachedProperties) {
        Unit targetUnit = this.targetUnits[unit.getIndex()];

        if (targetUnit != null) {
            unit.setMovementDestinationX(targetUnit.getPositionX());
            unit.setMovementDestinationY(targetUnit.getPositionY());
            unit.setMovementWaypointX(targetUnit.getPositionX());
            unit.setMovementWaypointY(targetUnit.getPositionY());
            unit.setDirectionTargetX(targetUnit.getPositionX());
            unit.setDirectionTargetY(targetUnit.getPositionY());
        }

        PathfindingSystem attachedPathfinding = this.pathfinding;

        if (attachedPathfinding != null && !this.isFlyer(unit)) {
            this.followPath(unit, attachedPathfinding);
        }

        if (unit.getFixedPositionX() >> MotionModel.FRACTION_BITS != unit.getPositionX() ||
            unit.getFixedPositionY() >> MotionModel.FRACTION_BITS != unit.getPositionY()) {
            unit.setFixedPositionX(unit.getPositionX() << MotionModel.FRACTION_BITS);
            unit.setFixedPositionY(unit.getPositionY() << MotionModel.FRACTION_BITS);
        }

        int previousX = unit.getPositionX();
        int previousY = unit.getPositionY();

        this.motionModel.update(this.step, unit, attachedProperties);
        this.step.commit(unit);
        this.motionModel.updateFacing(unit, attachedProperties);

        SpatialIndex index = this.spatialIndex;

        if (index != null && (unit.getPositionX() != previousX || unit.getPositionY() != previousY)) {
            index.relocate(unit, unit.getPositionX() - previousX, unit.getPositionY() - previousY);
        }
    }

    /**
     * Brings the path of a ground unit that is away from its destination up to date and heads the unit to the path's current waypoint; a unit
     * whose destination cannot be reached stops.
     */
    private void followPath(@NotNull Unit unit, @NotNull PathfindingSystem attachedPathfinding) {
        if (this.motionModel.isAtMovementDestination(unit)) {
            return;
        }

        int destinationX = unit.getMovementDestinationX();
        int destinationY = unit.getMovementDestinationY();

        if (!attachedPathfinding.updatePath(unit, destinationX, destinationY)) {
            this.stop(unit);

            return;
        }

        if (!attachedPathfinding.getWaypoint(unit, this.waypoint)) {
            return;
        }

        int waypointX = this.waypoint.getX();
        int waypointY = this.waypoint.getY();
        int flags = unit.getMovementFlags();

        unit.setMovementWaypointX(waypointX);
        unit.setMovementWaypointY(waypointY);
        unit.setDirectionTargetX(waypointX);
        unit.setDirectionTargetY(waypointY);
        unit.setMovementFlags(waypointX != destinationX || waypointY != destinationY ? flags | MovementFlags.MOVING : flags & ~MovementFlags.MOVING);
    }

    private boolean isFlyer(@NotNull Unit unit) {
        UnitType type = unit.getType();

        return type != null && requireAttached(this.content).getUnitTypes().hasTrait(type, UnitTrait.FLYER);
    }
}
