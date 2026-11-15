package org.fogofwarlabs.sc4j.engine.terrain;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.fogofwarlabs.sc4j.engine.map.grid.SquareGridTopology;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.jetbrains.annotations.NotNull;

/**
 * Stores a terrain in plain arrays: one walkable flag and one elevation per walk cell, one buildable flag per tile. The engine default
 * {@link TerrainGrid}; a new grid is unwalkable, unbuildable and at elevation {@code 0} everywhere until the mod's map loader fills it.
 * <p>
 * A cell or tile outside the map is not walkable, not buildable and at elevation {@code 0}.
 */
public final class ArrayTerrainGrid implements TerrainGrid, StateHashContributor {
    /**
     * The highest elevation level a cell can have.
     */
    public static final int MAXIMUM_ELEVATION = 7;

    private final int tileSize;
    private final int walkCellSize;
    private final int width;
    private final int height;
    private final int walkWidth;
    private final int walkHeight;
    private final @NotNull SquareGridTopology tileGrid;
    private final @NotNull SquareGridTopology walkGrid;

    private final boolean @NotNull [] walkable;
    private final byte @NotNull [] elevations;
    private final boolean @NotNull [] buildable;

    /**
     * Creates an {@link ArrayTerrainGrid} instance with no walkable cell and no buildable tile.
     *
     * @param tileSize     the size of a tile in pixels
     * @param walkCellSize the size of a walk cell in pixels, which divides the tile size
     * @param width        the map width in tiles
     * @param height       the map height in tiles
     * @throws IllegalArgumentException if a value is not positive or the walk cell size does not divide the tile size
     */
    public ArrayTerrainGrid(int tileSize, int walkCellSize, int width, int height) {
        if (tileSize <= 0 || walkCellSize <= 0 || tileSize % walkCellSize != 0) {
            throw Exceptions.newIllegalArgumentException("A terrain grid requires a tile size that is a multiple of the walk cell size: {0}, {1}",
                                                         tileSize, walkCellSize);
        }

        if (width <= 0 || height <= 0) {
            throw Exceptions.newIllegalArgumentException("A terrain grid requires positive dimensions: {0} x {1}", width, height);
        }

        this.tileSize     = tileSize;
        this.walkCellSize = walkCellSize;
        this.width        = width;
        this.height       = height;
        this.walkWidth    = width * (tileSize / walkCellSize);
        this.walkHeight   = height * (tileSize / walkCellSize);
        this.tileGrid     = new SquareGridTopology(tileSize, width, height);
        this.walkGrid     = new SquareGridTopology(walkCellSize, this.walkWidth, this.walkHeight);

        this.walkable   = new boolean[this.walkWidth * this.walkHeight];
        this.elevations = new byte[this.walkWidth * this.walkHeight];
        this.buildable  = new boolean[width * height];
    }

    /**
     * @return the size of a tile in pixels
     */
    @Override
    public int getTileSize() {
        return this.tileSize;
    }

    /**
     * @return the size of a walk cell in pixels, which divides the tile size
     */
    @Override
    public int getWalkCellSize() {
        return this.walkCellSize;
    }

    /**
     * @return the map width in tiles
     */
    @Override
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the map height in tiles
     */
    @Override
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the grid of the tiles over the world plane, the same instance on every call while the map size stays
     */
    @Override
    public @NotNull GridTopology getTileGrid() {
        return this.tileGrid;
    }

    /**
     * @return the grid of the walk cells over the world plane, the same instance on every call while the map size stays
     */
    @Override
    public @NotNull GridTopology getWalkGrid() {
        return this.walkGrid;
    }

    /**
     * @return the map width in walk cells
     */
    @Override
    public int getWalkWidth() {
        return this.walkWidth;
    }

    /**
     * @return the map height in walk cells
     */
    @Override
    public int getWalkHeight() {
        return this.walkHeight;
    }


    /**
     * Checks whether ground units can stand on a walk cell.
     *
     * @param walkX the horizontal walk cell
     * @param walkY the vertical walk cell
     * @return {@code true} if the walk cell is walkable, {@code false} otherwise
     */
    @Override
    public boolean isWalkable(int walkX, int walkY) {
        return this.isInsideWalkGrid(walkX, walkY) && this.walkable[walkY * this.walkWidth + walkX];
    }

    /**
     * Sets whether ground units can stand on a walk cell.
     *
     * @param walkX    the horizontal walk cell
     * @param walkY    the vertical walk cell
     * @param walkable whether the walk cell is walkable
     * @throws IndexOutOfBoundsException if the walk cell is outside the map
     */
    public void setWalkable(int walkX, int walkY, boolean walkable) {
        this.walkable[this.requireWalkCell(walkX, walkY)] = walkable;
    }

    /**
     * Checks whether a tile takes buildings.
     *
     * @param tileX the horizontal tile
     * @param tileY the vertical tile
     * @return {@code true} if the tile is buildable, {@code false} otherwise
     */
    @Override
    public boolean isBuildable(int tileX, int tileY) {
        return tileX >= 0 && tileX < this.width && tileY >= 0 && tileY < this.height && this.buildable[tileY * this.width + tileX];
    }

    /**
     * Sets whether a tile takes buildings.
     *
     * @param tileX     the horizontal tile
     * @param tileY     the vertical tile
     * @param buildable whether the tile is buildable
     * @throws IndexOutOfBoundsException if the tile is outside the map
     */
    public void setBuildable(int tileX, int tileY, boolean buildable) {
        if (tileX < 0 || tileX >= this.width || tileY < 0 || tileY >= this.height) {
            throw Exceptions.newIndexOutOfBoundsException("The tile is outside the terrain grid: {0}, {1}", tileX, tileY);
        }

        this.buildable[tileY * this.width + tileX] = buildable;
    }

    /**
     * Reads the elevation of a walk cell.
     *
     * @param walkX the horizontal walk cell
     * @param walkY the vertical walk cell
     * @return the elevation level, {@code 0} for the lowest ground
     */
    @Override
    public int getElevation(int walkX, int walkY) {
        return this.isInsideWalkGrid(walkX, walkY) ? this.elevations[walkY * this.walkWidth + walkX] : 0;
    }

    /**
     * Sets the elevation of a walk cell.
     *
     * @param walkX     the horizontal walk cell
     * @param walkY     the vertical walk cell
     * @param elevation the elevation level, {@code 0} up to {@value #MAXIMUM_ELEVATION}
     * @throws IndexOutOfBoundsException if the walk cell is outside the map
     * @throws IllegalArgumentException  if the elevation is outside its range
     */
    public void setElevation(int walkX, int walkY, int elevation) {
        if (elevation < 0 || elevation > MAXIMUM_ELEVATION) {
            throw Exceptions.newIllegalArgumentException("A terrain elevation is outside its range: {0}", elevation);
        }

        this.elevations[this.requireWalkCell(walkX, walkY)] = (byte) elevation;
    }

    /**
     * Mixes this part of the simulation state into the hash: every walk cell's walkable flag and elevation in row order, then every tile's
     * buildable flag in row order.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        for (int cell = 0; cell < this.walkable.length; cell++) {
            hash.mixBoolean(this.walkable[cell]);
            hash.mix(this.elevations[cell]);
        }

        for (boolean tileBuildable : this.buildable) {
            hash.mixBoolean(tileBuildable);
        }
    }

    private boolean isInsideWalkGrid(int walkX, int walkY) {
        return walkX >= 0 && walkX < this.walkWidth && walkY >= 0 && walkY < this.walkHeight;
    }

    private int requireWalkCell(int walkX, int walkY) {
        if (!this.isInsideWalkGrid(walkX, walkY)) {
            throw Exceptions.newIndexOutOfBoundsException("The walk cell is outside the terrain grid: {0}, {1}", walkX, walkY);
        }

        return walkY * this.walkWidth + walkX;
    }
}
