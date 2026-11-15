package org.fogofwarlabs.sc4j.engine.visibility;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Decides what each player sees: sight of units over the map, explored and visible areas, cloaking and detection. Combat,
 * orders, computer players and the client ask it whether a player sees a point or a unit.
 * <p>
 * Positions are pixels.
 * <p>
 * Determinism contract: sight is updated at the points of the frame the mod's phases call {@link #updateSight} (for
 * example after a unit moved, or every n-th frame for the whole map); the queries read the state of the last update,
 * change nothing, draw no random value and allocate nothing. Players are visited in ascending slot order.
 */
public interface VisibilitySystem extends GameSystem {
    /**
     * The key of the visibility system.
     */
    @NotNull SystemKey<VisibilitySystem> KEY = SystemKey.of("visibility", VisibilitySystem.class);

    /**
     * Checks whether a player currently sees a point.
     *
     * @param player the player
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @return {@code true} if the point is in the player's sight, {@code false} otherwise
     */
    boolean isVisible(@NotNull Player player, int x, int y);

    /**
     * Checks whether a player has ever seen a point.
     *
     * @param player the player
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @return {@code true} if the point is explored by the player, {@code false} otherwise
     */
    boolean isExplored(@NotNull Player player, int x, int y);

    /**
     * Checks whether a player sees a unit: the unit is in the player's sight and, if it is cloaked, detected by the player.
     *
     * @param player the player
     * @param unit   the unit
     * @return {@code true} if the player sees the unit, {@code false} otherwise
     */
    boolean isUnitVisible(@NotNull Player player, @NotNull Unit unit);

    /**
     * Checks whether a player detects a cloaked or burrowed unit.
     *
     * @param player the player
     * @param unit   the unit
     * @return {@code true} if the unit is not hidden from the player, {@code false} otherwise
     */
    boolean isDetected(@NotNull Player player, @NotNull Unit unit);

    /**
     * Returns the current sight range of a unit, after its stat modifiers.
     *
     * @param unit the unit
     * @return the sight range in pixels
     */
    int getSightRange(@NotNull Unit unit);

    /**
     * Updates the sight a unit gives its owner after it moved, changed type or changed its sight range.
     *
     * @param unit the unit
     */
    void updateSight(@NotNull Unit unit);

    /**
     * Sets whether a unit is cloaked: hidden from the players that do not detect it.
     *
     * @param unit    the unit
     * @param cloaked whether the unit is cloaked
     */
    void setCloaked(@NotNull Unit unit, boolean cloaked);
}
