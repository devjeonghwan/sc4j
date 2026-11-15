package org.fogofwarlabs.sc4j.engine.entity.pool;

import org.jetbrains.annotations.NotNull;

/**
 * Decides the order of the free slots of a pool and its generation rule: an allocation always takes the first free slot,
 * so the policy decides which slot that is by where it links and unlinks the slots, and it decides when and to which
 * value a slot's generation advances.
 * <p>
 * Determinism contract: a policy depends only on its arguments and the list; it draws no random value and keeps no
 * state of its own. The pool calls {@link #initialize} once, when it is created, {@link #unlink} once per committed
 * allocation, before the slot is marked allocated, and {@link #release} once per freed slot, after the slot was unlinked
 * from the used slots.
 */
public interface SlotPolicy {
    /**
     * @return when the pool advances the generation of a slot; {@link GenerationTiming#ON_FREE} by default
     */
    default @NotNull GenerationTiming getGenerationTiming() {
        return GenerationTiming.ON_FREE;
    }

    /**
     * Returns the generation that follows a generation; the pool keeps the low {@code 16} bits of the result. By default
     * the generation counts up by one.
     *
     * @param generation the current generation
     * @return the next generation
     */
    default int advanceGeneration(int generation) {
        return generation + 1;
    }


    /**
     * Links every slot of a new pool into the empty free list.
     *
     * @param freeSlots the empty free list
     * @param capacity  the pool capacity; the slots are {@code 1} up to it
     */
    void initialize(@NotNull SlotList freeSlots, int capacity);

    /**
     * Unlinks the first free slot when an allocation commits it. By default the slot is removed from the list and its
     * neighbors are linked to each other.
     *
     * @param freeSlots the free list
     * @param index     the first free slot
     */
    default void unlink(@NotNull SlotList freeSlots, int index) {
        freeSlots.remove(index);
    }

    /**
     * Links a freed slot into the free list.
     *
     * @param freeSlots the free list
     * @param index     the freed slot, not linked
     */
    void release(@NotNull SlotList freeSlots, int index);
}
