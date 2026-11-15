package org.fogofwarlabs.sc4j.engine.map.grid;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Lays square cells of one size in rows over the world plane: the engine default {@link GridTopology}. Cell {@code (column, row)} covers the
 * world positions from {@code column * cellSize} and {@code row * cellSize} up to the next cell; its centre lies {@code cellSize / 2} further.
 * Every cell has eight neighbours, clockwise from the one above: up, up-right, right, down-right, down, down-left, left, up-left.
 */
public final class SquareGridTopology implements GridTopology {
    private static final int NEIGHBOUR_COUNT = 8;
    private static final int CENTER_DIVISOR = 2;
    private static final int @NotNull [] NEIGHBOUR_COLUMNS = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int @NotNull [] NEIGHBOUR_ROWS = {-1, -1, 0, 1, 1, 1, 0, -1};

    private static int requireNeighbour(int neighbour) {
        if (neighbour < 0 || neighbour >= NEIGHBOUR_COUNT) {
            throw Exceptions.newIndexOutOfBoundsException("A square cell has no neighbour {0}", neighbour);
        }

        return neighbour;
    }

    private final int cellSize;
    private final int columnCount;
    private final int rowCount;

    /**
     * Creates a {@link SquareGridTopology} instance.
     *
     * @param cellSize    the side of a cell in world units
     * @param columnCount the number of columns
     * @param rowCount    the number of rows
     * @throws IllegalArgumentException if a value is not positive
     */
    public SquareGridTopology(int cellSize, int columnCount, int rowCount) {
        if (cellSize <= 0 || columnCount <= 0 || rowCount <= 0) {
            throw Exceptions.newIllegalArgumentException("A square grid requires a positive cell size and dimensions: {0}, {1} x {2}",
                                                         cellSize, columnCount, rowCount);
        }

        this.cellSize    = cellSize;
        this.columnCount = columnCount;
        this.rowCount    = rowCount;
    }

    /**
     * @return the number of columns
     */
    @Override
    public int getColumnCount() {
        return this.columnCount;
    }

    /**
     * @return the number of rows
     */
    @Override
    public int getRowCount() {
        return this.rowCount;
    }

    /**
     * @return the width of the bounds of one cell in world units
     */
    @Override
    public int getCellWidth() {
        return this.cellSize;
    }

    /**
     * @return the height of the bounds of one cell in world units
     */
    @Override
    public int getCellHeight() {
        return this.cellSize;
    }

    /**
     * @return the width of the world plane the grid covers in world units
     */
    @Override
    public int getWorldWidth() {
        return this.columnCount * this.cellSize;
    }

    /**
     * @return the height of the world plane the grid covers in world units
     */
    @Override
    public int getWorldHeight() {
        return this.rowCount * this.cellSize;
    }


    /**
     * Checks whether a cell belongs to the grid.
     *
     * @param column the column
     * @param row    the row
     * @return {@code true} if the cell lies inside the grid, {@code false} otherwise
     */
    @Override
    public boolean contains(int column, int row) {
        return column >= 0 && column < this.columnCount && row >= 0 && row < this.rowCount;
    }

    /**
     * Finds the column of the cell that holds a world position.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the column, outside the grid if the position lies outside the map
     */
    @Override
    public int findColumn(int worldX, int worldY) {
        return Math.floorDiv(worldX, this.cellSize);
    }

    /**
     * Finds the row of the cell that holds a world position.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the row, outside the grid if the position lies outside the map
     */
    @Override
    public int findRow(int worldX, int worldY) {
        return Math.floorDiv(worldY, this.cellSize);
    }

    /**
     * Reads the world x of a cell's centre.
     *
     * @param column the column
     * @param row    the row
     * @return the world x of the centre
     */
    @Override
    public int getCenterX(int column, int row) {
        return column * this.cellSize + this.cellSize / CENTER_DIVISOR;
    }

    /**
     * Reads the world y of a cell's centre.
     *
     * @param column the column
     * @param row    the row
     * @return the world y of the centre
     */
    @Override
    public int getCenterY(int column, int row) {
        return row * this.cellSize + this.cellSize / CENTER_DIVISOR;
    }

    /**
     * @return the number of neighbours of every cell, counting neighbours outside the grid
     */
    @Override
    public int getNeighbourCount() {
        return NEIGHBOUR_COUNT;
    }

    /**
     * Reads the column of a neighbour of a cell.
     *
     * @param column    the column of the cell
     * @param row       the row of the cell
     * @param neighbour the neighbour, {@code 0} up to {@link #getNeighbourCount()} excluded, in the topology's clockwise order
     * @return the neighbour's column, possibly outside the grid
     * @throws IndexOutOfBoundsException if the neighbour is outside its range
     */
    @Override
    public int getNeighbourColumn(int column, int row, int neighbour) {
        return column + NEIGHBOUR_COLUMNS[requireNeighbour(neighbour)];
    }

    /**
     * Reads the row of a neighbour of a cell.
     *
     * @param column    the column of the cell
     * @param row       the row of the cell
     * @param neighbour the neighbour, {@code 0} up to {@link #getNeighbourCount()} excluded, in the topology's clockwise order
     * @return the neighbour's row, possibly outside the grid
     * @throws IndexOutOfBoundsException if the neighbour is outside its range
     */
    @Override
    public int getNeighbourRow(int column, int row, int neighbour) {
        return row + NEIGHBOUR_ROWS[requireNeighbour(neighbour)];
    }
}
