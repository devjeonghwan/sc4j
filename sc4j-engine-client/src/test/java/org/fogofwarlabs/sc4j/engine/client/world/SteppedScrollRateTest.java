package org.fogofwarlabs.sc4j.engine.client.world;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the stepped scroll rate: one step of the table per update by the key or edge speed row, whatever the time and the zoom, starting at
 * the middle row of the table, with rows outside the table read as the nearest row and the acceleration steps of the table.
 */
final class SteppedScrollRateTest {
    private static final int SPEED_COUNT = 7;
    private static final int STEP_COUNT = 7;
    private static final int MIDDLE_SPEED = 3;
    private static final int KEY_SPEED = 1;
    private static final int EDGE_SPEED = 5;
    private static final int STEP = 3;
    private static final long MILLISECONDS = 250;
    private static final float ZOOM = 2;
    private static final int SMALL_SPEED_COUNT = 3;
    private static final int SMALL_STEP_COUNT = 5;
    private static final int SMALL_MIDDLE_SPEED = 1;

    @Test
    void stepped_rate_reads_one_table_step_per_update() {
        LinearScrollStepTable table = new LinearScrollStepTable(SPEED_COUNT, STEP_COUNT);
        SteppedScrollRate rate = new SteppedScrollRate(table);

        Assertions.assertEquals(MIDDLE_SPEED, rate.getKeySpeed());
        Assertions.assertEquals(MIDDLE_SPEED, rate.getEdgeSpeed());

        rate.setKeySpeed(KEY_SPEED);
        rate.setEdgeSpeed(EDGE_SPEED);

        Assertions.assertEquals(table.getStep(KEY_SPEED, STEP), rate.getKeyDistance(STEP, MILLISECONDS, ZOOM));
        Assertions.assertEquals(table.getStep(EDGE_SPEED, STEP), rate.getEdgeDistance(STEP, 0, 1), "time and zoom do not count");

        rate.setKeySpeed(SPEED_COUNT);
        rate.setEdgeSpeed(-1);

        Assertions.assertEquals(table.getStep(SPEED_COUNT - 1, STEP), rate.getKeyDistance(STEP, MILLISECONDS, ZOOM));
        Assertions.assertEquals(table.getStep(0, STEP), rate.getEdgeDistance(STEP, MILLISECONDS, ZOOM));
    }

    @Test
    void stepped_rate_clamps_to_the_counts_of_its_table() {
        LinearScrollStepTable table = new LinearScrollStepTable(SMALL_SPEED_COUNT, SMALL_STEP_COUNT);
        SteppedScrollRate rate = new SteppedScrollRate(table);

        Assertions.assertEquals(SMALL_MIDDLE_SPEED, rate.getKeySpeed(), "the middle row of three");
        Assertions.assertEquals(SMALL_STEP_COUNT, rate.getStepCount());

        rate.setKeySpeed(SPEED_COUNT - 1);

        Assertions.assertEquals(table.getStep(SMALL_SPEED_COUNT - 1, STEP), rate.getKeyDistance(STEP, MILLISECONDS, ZOOM),
                                "a row past the third reads the third");
    }
}
