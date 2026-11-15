package org.fogofwarlabs.sc4j.engine.collision;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.hook.HookComposition;
import org.fogofwarlabs.sc4j.engine.hook.HookPoint;
import org.fogofwarlabs.sc4j.engine.math.MutablePoint;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps units apart: which units block each other, where a unit can be put down near a position, and which units others
 * cannot push. It uses the spatial index for the nearby units. Resolving overlaps while units move is part of the
 * movement implementation's motion step, not an operation other systems call.
 * <p>
 * Positions are pixels.
 * <p>
 * Determinism contract: the operations run at the point of the frame their caller runs, draw no random value and
 * allocate nothing; {@link #isBlocking} and {@link #findFreePosition} change no unit (a failed search may leave the
 * implementation's command error for the player's feedback). Candidate units are visited in spatial index order.
 */
public interface CollisionSystem extends GameSystem {
    /**
     * The key of the collision system.
     */
    @NotNull SystemKey<CollisionSystem> KEY = SystemKey.of("collision", CollisionSystem.class);
    /**
     * The point a movement blocking test asks once its general rule found that two units collide at all: a hook may decide that the other unit
     * blocks or passes ({@link HookComposition#FIRST_ANSWER}; the default is that it blocks).
     */
    @NotNull HookPoint<BlockingHook> BLOCKING = HookPoint.of("collision.blocking", BlockingHook.class, HookComposition.FIRST_ANSWER);
    /**
     * The point a placement test asks once its general rule found that two units collide at all, before its layer rule: a hook may decide that the
     * other unit blocks or does not block the unit being put down ({@link HookComposition#FIRST_ANSWER}; the default is that units on the same
     * layer block each other).
     */
    @NotNull HookPoint<PlacementExemptionHook> PLACEMENT_EXEMPTION = HookPoint.of("collision.placement-exemption", PlacementExemptionHook.class,
                                                                                  HookComposition.FIRST_ANSWER);

    /**
     * Checks whether one unit blocks the movement of another.
     *
     * @param unit      the moving unit
     * @param otherUnit the unit in the way
     * @return {@code true} if the other unit blocks the moving unit, {@code false} otherwise
     */
    boolean isBlocking(@NotNull Unit unit, @NotNull Unit otherUnit);

    /**
     * Finds a free position for a unit near a position, within the search area of the implementation: the requested
     * position itself if the unit can stand there, else the nearest free one the search meets.
     *
     * @param unit     the unit to put down
     * @param x        the x coordinate to search around
     * @param y        the y coordinate to search around
     * @param position receives the found position
     * @return {@code true} if a free position was found and written, {@code false} otherwise
     */
    boolean findFreePosition(@NotNull Unit unit, int x, int y, @NotNull MutablePoint position);

    /**
     * Makes a unit immovable for other units: pushing does not move it until it moves again by itself.
     *
     * @param unit the unit
     */
    void makeImmovable(@NotNull Unit unit);
}
