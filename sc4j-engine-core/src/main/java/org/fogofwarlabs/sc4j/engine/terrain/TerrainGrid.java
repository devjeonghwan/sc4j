package org.fogofwarlabs.sc4j.engine.terrain;

import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.jetbrains.annotations.NotNull;

/**
 * Describes the terrain of a map: which walk cells ground units can stand on, how high each walk cell lies and which tiles take buildings. The
 * map is a grid of square tiles, each divided into square walk cells; the mod configures both sizes (the tile size is a multiple of the walk
 * cell size). The engine default storage is {@link ArrayTerrainGrid}; a mod may keep its own layout.
 * <p>
 * Walk cells and tiles are numbered from the top left: walk cell {@code (walkX, walkY)} covers the pixels from {@code walkX * getWalkCellSize()}
 * and {@code walkY * getWalkCellSize()}. Code that converts between world positions and cells goes through the two grid topologies
 * ({@link #getTileGrid()}, {@link #getWalkGrid()}), so the cell layout is stated in one place. Elevations are small levels from {@code 0}
 * (the lowest ground) upward; a higher level hides the lower ones from sight.
 * <p>
 * Determinism contract: the terrain changes only where the mod's rules change it inside the simulation frame; the queries change nothing and
 * allocate nothing. A cell outside the map reads as its storage defines it.
 */
public interface TerrainGrid {
    /**
     * @return the size of a tile in pixels
     */
    int getTileSize();

    /**
     * @return the size of a walk cell in pixels, which divides the tile size
     */
    int getWalkCellSize();

    /**
     * @return the map width in tiles
     */
    int getWidth();

    /**
     * @return the map height in tiles
     */
    int getHeight();

    /**
     * @return the grid of the tiles over the world plane, the same instance on every call while the map size stays
     */
    @NotNull GridTopology getTileGrid();

    /**
     * @return the grid of the walk cells over the world plane, the same instance on every call while the map size stays
     */
    @NotNull GridTopology getWalkGrid();

    /**
     * @return the map width in walk cells
     */
    default int getWalkWidth() {
        return this.getWidth() * (this.getTileSize() / this.getWalkCellSize());
    }

    /**
     * @return the map height in walk cells
     */
    default int getWalkHeight() {
        return this.getHeight() * (this.getTileSize() / this.getWalkCellSize());
    }

    /**
     * Checks whether ground units can stand on a walk cell.
     *
     * @param walkX the horizontal walk cell
     * @param walkY the vertical walk cell
     * @return {@code true} if the walk cell is walkable, {@code false} otherwise
     */
    boolean isWalkable(int walkX, int walkY);

    /**
     * Checks whether a tile takes buildings.
     *
     * @param tileX the horizontal tile
     * @param tileY the vertical tile
     * @return {@code true} if the tile is buildable, {@code false} otherwise
     */
    boolean isBuildable(int tileX, int tileY);

    /**
     * Reads the elevation of a walk cell.
     *
     * @param walkX the horizontal walk cell
     * @param walkY the vertical walk cell
     * @return the elevation level, {@code 0} for the lowest ground
     */
    int getElevation(int walkX, int walkY);
}
