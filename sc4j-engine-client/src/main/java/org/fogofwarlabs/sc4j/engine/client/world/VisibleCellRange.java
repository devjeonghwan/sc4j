package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the cells of a grid that a frame's viewport may show: the columns and rows of the world box around the four corners of the shown area,
 * as the projection maps them back to the world, limited to the grid. The world layers that draw per cell (terrain, tiles, fog) keep one and
 * update it every frame ({@link #update}), so the range allocates nothing. With the top-down square projection the range is exactly the cells
 * the shown area touches; with a turned projection it also holds cells beside the shown area, which draw outside the screen.
 */
public final class VisibleCellRange {
    private static final int HALF_DIVISOR = 2;

    private int firstColumn;
    private int lastColumn;
    private int firstRow;
    private int lastRow;

    /**
     * Creates a {@link VisibleCellRange} instance that holds no cell until it is updated.
     */
    public VisibleCellRange() {
        this.firstColumn = 0;
        this.lastColumn  = -1;
        this.firstRow    = 0;
        this.lastRow     = -1;
    }

    /**
     * @return the first column of the range
     */
    public int getFirstColumn() {
        return this.firstColumn;
    }

    /**
     * @return the last column of the range, below the first one if the range is empty
     */
    public int getLastColumn() {
        return this.lastColumn;
    }

    /**
     * @return the first row of the range
     */
    public int getFirstRow() {
        return this.firstRow;
    }

    /**
     * @return the last row of the range, below the first one if the range is empty
     */
    public int getLastRow() {
        return this.lastRow;
    }


    /**
     * Updates the range to the cells of a grid that a viewport may show.
     *
     * @param viewport the frame's viewport
     * @param grid     the grid of the cells
     */
    public void update(@NotNull WorldViewport viewport, @NotNull GridTopology grid) {
        WorldProjection projection = viewport.getProjection();
        float left = viewport.getCameraX();
        float top = viewport.getCameraY();
        float right = viewport.getCameraRight();
        float bottom = viewport.getCameraBottom();
        float topLeftX = projection.toWorldX(left, top);
        float topLeftY = projection.toWorldY(left, top);
        float topRightX = projection.toWorldX(right, top);
        float topRightY = projection.toWorldY(right, top);
        float bottomRightX = projection.toWorldX(right, bottom);
        float bottomRightY = projection.toWorldY(right, bottom);
        float bottomLeftX = projection.toWorldX(left, bottom);
        float bottomLeftY = projection.toWorldY(left, bottom);
        int minimumX = (int) Math.floor(Math.min(Math.min(topLeftX, topRightX), Math.min(bottomRightX, bottomLeftX)));
        int minimumY = (int) Math.floor(Math.min(Math.min(topLeftY, topRightY), Math.min(bottomRightY, bottomLeftY)));
        int maximumX = (int) Math.floor(Math.max(Math.max(topLeftX, topRightX), Math.max(bottomRightX, bottomLeftX)));
        int maximumY = (int) Math.floor(Math.max(Math.max(topLeftY, topRightY), Math.max(bottomRightY, bottomLeftY)));

        this.firstColumn = Math.max(grid.findColumn(minimumX, minimumY), 0);
        this.firstRow    = Math.max(grid.findRow(minimumX, minimumY), 0);
        this.lastColumn  = Math.min(grid.findColumn(maximumX, maximumY), grid.getColumnCount() - 1);
        this.lastRow     = Math.min(grid.findRow(maximumX, maximumY), grid.getRowCount() - 1);
    }

    /**
     * Writes the screen outline of a cell's bounds through the viewport's projection: the corners of the world top-left, top-right,
     * bottom-right and bottom-left corners of the box of the cell's size around its centre, as x and y pairs.
     *
     * @param viewport the frame's viewport
     * @param grid     the grid of the cell
     * @param column   the column
     * @param row      the row
     * @param corners  the {@value WorldProjection#OUTLINE_LENGTH} values written from index {@code 0}
     * @throws IndexOutOfBoundsException if the array is shorter than {@value WorldProjection#OUTLINE_LENGTH}
     */
    public void outlineCell(@NotNull WorldViewport viewport, @NotNull GridTopology grid, int column, int row, float @NotNull [] corners) {
        int left = grid.getCenterX(column, row) - grid.getCellWidth() / HALF_DIVISOR;
        int top = grid.getCenterY(column, row) - grid.getCellHeight() / HALF_DIVISOR;

        viewport.outlineOnScreen(left, top, left + grid.getCellWidth(), top + grid.getCellHeight(), corners);
    }
}
