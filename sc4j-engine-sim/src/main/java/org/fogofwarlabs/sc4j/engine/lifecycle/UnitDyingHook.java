package org.fogofwarlabs.sc4j.engine.lifecycle;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Hears that a unit dies, at the point of its death where the unit leaves the systems it provides for ({@link UnitLifecycle#UNIT_DYING}).
 */
@FunctionalInterface
public interface UnitDyingHook {
    /**
     * Called while a unit dies.
     *
     * @param unit the dying unit
     */
    void onUnitDying(@NotNull Unit unit);
}
