package org.fogofwarlabs.sc4j.engine.client.host;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the logical width of the aspect range of a logical screen.
 */
final class AspectRangeTest {
    private static final int LOGICAL_HEIGHT = 480;
    private static final @NotNull AspectRange RANGE = new AspectRange(LOGICAL_HEIGHT, 4.0f / 3, 16.0f / 9);

    @Test
    void getters_should_return_the_constructor_values() {
        Assertions.assertEquals(LOGICAL_HEIGHT, RANGE.getLogicalHeight());
        Assertions.assertEquals(4.0f / 3, RANGE.getMinimumRatio());
        Assertions.assertEquals(16.0f / 9, RANGE.getMaximumRatio());
    }

    @Test
    void get_logical_width_should_follow_the_window_ratio_within_the_range() {
        Assertions.assertEquals(640, RANGE.getLogicalWidth(1024, 768));
        Assertions.assertEquals(853, RANGE.getLogicalWidth(1920, 1080));
        Assertions.assertEquals(768, RANGE.getLogicalWidth(1680, 1050));
    }

    @Test
    void get_logical_width_should_clamp_ratios_outside_the_range() {
        Assertions.assertEquals(853, RANGE.getLogicalWidth(2560, 1080));
        Assertions.assertEquals(640, RANGE.getLogicalWidth(800, 800));
        Assertions.assertEquals(640, RANGE.getLogicalWidth(800, 0));
    }

    @Test
    void constructor_should_reject_reversed_ratios() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AspectRange(LOGICAL_HEIGHT, 2, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AspectRange(0, 1, 2));
    }
}
