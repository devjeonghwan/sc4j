package org.fogofwarlabs.sc4j.engine.movement;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Decides that a transport picks a unit up instead of the unit moving to a point by itself ({@link MovementSystem#TRANSPORT_PICKUP}).
 */
@FunctionalInterface
public interface TransportPickupHook {
    /**
     * Checks whether a transport picks a unit up for a destination; a hook that answers {@code true} has arranged the pickup.
     *
     * @param unit the unit about to move
     * @param x    the x coordinate of the destination
     * @param y    the y coordinate of the destination
     * @return {@code true} if a transport picks the unit up and the unit does not move by itself, {@code false} if this hook gives no answer
     */
    boolean requiresPickup(@NotNull Unit unit, int x, int y);
}
