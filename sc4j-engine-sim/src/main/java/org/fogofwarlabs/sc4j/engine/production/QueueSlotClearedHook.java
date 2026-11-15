package org.fogofwarlabs.sc4j.engine.production;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Hears that a queue slot of a producer is emptied ({@link ProductionSystem#QUEUE_SLOT_CLEARED}).
 */
@FunctionalInterface
public interface QueueSlotClearedHook {
    /**
     * Called when a queue slot becomes empty.
     *
     * @param producer the producing unit
     * @param slot     the emptied slot
     */
    void onQueueSlotCleared(@NotNull Unit producer, int slot);
}
