package org.fogofwarlabs.sc4j.engine.entity.pool;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnReset;
import org.fogofwarlabs.sc4j.engine.entity.column.IntColumn;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the entity pool lifecycle: slot order of both standard policies and of a mod policy, two-step allocation, both
 * generation rules, used-slot order, handles, clearing for a restore and the reset of extension columns.
 */
final class EntityPoolTest {
    private static final @NotNull ColumnKey<IntColumn> RESET_COLUMN = ColumnKey.ofInt("energy", 50, ColumnReset.ON_ALLOCATE);
    private static final @NotNull ColumnKey<IntColumn> KEPT_COLUMN = ColumnKey.ofInt("history", -1, ColumnReset.KEEP);

    /**
     * A policy that starts with the slots in descending order, unlinks a committed slot by moving the head past it,
     * reuses the most recently freed slot first and advances the generation by two when a slot is initialized.
     */
    private enum InitializeTimedPolicy implements SlotPolicy {
        INSTANCE;

        /**
         * @return when the pool advances the generation of a slot: {@link GenerationTiming#ON_INITIALIZE}
         */
        @Override
        public @NotNull GenerationTiming getGenerationTiming() {
            return GenerationTiming.ON_INITIALIZE;
        }

        /**
         * Returns the generation that follows a generation; the pool keeps the low {@code 16} bits of the result.
         *
         * @param generation the current generation
         * @return the next generation, two more
         */
        @Override
        public int advanceGeneration(int generation) {
            return generation + 2;
        }

        /**
         * Links every slot of a new pool into the empty free list: descending.
         *
         * @param freeSlots the empty free list
         * @param capacity  the pool capacity; the slots are {@code 1} up to it
         */
        @Override
        public void initialize(@NotNull SlotList freeSlots, int capacity) {
            for (int index = capacity; index >= 1; index--) {
                freeSlots.addLast(index);
            }
        }

        /**
         * Unlinks the first free slot when an allocation commits it: the head moves to the next slot.
         *
         * @param freeSlots the free list
         * @param index     the first free slot
         */
        @Override
        public void unlink(@NotNull SlotList freeSlots, int index) {
            int next = freeSlots.getNext(index);

            freeSlots.setFirst(next);

            if (next == SlotList.NONE) {
                freeSlots.setLast(SlotList.NONE);
            } else {
                freeSlots.setPrevious(next, SlotList.NONE);
            }

            freeSlots.setNext(index, SlotList.NONE);
        }

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
    }

    @Test
    void allocate_should_follow_the_reuse_recent_policy() {
        EntityPool<Unit> pool = createPool(4, StandardSlotPolicy.REUSE_RECENT);
        Unit first = pool.allocate();
        Unit second = pool.allocate();
        Unit third = pool.allocate();

        Assertions.assertNotNull(first);
        Assertions.assertNotNull(second);
        Assertions.assertNotNull(third);
        Assertions.assertEquals(List.of(1, 2, 3), List.of(first.getIndex(), second.getIndex(), third.getIndex()));

        pool.free(second);
        pool.free(first);

        Assertions.assertEquals(List.of(3), readUsed(pool));
        Assertions.assertEquals(List.of(1, 2, 4), readIndices(pool, 3));
        Assertions.assertEquals(List.of(3, 1, 2, 4), readUsed(pool));
        Assertions.assertNull(pool.allocate());
    }

    @Test
    void allocate_should_follow_the_reuse_oldest_policy() {
        EntityPool<Unit> pool = createPool(4, StandardSlotPolicy.REUSE_OLDEST);
        List<Integer> allocated = readIndices(pool, 3);

        Assertions.assertEquals(List.of(1, 2, 3), allocated);

        pool.free(pool.get(2));
        pool.free(pool.get(1));

        Assertions.assertEquals(List.of(4, 2, 1), readIndices(pool, 3));
        Assertions.assertEquals(4, pool.getAllocatedCount());
    }

    @Test
    void free_should_advance_the_generation_and_invalidate_handles() {
        EntityPool<Unit> pool = createPool(2, StandardSlotPolicy.REUSE_RECENT);
        Unit unit = pool.allocate();

        Assertions.assertNotNull(unit);

        int handle = pool.createHandle(unit);

        Assertions.assertSame(unit, pool.resolveHandle(handle));

        pool.free(unit);

        Assertions.assertEquals(1, pool.getGeneration(unit.getIndex()));
        Assertions.assertNull(pool.resolveHandle(handle));
        Assertions.assertThrows(IllegalStateException.class, () -> pool.free(unit));
        Assertions.assertSame(unit, pool.allocate());
        Assertions.assertNull(pool.resolveHandle(handle));
        Assertions.assertSame(unit, pool.resolveHandle(pool.createHandle(unit)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> pool.free(new Unit(1)));
    }

    @Test
    void allocate_should_reset_only_the_reset_columns() {
        EntityPool<Unit> pool = createPool(1, StandardSlotPolicy.REUSE_RECENT);
        IntColumn energy = pool.getColumn(RESET_COLUMN);
        IntColumn history = pool.getColumn(KEPT_COLUMN);
        Unit unit = pool.allocate();

        Assertions.assertNotNull(unit);
        Assertions.assertEquals(50, energy.get(unit.getIndex()));
        Assertions.assertEquals(-1, history.get(unit.getIndex()));

        energy.set(unit.getIndex(), 7);
        history.set(unit.getIndex(), 9);

        Player owner = new Player(2);

        unit.setOwner(owner);
        pool.free(unit);

        Assertions.assertSame(unit, pool.allocate());
        Assertions.assertEquals(50, energy.get(unit.getIndex()));
        Assertions.assertEquals(9, history.get(unit.getIndex()));
        Assertions.assertSame(owner, unit.getOwner());
    }

    @Test
    void state_hash_should_depend_on_slots_entities_and_columns() {
        EntityPool<Unit> first = createPool(3, StandardSlotPolicy.REUSE_RECENT);
        EntityPool<Unit> second = createPool(3, StandardSlotPolicy.REUSE_RECENT);

        Assertions.assertEquals(computeHash(first), computeHash(second));

        Unit unit = first.allocate();

        Assertions.assertNotNull(unit);
        Assertions.assertNotEquals(computeHash(first), computeHash(second));

        Unit other = second.allocate();

        Assertions.assertNotNull(other);
        Assertions.assertEquals(computeHash(first), computeHash(second));

        first.getColumn(KEPT_COLUMN).set(unit.getIndex(), 3);

        Assertions.assertNotEquals(computeHash(first), computeHash(second));
    }

    @Test
    void commit_should_follow_the_policy_and_its_generation_rule() {
        EntityPool<Unit> pool = createPool(3, InitializeTimedPolicy.INSTANCE);
        Unit first = pool.getFirstFree();

        Assertions.assertNotNull(first);
        Assertions.assertEquals(3, first.getIndex());
        Assertions.assertThrows(IllegalStateException.class, () -> pool.advanceGeneration(pool.get(1)));
        Assertions.assertThrows(IllegalStateException.class, () -> pool.commit(pool.get(1)));

        pool.advanceGeneration(first);

        Assertions.assertEquals(2, pool.getGeneration(first.getIndex()));
        Assertions.assertFalse(pool.isAllocated(first.getIndex()));
        Assertions.assertSame(first, pool.getFirstFree());

        pool.commit(first);

        Assertions.assertTrue(pool.isAllocated(first.getIndex()));
        Assertions.assertEquals(SlotList.NONE, pool.getFreeSlots().getNext(first.getIndex()));
        Assertions.assertThrows(IllegalStateException.class, () -> pool.commit(first));

        pool.free(first);

        Assertions.assertEquals(2, pool.getGeneration(first.getIndex()));
        Assertions.assertSame(first, pool.allocate());
        Assertions.assertEquals(4, pool.getGeneration(first.getIndex()));
        Assertions.assertEquals(List.of(2, 1), readIndices(pool, 2));
        Assertions.assertNull(pool.getFirstFree());
        Assertions.assertNull(pool.allocate());
    }

    @Test
    void advance_generation_should_require_the_initialize_timing() {
        EntityPool<Unit> pool = createPool(1, StandardSlotPolicy.REUSE_RECENT);
        Unit unit = pool.getFirstFree();

        Assertions.assertNotNull(unit);
        Assertions.assertThrows(IllegalStateException.class, () -> pool.advanceGeneration(unit));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> pool.get(0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> pool.get(2));
    }

    @Test
    void clear_should_free_every_slot_for_a_restore() {
        EntityPool<Unit> pool = createPool(3, StandardSlotPolicy.REUSE_RECENT);

        readIndices(pool, 2);
        pool.free(pool.get(1));
        pool.clear();

        Assertions.assertEquals(0, pool.getAllocatedCount());
        Assertions.assertEquals(0, pool.getGeneration(1));
        Assertions.assertNull(pool.getFirstFree());
        Assertions.assertTrue(pool.getUsedSlots().isEmpty());

        pool.restoreAllocated(3);
        pool.restoreAllocated(1);
        pool.getFreeSlots().addLast(2);

        Assertions.assertThrows(IllegalStateException.class, () -> pool.restoreAllocated(3));
        Assertions.assertEquals(List.of(3, 1), readUsed(pool));
        Assertions.assertEquals(List.of(2), readIndices(pool, 1));
    }

    @Test
    void constructor_should_reject_an_invalid_capacity() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> createPool(-1, StandardSlotPolicy.REUSE_RECENT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> createPool(EntityPool.MAXIMUM_CAPACITY + 1, StandardSlotPolicy.REUSE_RECENT));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new EntityPool<>(2, index -> new Unit(1), StandardSlotPolicy.REUSE_RECENT, List.of()));
    }

    private static @NotNull EntityPool<Unit> createPool(int capacity, @NotNull SlotPolicy policy) {
        return new EntityPool<>(capacity, Unit::new, policy, List.of(RESET_COLUMN, KEPT_COLUMN));
    }

    private static @NotNull List<Integer> readIndices(@NotNull EntityPool<Unit> pool, int count) {
        List<Integer> indices = new ArrayList<>();

        for (int allocation = 0; allocation < count; allocation++) {
            Unit unit = pool.allocate();

            Assertions.assertNotNull(unit);
            indices.add(unit.getIndex());
        }

        return indices;
    }

    private static @NotNull List<Integer> readUsed(@NotNull EntityPool<Unit> pool) {
        List<Integer> indices = new ArrayList<>();
        SlotList used = pool.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            indices.add(index);
        }

        return indices;
    }

    private static long computeHash(@NotNull EntityPool<Unit> pool) {
        StateHash hash = new StateHash();

        pool.contributeStateHash(hash);

        return hash.getValue();
    }
}
