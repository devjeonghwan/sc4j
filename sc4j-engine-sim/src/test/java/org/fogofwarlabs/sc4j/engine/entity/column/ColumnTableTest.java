package org.fogofwarlabs.sc4j.engine.entity.column;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the column table: typed lookup, registration order, defaults and duplicate keys.
 */
final class ColumnTableTest {
    private static final @NotNull ColumnKey<IntColumn> COUNT = ColumnKey.ofInt("count", 4, ColumnReset.ON_ALLOCATE);
    private static final @NotNull ColumnKey<ByteColumn> FLAGS = ColumnKey.ofByte("flags", (byte) 0x11, ColumnReset.KEEP);
    private static final @NotNull ColumnKey<BooleanColumn> MARKED = ColumnKey.ofBoolean("marked", true, ColumnReset.ON_ALLOCATE);

    @Test
    void get_should_return_the_typed_column_with_defaults() {
        ColumnTable table = new ColumnTable(List.of(COUNT, FLAGS, MARKED), 3);

        Assertions.assertEquals(3, table.getCount());
        Assertions.assertSame(FLAGS, table.getKey(1));
        Assertions.assertEquals(3, table.get(COUNT).getLength());
        Assertions.assertEquals(4, table.get(COUNT).get(2));
        Assertions.assertEquals((byte) 0x11, table.get(FLAGS).get(2));
        Assertions.assertTrue(table.get(MARKED).get(2));
        Assertions.assertTrue(table.contains(MARKED));
    }

    @Test
    void reset_slot_should_reset_only_the_reset_columns() {
        ColumnTable table = new ColumnTable(List.of(COUNT, FLAGS, MARKED), 3);

        table.get(COUNT).set(1, 9);
        table.get(FLAGS).set(1, (byte) 2);
        table.get(MARKED).set(1, false);
        table.resetSlot(1);

        Assertions.assertEquals(4, table.get(COUNT).get(1));
        Assertions.assertEquals((byte) 2, table.get(FLAGS).get(1));
        Assertions.assertTrue(table.get(MARKED).get(1));
    }

    @Test
    void get_write_count_should_count_every_set_and_reset_of_every_column() {
        ColumnTable table = new ColumnTable(List.of(COUNT, FLAGS, MARKED), 3);

        Assertions.assertEquals(0L, table.getWriteCount());

        table.get(COUNT).set(1, 4);
        table.get(FLAGS).set(0, (byte) 1);
        table.get(MARKED).set(2, false);
        table.get(COUNT).reset(1);

        Assertions.assertEquals(2L, table.get(COUNT).getWriteCount());
        Assertions.assertEquals(1L, table.get(MARKED).getWriteCount());
        Assertions.assertEquals(4L, table.getWriteCount());

        table.resetSlot(0);

        Assertions.assertEquals(6L, table.getWriteCount());
    }

    @Test
    void constructor_should_reject_a_key_listed_twice_and_get_an_unknown_key() {
        ColumnTable table = new ColumnTable(List.of(COUNT), 2);

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ColumnTable(List.of(COUNT, COUNT), 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> table.get(FLAGS));
        Assertions.assertFalse(table.contains(FLAGS));
    }
}
