package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Hears that the order timer of a unit runs its timed order ({@link OrderSystem#ORDER_TIMER}).
 */
@FunctionalInterface
public interface OrderTimerHook {
    /**
     * Called before the timed order of a unit runs.
     *
     * @param unit the unit
     */
    void onOrderTimer(@NotNull Unit unit);
}
