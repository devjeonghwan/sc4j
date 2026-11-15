package org.fogofwarlabs.sc4j.engine.lifecycle;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Hears that the owner change of a unit is finished ({@link UnitLifecycle#UNIT_OWNER_CHANGED}).
 */
@FunctionalInterface
public interface UnitOwnerChangedHook {
    /**
     * Called when the owner change of a unit is finished.
     *
     * @param unit             the unit with its new owner
     * @param instructionLimit the instruction limit of each animation program run the hook starts
     */
    void onUnitOwnerChanged(@NotNull Unit unit, int instructionLimit);
}
