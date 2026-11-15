package org.fogofwarlabs.sc4j.engine.client.world;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the default scroll step table: it reports the speed and step counts it is created with, and steps grow linearly with the speed row and
 * the step index.
 */
final class LinearScrollStepTableTest {
    private static final int SPEED_COUNT = 3;
    private static final int STEP_COUNT = 5;
    private static final int UNIT = 3;
    private static final int SPEED = 2;
    private static final int INDEX = 4;

    @Test
    void get_step_should_grow_with_speed_and_index() {
        LinearScrollStepTable table = new LinearScrollStepTable(SPEED_COUNT, STEP_COUNT, UNIT);

        Assertions.assertEquals(SPEED_COUNT, table.getSpeedCount());
        Assertions.assertEquals(STEP_COUNT, table.getStepCount());
        Assertions.assertEquals(0, table.getStep(SPEED, 0), "no step before the first moving tick");
        Assertions.assertEquals(INDEX * (SPEED + 1) * UNIT, table.getStep(SPEED, INDEX));
        Assertions.assertEquals(LinearScrollStepTable.DEFAULT_UNIT, new LinearScrollStepTable(SPEED_COUNT, STEP_COUNT).getStep(0, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LinearScrollStepTable(SPEED_COUNT, STEP_COUNT, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LinearScrollStepTable(0, STEP_COUNT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LinearScrollStepTable(SPEED_COUNT, 0));
    }
}
