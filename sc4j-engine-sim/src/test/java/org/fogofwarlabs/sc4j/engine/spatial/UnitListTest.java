package org.fogofwarlabs.sc4j.engine.spatial;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the unit list of the spatial queries: only the positions below the size hold collected units, whatever the slots past it hold.
 */
final class UnitListTest {
    private static final int CAPACITY = 3;

    @Test
    void get_should_return_collected_units_only_below_the_size() {
        UnitList list = new UnitList(CAPACITY);
        Unit first = new Unit(1);
        Unit second = new Unit(2);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));

        list.add(first);
        list.add(second);

        Assertions.assertSame(first, list.get(0));
        Assertions.assertSame(second, list.get(1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));

        list.clear();

        Assertions.assertTrue(list.isEmpty());
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
    }
}
