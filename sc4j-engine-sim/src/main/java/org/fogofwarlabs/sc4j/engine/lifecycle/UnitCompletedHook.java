package org.fogofwarlabs.sc4j.engine.lifecycle;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Hears that a unit is completed ({@link UnitLifecycle#UNIT_COMPLETED}).
 */
@FunctionalInterface
public interface UnitCompletedHook {
    /**
     * Called when a unit is completed.
     *
     * @param unit the completed unit
     */
    void onUnitCompleted(@NotNull Unit unit);
}
