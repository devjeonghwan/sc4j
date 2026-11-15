package org.fogofwarlabs.sc4j.engine.collision;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.spatial.SearchCounter;
import org.fogofwarlabs.sc4j.engine.spatial.SortedEdgeIndex;
import org.fogofwarlabs.sc4j.engine.spatial.UnitBounds;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the blocker searches over the engine spatial index: the step search, the largest overlapping blocker and the first placement blocker.
 */
final class UnitCollisionSearchTest {
    private static final int UNIT_CAPACITY = 3;
    private static final int SMALL_HALF_SIZE = 4;
    private static final int LARGE_HALF_SIZE = 8;

    /**
     * Reads square bounds around the engine unit position: the first unit is large, the others small.
     */
    private static final @NotNull UnitBounds<Unit> SQUARE_BOUNDS = new UnitBounds<>() {
        @Override
        public int getLeft(@NotNull Unit unit) {
            return unit.getPositionX() - getHalfSize(unit);
        }

        @Override
        public int getTop(@NotNull Unit unit) {
            return unit.getPositionY() - getHalfSize(unit);
        }

        @Override
        public int getRight(@NotNull Unit unit) {
            return unit.getPositionX() + getHalfSize(unit);
        }

        @Override
        public int getBottom(@NotNull Unit unit) {
            return unit.getPositionY() + getHalfSize(unit);
        }

        @Override
        public int getWidth(@NotNull Unit unit) {
            return 2 * getHalfSize(unit) + 1;
        }

        @Override
        public int getHeight(@NotNull Unit unit) {
            return 2 * getHalfSize(unit) + 1;
        }

        @Override
        public int getCenterX(@NotNull Unit unit) {
            return unit.getPositionX();
        }

        @Override
        public int getCenterY(@NotNull Unit unit) {
            return unit.getPositionY();
        }

        @Override
        public boolean isPlaced(@NotNull Unit unit) {
            return true;
        }
    };

    @Test
    void find_step_blocker_should_return_the_first_accepted_unit_in_the_step_direction() {
        Unit[] units = createUnits();
        SortedEdgeIndex<Unit> index = createIndex(units);
        UnitCollisionSearch<Unit> search = new UnitCollisionSearch<>(index, SQUARE_BOUNDS);

        place(index, units[0], 100, 10);
        place(index, units[1], 10, 10);
        place(index, units[2], 30, 10);

        Assertions.assertSame(units[2], search.findStepBlocker(units[1], 12, 0, (unit, otherUnit) -> true));
        Assertions.assertNull(search.findStepBlocker(units[1], 4, 0, (unit, otherUnit) -> true));
        Assertions.assertNull(search.findStepBlocker(units[1], 12, 0, (unit, otherUnit) -> otherUnit != units[2]));
        Assertions.assertNull(search.findStepBlocker(units[1], 0, 10, (unit, otherUnit) -> true));
    }

    @Test
    void rectangle_searches_return_the_largest_and_the_first_blocker() {
        Unit[] units = createUnits();
        SortedEdgeIndex<Unit> index = createIndex(units);
        UnitCollisionSearch<Unit> search = new UnitCollisionSearch<>(index, SQUARE_BOUNDS);

        place(index, units[0], 30, 10);
        place(index, units[1], 10, 10);
        place(index, units[2], 100, 100);

        Assertions.assertSame(units[0], search.findLargestBlocker(units[2], 0, 0, 40, 20, (unit, otherUnit) -> true));
        Assertions.assertSame(units[1], search.findFirstBlocker(units[2], 0, 0, 40, 20, (unit, otherUnit) -> true));
        Assertions.assertSame(units[1], search.findLargestBlocker(units[2], 0, 0, 40, 20, (unit, otherUnit) -> otherUnit != units[0]));
        Assertions.assertNull(search.findFirstBlocker(units[2], 200, 200, 210, 210, (unit, otherUnit) -> true));
    }

    private static int getHalfSize(@NotNull Unit unit) {
        return unit.getIndex() == 1 ? LARGE_HALF_SIZE : SMALL_HALF_SIZE;
    }

    private static Unit @NotNull [] createUnits() {
        Unit[] units = new Unit[UNIT_CAPACITY];

        for (int slot = 1; slot <= UNIT_CAPACITY; slot++) {
            units[slot - 1] = new Unit(slot);
        }

        return units;
    }

    private static @NotNull SortedEdgeIndex<Unit> createIndex(@NotNull Unit @NotNull [] units) {
        SortedEdgeIndex<Unit> index = new SortedEdgeIndex<>(UNIT_CAPACITY, slot -> units[slot - 1], SQUARE_BOUNDS, new SearchCounter());
        int largestSize = 2 * LARGE_HALF_SIZE + 1;

        index.setMinimumQueryDimensions(largestSize, largestSize);

        return index;
    }

    private static void place(@NotNull SortedEdgeIndex<Unit> index, @NotNull Unit unit, int x, int y) {
        unit.setPosition(x, y);
        index.insert(unit);
    }
}
