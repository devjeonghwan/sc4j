package org.fogofwarlabs.sc4j.engine.path;

/**
 * Describes the walkable terrain as a graph of regions: the minimal terrain the engine path default needs. Every map pixel lies in one region;
 * regions that ground units can walk between share a connected component; each region lists its neighbors, the walkable ones first; a region has
 * a center the route search measures between.
 * <p>
 * Regions and components are plain indices of the terrain structure, not content identities: {@code 0} up to {@link #getRegionCapacity()}.
 * Positions are map pixels; centers are fixed-point values with {@value #FIXED_POINT_SHIFT} fraction bits.
 * <p>
 * Determinism contract: a graph changes only between frames the mod's terrain phases run in; the queries change nothing and allocate nothing.
 */
public interface RegionGraph {
    /**
     * The fraction bits of the fixed-point region centers.
     */
    int FIXED_POINT_SHIFT = 8;

    /**
     * @return the number of region indices: every region index is below it
     */
    int getRegionCapacity();

    /**
     * Finds the region of a map position.
     *
     * @param x the x coordinate in pixels
     * @param y the y coordinate in pixels
     * @return the region index
     */
    int findRegion(int x, int y);

    /**
     * @param region the region index
     * @return the connected component of the region: regions with the same component reach each other on the ground
     */
    int getComponent(int region);

    /**
     * @param region the region index
     * @return the number of neighbors of the region, walkable or not
     */
    int getNeighborCount(int region);

    /**
     * @param region the region index
     * @return the number of walkable neighbors, which come first in the neighbor list
     */
    int getWalkableNeighborCount(int region);

    /**
     * @param region        the region index
     * @param neighborIndex the position in the neighbor list, below the neighbor count
     * @return the neighboring region index
     */
    int getNeighborRegion(int region, int neighborIndex);

    /**
     * @param region the region index
     * @return the horizontal center, fixed-point
     */
    int getCenterHorizontalFixed(int region);

    /**
     * @param region the region index
     * @return the vertical center, fixed-point
     */
    int getCenterVerticalFixed(int region);
}
