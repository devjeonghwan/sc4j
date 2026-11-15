package org.fogofwarlabs.sc4j.engine.entity.pool;

import org.jetbrains.annotations.NotNull;

/**
 * Holds the engine slot policies. Both start with the slots in ascending order, so a new pool allocates {@code 1, 2, 3,
 * ...}; they differ in where a freed slot goes.
 */
public enum StandardSlotPolicy implements SlotPolicy {
    /**
     * A freed slot is the next one allocated: the most recently freed slot is reused first, which keeps the used slots
     * dense. The engine default.
     */
    REUSE_RECENT {
        /**
         * Links a freed slot into the free list: before the first free slot.
         *
         * @param freeSlots the free list
         * @param index     the freed slot, not linked
         */
        @Override
        public void release(@NotNull SlotList freeSlots, int index) {
            freeSlots.addFirst(index);
        }
    },
    /**
     * A freed slot is allocated after every slot that is already free: the slot that has been free longest is reused
     * first, which delays the reuse of an index that stale references may still name.
     */
    REUSE_OLDEST {
        /**
         * Links a freed slot into the free list: after the last free slot.
         *
         * @param freeSlots the free list
         * @param index     the freed slot, not linked
         */
        @Override
        public void release(@NotNull SlotList freeSlots, int index) {
            freeSlots.addLast(index);
        }
    };

    /**
     * Links every slot of a new pool into the empty free list: ascending, {@code 1} first.
     *
     * @param freeSlots the empty free list
     * @param capacity  the pool capacity; the slots are {@code 1} up to it
     */
    @Override
    public void initialize(@NotNull SlotList freeSlots, int capacity) {
        for (int index = 1; index <= capacity; index++) {
            freeSlots.addLast(index);
        }
    }
}
