package org.fogofwarlabs.sc4j.engine.path;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.math.MutablePoint;
import org.jetbrains.annotations.NotNull;

/**
 * Finds paths for units over the terrain and holds each unit's current path. The movement system brings a unit's path
 * up to date and walks its waypoints; orders and computer players ask whether a point is reachable. The search data
 * (regions, contours, the path pool) stays inside the implementation.
 * <p>
 * Positions are pixels.
 * <p>
 * Determinism contract: a path update depends only on the game state and the unit; it draws no random value and takes
 * path storage from the implementation's pool without allocating. The queries change nothing and allocate nothing.
 */
public interface PathfindingSystem extends GameSystem {
    /**
     * The key of the path finding system.
     */
    @NotNull SystemKey<PathfindingSystem> KEY = SystemKey.of("pathfinding", PathfindingSystem.class);

    /**
     * Brings the path of a unit to a destination up to date: a unit that has not reached its current waypoint keeps it,
     * a unit at its waypoint advances to the next one, and a unit without a usable path gets a new one. The current
     * waypoint afterwards is read with {@link #getWaypoint}.
     *
     * @param unit    the unit
     * @param targetX the x coordinate of the destination
     * @param targetY the y coordinate of the destination
     * @return {@code true} if the unit has a path afterwards, {@code false} if the destination cannot be reached
     */
    boolean updatePath(@NotNull Unit unit, int targetX, int targetY);

    /**
     * Checks whether a unit can reach a point over the terrain.
     *
     * @param unit    the unit
     * @param targetX the x coordinate of the point
     * @param targetY the y coordinate of the point
     * @return {@code true} if the point is reachable for the unit, {@code false} otherwise
     */
    boolean isReachable(@NotNull Unit unit, int targetX, int targetY);

    /**
     * Reads the current waypoint of a unit's path.
     *
     * @param unit     the unit
     * @param waypoint receives the waypoint
     * @return {@code true} if the unit has a path and the waypoint was written, {@code false} otherwise
     */
    boolean getWaypoint(@NotNull Unit unit, @NotNull MutablePoint waypoint);

    /**
     * Releases the path of a unit, for example when it stops or dies.
     *
     * @param unit the unit
     */
    void releasePath(@NotNull Unit unit);
}
