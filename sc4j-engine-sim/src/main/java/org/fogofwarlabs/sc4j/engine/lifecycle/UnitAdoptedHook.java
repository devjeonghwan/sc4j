package org.fogofwarlabs.sc4j.engine.lifecycle;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Hears that a unit has a new owner ({@link UnitLifecycle#UNIT_ADOPTED}).
 */
@FunctionalInterface
public interface UnitAdoptedHook {
    /**
     * Called when a unit has its new owner.
     *
     * @param unit             the unit with its new owner
     * @param instructionLimit the instruction limit of each animation program run the hook starts
     */
    void onUnitAdopted(@NotNull Unit unit, int instructionLimit);
}
