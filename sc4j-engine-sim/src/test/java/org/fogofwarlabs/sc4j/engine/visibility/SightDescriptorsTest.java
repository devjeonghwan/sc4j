package org.fogofwarlabs.sc4j.engine.visibility;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the sight descriptors: each descriptor orders the tiles of a circle mask as a spiral from the center.
 */
final class SightDescriptorsTest {
    private static final int MAP_SIZE = 32;
    private static final int MAXIMUM_SIGHT_RANGE = 12;

    @Test
    void sight_descriptors_order_a_circle_mask_as_a_spiral() {
        SightDescriptors descriptors = new SightDescriptors(MAP_SIZE, MAXIMUM_SIGHT_RANGE);

        Assertions.assertEquals(MAXIMUM_SIGHT_RANGE, descriptors.getDescriptorCount());
        Assertions.assertEquals(SightDescriptors.CENTRAL_RECORD_COUNT, descriptors.getRecordCount(0));
        Assertions.assertEquals(-1, descriptors.getFirstPredecessor(0, 0));
        Assertions.assertEquals(0, descriptors.getHorizontalOffset(0, 0));
        Assertions.assertEquals(0, descriptors.getHorizontalOffset(0, 1));
        Assertions.assertEquals(-1, descriptors.getVerticalOffset(0, 1));
        Assertions.assertEquals(-MAP_SIZE, descriptors.getTileOffset(0, 1));

        for (int radiusIndex = 1; radiusIndex < MAXIMUM_SIGHT_RANGE; radiusIndex++) {
            Assertions.assertTrue(descriptors.getRecordCount(radiusIndex) > descriptors.getRecordCount(radiusIndex - 1));

            for (int recordIndex = SightDescriptors.CENTRAL_RECORD_COUNT; recordIndex < descriptors.getRecordCount(radiusIndex); recordIndex++) {
                Assertions.assertTrue(descriptors.getFirstPredecessor(radiusIndex, recordIndex) < recordIndex);
                Assertions.assertTrue(descriptors.getSecondPredecessor(radiusIndex, recordIndex) < recordIndex);
            }
        }
    }
}
