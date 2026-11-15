package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the ordered selection and the control groups that store it: units are kept once, in order and within the selection capacity.
 */
final class OrderedSelectionTest {
    private static final int GROUPS = 10;

    @Test
    void selection_and_control_groups_keep_units_once_within_capacity() {
        OrderedSelection selection = new OrderedSelection(2);
        ControlGroups groups = new ControlGroups(GROUPS);
        Unit first = new Unit(1);
        Unit second = new Unit(2);
        Unit third = new Unit(3);

        selection.select(List.of(first, first, second, third));

        Assertions.assertEquals(List.of(first, second), selection.getUnits());
        Assertions.assertTrue(selection.isFull());
        Assertions.assertFalse(selection.add(third));
        Assertions.assertFalse(selection.toggle(first));
        Assertions.assertTrue(selection.toggle(third));

        groups.assign(1, selection);
        selection.select(first);
        groups.add(1, selection);

        Assertions.assertEquals(List.of(second, third, first), groups.getUnits(1));
        Assertions.assertFalse(groups.recall(2, selection), "an empty group keeps the selection");
        Assertions.assertTrue(groups.recall(1, selection));
        Assertions.assertEquals(List.of(second, third), selection.getUnits(), "the recall stops at the capacity");

        groups.retain(unit -> unit != third);
        selection.retain(unit -> unit != second);

        Assertions.assertEquals(List.of(second, first), groups.getUnits(1));
        Assertions.assertEquals(List.of(third), selection.getUnits());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new OrderedSelection(0));
    }
}
