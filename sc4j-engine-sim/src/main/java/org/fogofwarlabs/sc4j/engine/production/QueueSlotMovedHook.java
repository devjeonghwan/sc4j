package org.fogofwarlabs.sc4j.engine.production;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Hears that a queue entry of a producer moves from one slot to another ({@link ProductionSystem#QUEUE_SLOT_MOVED}).
 */
@FunctionalInterface
public interface QueueSlotMovedHook {
    /**
     * Called when a queue entry moves.
     *
     * @param producer   the producing unit
     * @param slot       the slot that receives the entry
     * @param sourceSlot the slot the entry leaves
     */
    void onQueueSlotMoved(@NotNull Unit producer, int slot, int sourceSlot);
}
