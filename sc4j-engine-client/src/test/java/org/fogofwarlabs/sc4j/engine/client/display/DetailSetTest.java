package org.fogofwarlabs.sc4j.engine.client.display;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests picking the version of a resource with detail versions by the physical pixels per resource pixel of a drawing.
 */
final class DetailSetTest {
    private static final int DOUBLE = 2;
    private static final int QUADRUPLE = 4;
    private static final int TOO_LARGE = 5;
    private static final float HALF = 0.5f;
    private static final float ONE_AND_A_HALF = 1.5f;
    private static final float THREE = 3;
    private static final float EIGHT = 8;
    private static final float NEAR_DOUBLE = 2.04f;

    @Test
    void find_should_pick_the_smallest_detail_with_at_least_the_pixels_per_unit() {
        DetailSet<String> set = new DetailSet<>("1x", List.of("2x", "4x"), new int[]{DOUBLE, QUADRUPLE});

        Assertions.assertEquals("1x", set.find(HALF));
        Assertions.assertEquals("1x", set.find(1));
        Assertions.assertEquals("2x", set.find(ONE_AND_A_HALF));
        Assertions.assertEquals("2x", set.find(DOUBLE));
        Assertions.assertEquals("2x", set.find(NEAR_DOUBLE), "a scale computed from rounded sizes keeps its multiple");
        Assertions.assertEquals("4x", set.find(THREE));
        Assertions.assertEquals("4x", set.find(EIGHT), "the largest detail beyond the multiples");
        Assertions.assertEquals(QUADRUPLE, set.findMultiple(THREE));
        Assertions.assertEquals(1, set.findMultiple(1));
        Assertions.assertEquals(-1, set.findDetail(1));
        Assertions.assertEquals(1, set.findDetail(THREE));
        Assertions.assertEquals("1x", set.getResource());
        Assertions.assertEquals(DOUBLE, set.getDetailCount());
        Assertions.assertEquals("4x", set.getDetail(1));
        Assertions.assertEquals(DOUBLE, set.getMultiple(0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> set.getMultiple(DOUBLE));
    }

    @Test
    void of_should_create_a_set_that_always_draws_the_resource() {
        DetailSet<String> set = DetailSet.of("1x");

        Assertions.assertEquals(0, set.getDetailCount());
        Assertions.assertEquals("1x", set.find(EIGHT));
        Assertions.assertEquals(1, set.findMultiple(EIGHT));
    }

    @Test
    void constructor_should_reject_multiples_out_of_range_or_order() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new DetailSet<>("1x", List.of("1x"), new int[]{1}));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new DetailSet<>("1x", List.of("5x"), new int[]{TOO_LARGE}));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new DetailSet<>("1x", List.of("4x", "2x"), new int[]{QUADRUPLE, DOUBLE}));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new DetailSet<>("1x", List.of("2x"), new int[]{DOUBLE, QUADRUPLE}));
    }
}
