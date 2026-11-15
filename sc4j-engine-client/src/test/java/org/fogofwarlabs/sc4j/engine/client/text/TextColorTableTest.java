package org.fogofwarlabs.sc4j.engine.client.text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the default colour code table: fixed colours, the restore and line colour codes, and characters that are not codes.
 */
final class TextColorTableTest {
    private static final int RED_CODE = 0x03;
    private static final int RESTORE_CODE = 0x01;
    private static final int LINE_COLOR_CODE = 0x05;
    private static final int UNSET_CODE = 0x02;
    private static final int RED = 0xFF0000FF;
    private static final int LINE = 0x101010FF;
    private static final int CURRENT = 0x202020FF;
    private static final int PREVIOUS = 0x303030FF;

    @Test
    void table_codes_set_fixed_previous_or_line_colors() {
        TextColorTable table = TextColorTable.builder()
                                             .setColor(RED_CODE, RED)
                                             .setRestoreCode(RESTORE_CODE)
                                             .setLineColorCode(LINE_COLOR_CODE)
                                             .build();

        Assertions.assertEquals(RED, table.getColor(RED_CODE, LINE, CURRENT, PREVIOUS));
        Assertions.assertEquals(PREVIOUS, table.getColor(RESTORE_CODE, LINE, CURRENT, PREVIOUS));
        Assertions.assertEquals(LINE, table.getColor(LINE_COLOR_CODE, LINE, CURRENT, PREVIOUS));
    }

    @Test
    void is_color_code_should_accept_only_the_codes_of_the_table() {
        TextColorTable table = TextColorTable.builder().setColor(RED_CODE, RED).build();

        Assertions.assertTrue(table.isColorCode(RED_CODE));
        Assertions.assertFalse(table.isColorCode(UNSET_CODE));
        Assertions.assertFalse(table.isColorCode('A'));
        Assertions.assertFalse(table.isColorCode(-1));
    }

    @Test
    void set_color_should_refuse_a_negative_code() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> TextColorTable.builder().setColor(-1, RED));
    }
}
