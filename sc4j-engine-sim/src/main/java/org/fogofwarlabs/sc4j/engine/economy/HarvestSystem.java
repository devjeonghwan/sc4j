package org.fogofwarlabs.sc4j.engine.economy;

import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the resources on the map and in the hands of workers: what a resource container ({@link UnitTrait#RESOURCE_CONTAINER}) holds and what a
 * worker carries. The gather and return orders of workers use it; the map setup fills the containers; computer players ask it where the
 * resources are.
 * <p>
 * Positions are pixels.
 * <p>
 * Determinism contract: the queries read the game state only, draw no random value and allocate nothing; a search visits the units in slot
 * order.
 */
public interface HarvestSystem extends GameSystem {
    /**
     * The key of the harvest system.
     */
    @NotNull SystemKey<HarvestSystem> KEY = SystemKey.of("harvest", HarvestSystem.class);

    /**
     * Fills a resource container.
     *
     * @param container the container unit
     * @param kind      the kind of resource it holds
     * @param amount    the amount it holds, at least {@code 0}
     */
    void setResources(@NotNull Unit container, @NotNull ResourceKind kind, int amount);

    /**
     * Returns the kind of resource a container holds.
     *
     * @param container the container unit
     * @return the kind, or {@code null} if the unit holds no resources
     */
    @Nullable ResourceKind getResourceKind(@NotNull Unit container);

    /**
     * Returns the amount a container holds.
     *
     * @param container the container unit
     * @return the amount, {@code 0} if the unit holds no resources
     */
    int getResourceAmount(@NotNull Unit container);

    /**
     * Returns the kind of resource a worker carries.
     *
     * @param worker the worker unit
     * @return the kind, or {@code null} if the worker carries nothing
     */
    @Nullable ResourceKind getCarriedKind(@NotNull Unit worker);

    /**
     * Returns the amount a worker carries.
     *
     * @param worker the worker unit
     * @return the amount, {@code 0} if the worker carries nothing
     */
    int getCarriedAmount(@NotNull Unit worker);

    /**
     * Finds the resource container nearest to a point that holds resources.
     *
     * @param kind  the kind of resource, or {@code null} for any
     * @param x     the x coordinate of the point
     * @param y     the y coordinate of the point
     * @param range the largest distance in pixels from the point
     * @return the container, the first in slot order among equally near ones, or {@code null} if none is within the range
     */
    @Nullable Unit findResourceContainer(@Nullable ResourceKind kind, int x, int y, int range);
}
