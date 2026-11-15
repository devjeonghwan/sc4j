package org.fogofwarlabs.sc4j.engine.lifecycle;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Hears that a building is destroyed ({@link UnitLifecycle#BUILDING_DESTROYED}).
 */
@FunctionalInterface
public interface BuildingDestroyedHook {
    /**
     * Called when a building dies.
     *
     * @param unit the destroyed building
     */
    void onBuildingDestroyed(@NotNull Unit unit);
}
