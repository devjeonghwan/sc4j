package org.fogofwarlabs.sc4j.engine.lifecycle;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Hears that a unit leaves its owner, by death or by an owner change ({@link UnitLifecycle#UNIT_REMOVED}).
 */
@FunctionalInterface
public interface UnitRemovedHook {
    /**
     * Called when a unit leaves its owner.
     *
     * @param unit  the unit
     * @param death {@code true} if the unit dies, {@code false} if it changes owner
     */
    void onUnitRemoved(@NotNull Unit unit, boolean death);
}
