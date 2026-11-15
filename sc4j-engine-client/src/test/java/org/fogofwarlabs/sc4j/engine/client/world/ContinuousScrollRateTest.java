package org.fogofwarlabs.sc4j.engine.client.world;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the continuous scroll rate: screen pixels per second for the time of an update, divided by the zoom, the acceleration step ignored.
 */
final class ContinuousScrollRateTest {
    private static final float KEY_SPEED = 1500;
    private static final float EDGE_SPEED = 600;
    private static final long MILLISECONDS = 100;
    private static final float ZOOM = 2;
    private static final int STEP = 4;
    private static final float MILLISECONDS_PER_SECOND = 1000;
    private static final float DELTA = 0.001f;

    @Test
    void continuous_rate_moves_by_speed_and_time_in_screen_pixels() {
        ContinuousScrollRate rate = new ContinuousScrollRate();

        Assertions.assertEquals(ContinuousScrollRate.DEFAULT_KEY_SPEED, rate.getKeySpeed(), DELTA);
        Assertions.assertEquals(ContinuousScrollRate.DEFAULT_EDGE_SPEED, rate.getEdgeSpeed(), DELTA);

        rate.setKeySpeed(KEY_SPEED);
        rate.setEdgeSpeed(EDGE_SPEED);

        Assertions.assertEquals(KEY_SPEED * MILLISECONDS / MILLISECONDS_PER_SECOND / ZOOM, rate.getKeyDistance(STEP, MILLISECONDS, ZOOM), DELTA);
        Assertions.assertEquals(EDGE_SPEED * MILLISECONDS / MILLISECONDS_PER_SECOND, rate.getEdgeDistance(0, MILLISECONDS, 1), DELTA);
        Assertions.assertEquals(0, rate.getEdgeDistance(STEP, 0, 1), DELTA);
        Assertions.assertThrows(IllegalArgumentException.class, () -> rate.setKeySpeed(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> rate.setEdgeSpeed(-1));
    }
}
