package org.fogofwarlabs.sc4j.engine.movement;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.hook.HookComposition;
import org.fogofwarlabs.sc4j.engine.hook.HookPoint;
import org.jetbrains.annotations.NotNull;

/**
 * Moves units: destinations, target following, stopping, turning and the speed of a unit. Orders and other systems ask it to move a unit; it
 * asks the path finding and collision systems how.
 * <p>
 * Units are engine {@link Unit}s: the units of the engine pool, or those of a mod that pools its units itself in a subclass of {@link Unit}.
 * Positions are pixels.
 * <p>
 * The motion step of a frame is not an operation of the interface: the frame phase that owns the unit loop runs the motion of its movement
 * implementation (an implementation may be that phase itself), so no other system advances a unit's motion.
 * <p>
 * Determinism contract: the motion step runs once per unit and running frame, in pool order, and draws random values only from the game
 * generator with the sources the implementation documents. The operations of the interface change only the moved unit's movement state and
 * allocate nothing; only {@link #recalculateSpeed} may run an animation program, bounded by its instruction limit, and draw what that program
 * draws.
 */
public interface MovementSystem extends GameSystem {
    /**
     * The key of the movement system.
     */
    @NotNull SystemKey<MovementSystem> KEY = SystemKey.of("movement", MovementSystem.class);
    /**
     * The point a destination is set at, before the unit moves by itself: a hook may arrange a transport pickup instead
     * ({@link HookComposition#FIRST_ANSWER}; the default is that the unit moves by itself).
     */
    @NotNull HookPoint<TransportPickupHook> TRANSPORT_PICKUP = HookPoint.of("movement.transport-pickup", TransportPickupHook.class,
                                                                            HookComposition.FIRST_ANSWER);

    /**
     * @return the number of direction steps of a full turn of the directions and headings of this system's units, clockwise from up
     */
    int getDirectionCount();


    /**
     * Sets the point a unit moves to; the motion starts in its next update.
     *
     * @param unit the unit
     * @param x    the x coordinate of the destination
     * @param y    the y coordinate of the destination
     */
    void setDestination(@NotNull Unit unit, int x, int y);

    /**
     * Makes a unit follow another unit: the destination follows the target's position in each update.
     *
     * @param unit       the unit
     * @param targetUnit the unit to follow
     */
    void setTargetUnit(@NotNull Unit unit, @NotNull Unit targetUnit);

    /**
     * Stops a unit where it is: the destination becomes its position and its speed becomes {@code 0}.
     *
     * @param unit the unit
     */
    void stop(@NotNull Unit unit);

    /**
     * Turns a unit towards a direction without moving it.
     *
     * @param unit      the unit
     * @param direction the direction, {@code 0} up to the direction count less one ({@link #getDirectionCount})
     */
    void turnTo(@NotNull Unit unit, int direction);

    /**
     * Checks whether a unit stands at its destination.
     *
     * @param unit the unit
     * @return {@code true} if the unit reached its destination and does not move, {@code false} otherwise
     */
    boolean isAtDestination(@NotNull Unit unit);

    /**
     * Recalculates the speed, acceleration and turn rate of a unit after its type or its stat modifiers changed. A movement whose speed comes
     * from the unit's walking animation runs that program to measure it.
     *
     * @param unit             the unit
     * @param instructionLimit the instruction limit of each animation program run the recalculation starts
     */
    void recalculateSpeed(@NotNull Unit unit, int instructionLimit);
}
