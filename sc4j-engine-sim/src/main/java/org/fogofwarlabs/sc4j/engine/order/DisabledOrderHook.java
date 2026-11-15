package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Hears that a disabled unit, or an immobile unit that cannot attack, runs its main order ({@link OrderSystem#DISABLED_ORDER}).
 */
@FunctionalInterface
public interface DisabledOrderHook {
    /**
     * Called when a disabled or immobile unarmed unit runs its main order.
     *
     * @param unit the unit
     */
    void onDisabledOrder(@NotNull Unit unit);
}
