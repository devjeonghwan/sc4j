package org.fogofwarlabs.sc4j.engine.path;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the region route search over a region graph: a route goes around a blocked region.
 */
final class RegionRouteSearchTest {
    private static final int GRID_SIZE = 3;
    private static final int REGION_SIZE = 64;
    private static final int HALF_REGION = REGION_SIZE / 2;
    private static final int BLOCKED_REGION = 4;
    private static final int WALKABLE_COMPONENT = 1;
    private static final int BLOCKED_COMPONENT = 2;
    private static final int DIRECTION_COUNT = 4;

    /**
     * A three by three grid of square regions with four neighbors each; the center region is a separate component without walkable neighbors.
     */
    private static final class GridRegions implements RegionGraph {
        @Override
        public int getRegionCapacity() {
            return GRID_SIZE * GRID_SIZE;
        }

        @Override
        public int findRegion(int x, int y) {
            return Math.floorDiv(y, REGION_SIZE) * GRID_SIZE + Math.floorDiv(x, REGION_SIZE);
        }

        @Override
        public int getComponent(int region) {
            return region == BLOCKED_REGION ? BLOCKED_COMPONENT : WALKABLE_COMPONENT;
        }

        @Override
        public int getNeighborCount(int region) {
            int count = 0;

            for (int direction = 0; direction < DIRECTION_COUNT; direction++) {
                if (this.findNeighbor(region, direction) >= 0) {
                    count++;
                }
            }

            return count;
        }

        @Override
        public int getWalkableNeighborCount(int region) {
            return region == BLOCKED_REGION ? 0 : this.getNeighborCount(region);
        }

        @Override
        public int getNeighborRegion(int region, int neighborIndex) {
            int remaining = neighborIndex;

            for (int direction = 0; direction < DIRECTION_COUNT; direction++) {
                int neighbor = this.findNeighbor(region, direction);

                if (neighbor >= 0 && remaining-- == 0) {
                    return neighbor;
                }
            }

            throw Exceptions.newIndexOutOfBoundsException("No neighbor {0} of region {1}", neighborIndex, region);
        }

        @Override
        public int getCenterHorizontalFixed(int region) {
            return (region % GRID_SIZE * REGION_SIZE + HALF_REGION) << FIXED_POINT_SHIFT;
        }

        @Override
        public int getCenterVerticalFixed(int region) {
            return (region / GRID_SIZE * REGION_SIZE + HALF_REGION) << FIXED_POINT_SHIFT;
        }

        /**
         * Returns the neighbor of a region in one of the four directions that is walkable from it.
         *
         * @param region    the region
         * @param direction the direction: up, right, down or left
         * @return the neighbor, or {@code -1} if the direction leaves the grid or leads to the blocked region
         */
        private int findNeighbor(int region, int direction) {
            int column = region % GRID_SIZE + (direction == 1 ? 1 : direction == 3 ? -1 : 0);
            int row = region / GRID_SIZE + (direction == 0 ? -1 : direction == 2 ? 1 : 0);
            int neighbor = row * GRID_SIZE + column;

            if (column < 0 || column >= GRID_SIZE || row < 0 || row >= GRID_SIZE || neighbor == BLOCKED_REGION) {
                return -1;
            }

            return neighbor;
        }
    }

    @Test
    void route_search_goes_around_the_blocked_region() {
        RegionRouteSearch search = new RegionRouteSearch(new GridRegions());

        Assertions.assertTrue(search.search(HALF_REGION, HALF_REGION + REGION_SIZE, 2 * REGION_SIZE + HALF_REGION, HALF_REGION + REGION_SIZE));

        Assertions.assertEquals(5, search.getRouteCount());
        Assertions.assertEquals(3, search.getRouteRegion(0));
        Assertions.assertEquals(5, search.getRouteRegion(search.getRouteCount() - 1));
        Assertions.assertFalse(search.isIncomplete());

        for (int index = 0; index < search.getRouteCount(); index++) {
            Assertions.assertNotEquals(BLOCKED_REGION, search.getRouteRegion(index));
        }
    }
}
