package org.fogofwarlabs.sc4j.engine.spatial;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine default spatial index: sorted edges with reciprocal indices, rectangle and nearest queries, relocation and removal.
 */
final class SortedEdgeIndexTest {
    private static final int UNIT_CAPACITY = 3;
    private static final int HALF_SIZE = 4;
    private static final int UNIT_SIZE = 2 * HALF_SIZE + 1;

    /**
     * Reads square bounds of {@link #UNIT_SIZE} pixels around the engine unit position.
     */
    private static final @NotNull UnitBounds<Unit> SQUARE_BOUNDS = new UnitBounds<>() {
        @Override
        public int getLeft(@NotNull Unit unit) {
            return unit.getPositionX() - HALF_SIZE;
        }

        @Override
        public int getTop(@NotNull Unit unit) {
            return unit.getPositionY() - HALF_SIZE;
        }

        @Override
        public int getRight(@NotNull Unit unit) {
            return unit.getPositionX() + HALF_SIZE;
        }

        @Override
        public int getBottom(@NotNull Unit unit) {
            return unit.getPositionY() + HALF_SIZE;
        }

        @Override
        public int getWidth(@NotNull Unit unit) {
            return UNIT_SIZE;
        }

        @Override
        public int getHeight(@NotNull Unit unit) {
            return UNIT_SIZE;
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
    void insert_should_keep_both_axes_sorted_with_reciprocal_edge_indices() {
        Unit[] units = createUnits();
        SortedEdgeIndex<Unit> index = createIndex(units);

        place(index, units[0], 50, 10);
        place(index, units[1], 10, 30);
        place(index, units[2], 30, 20);

        Assertions.assertEquals(6, index.getCount());

        for (int edge = 1; edge < index.getCount(); edge++) {
            Assertions.assertTrue(index.getCoordinate(true, edge - 1) <= index.getCoordinate(true, edge));
            Assertions.assertTrue(index.getCoordinate(false, edge - 1) <= index.getCoordinate(false, edge));
        }

        for (int slot = 1; slot <= UNIT_CAPACITY; slot++) {
            Unit unit = units[slot - 1];

            Assertions.assertEquals(slot, index.getUnitIndex(true, unit.getMinimumHorizontalEdgeIndex()));
            Assertions.assertEquals(slot, index.getUnitIndex(true, unit.getMaximumHorizontalEdgeIndex()));
            Assertions.assertEquals(slot, index.getUnitIndex(false, unit.getMinimumVerticalEdgeIndex()));
            Assertions.assertEquals(slot, index.getUnitIndex(false, unit.getMaximumVerticalEdgeIndex()));
        }
    }

    @Test
    void rectangle_query_returns_overlapping_units_in_left_edge_order() {
        Unit[] units = createUnits();
        SortedEdgeIndex<Unit> index = createIndex(units);
        UnitList result = new UnitList(UNIT_CAPACITY);

        place(index, units[0], 50, 10);
        place(index, units[1], 10, 10);
        place(index, units[2], 100, 100);

        index.findInRectangle(0, 0, 60, 20, UnitFilter.ANY, result);

        Assertions.assertEquals(2, result.size());
        Assertions.assertSame(units[1], result.get(0));
        Assertions.assertSame(units[0], result.get(1));

        index.findInRectangle(0, 0, 60, 20, unit -> unit != units[1], result);

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(units[0], result.get(0));
    }

    @Test
    void relocate_and_remove_should_keep_queries_consistent() {
        Unit[] units = createUnits();
        SortedEdgeIndex<Unit> index = createIndex(units);
        UnitList result = new UnitList(UNIT_CAPACITY);

        place(index, units[0], 10, 10);
        place(index, units[1], 40, 10);

        units[0].setPosition(70, 10);
        index.relocate(units[0], 60, 0);
        index.findInRectangle(0, 0, 50, 20, UnitFilter.ANY, result);

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(units[1], result.get(0));
        Assertions.assertSame(units[1], index.findNearestAccepted(0, 10, 100, UnitFilter.ANY));

        index.remove(units[1]);

        Assertions.assertEquals(2, index.getCount());
        Assertions.assertEquals(-1, units[1].getMinimumHorizontalEdgeIndex());
        Assertions.assertSame(units[0], index.findNearestAccepted(0, 10, 100, UnitFilter.ANY));
        Assertions.assertNull(index.findNearestAccepted(0, 10, 20, UnitFilter.ANY));
    }

    private static @NotNull Unit @NotNull [] createUnits() {
        Unit[] units = new Unit[UNIT_CAPACITY];

        for (int slot = 1; slot <= UNIT_CAPACITY; slot++) {
            units[slot - 1] = new Unit(slot);
        }

        return units;
    }

    private static @NotNull SortedEdgeIndex<Unit> createIndex(@NotNull Unit @NotNull [] units) {
        SortedEdgeIndex<Unit> index = new SortedEdgeIndex<>(UNIT_CAPACITY, slot -> units[slot - 1], SQUARE_BOUNDS, new SearchCounter());

        index.setMinimumQueryDimensions(UNIT_SIZE, UNIT_SIZE);

        return index;
    }

    private static void place(@NotNull SortedEdgeIndex<Unit> index, @NotNull Unit unit, int x, int y) {
        unit.setPosition(x, y);
        index.insert(unit);
    }
}
