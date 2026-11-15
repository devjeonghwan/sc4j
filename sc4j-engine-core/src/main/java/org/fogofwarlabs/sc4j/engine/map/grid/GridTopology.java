package org.fogofwarlabs.sc4j.engine.map.grid;

/**
 * Describes how a grid of cells lies over the world plane of a map: how many cells it has, which cell holds a world position, where a cell's
 * centre lies and which cells neighbour a cell. The simulation's world positions are integer world units with the origin at the top-left corner
 * of the map, x to the right and y downward; how the client shows that plane on the screen is a separate concern of the client's world
 * projection. The engine default is {@link SquareGridTopology}; the interface leaves room for other layouts (hexagonal cells in staggered rows,
 * for example), whose cell lookup depends on both coordinates.
 * <p>
 * Cells are addressed by a column and a row from {@code 0}; a lookup of a world position outside the map gives a column or row outside the
 * grid, which {@link #contains} rejects. Every query is a plain computation: it changes nothing and allocates nothing, so the simulation can use
 * it in deterministic code.
 */
public interface GridTopology {
    /**
     * @return the number of columns
     */
    int getColumnCount();

    /**
     * @return the number of rows
     */
    int getRowCount();

    /**
     * @return the width of the bounds of one cell in world units
     */
    int getCellWidth();

    /**
     * @return the height of the bounds of one cell in world units
     */
    int getCellHeight();

    /**
     * @return the width of the world plane the grid covers in world units
     */
    int getWorldWidth();

    /**
     * @return the height of the world plane the grid covers in world units
     */
    int getWorldHeight();

    /**
     * Checks whether a cell belongs to the grid.
     *
     * @param column the column
     * @param row    the row
     * @return {@code true} if the cell lies inside the grid, {@code false} otherwise
     */
    boolean contains(int column, int row);

    /**
     * Finds the column of the cell that holds a world position.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the column, outside the grid if the position lies outside the map
     */
    int findColumn(int worldX, int worldY);

    /**
     * Finds the row of the cell that holds a world position.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the row, outside the grid if the position lies outside the map
     */
    int findRow(int worldX, int worldY);

    /**
     * Reads the world x of a cell's centre.
     *
     * @param column the column
     * @param row    the row
     * @return the world x of the centre
     */
    int getCenterX(int column, int row);

    /**
     * Reads the world y of a cell's centre.
     *
     * @param column the column
     * @param row    the row
     * @return the world y of the centre
     */
    int getCenterY(int column, int row);

    /**
     * @return the number of neighbours of every cell, counting neighbours outside the grid
     */
    int getNeighbourCount();

    /**
     * Reads the column of a neighbour of a cell.
     *
     * @param column    the column of the cell
     * @param row       the row of the cell
     * @param neighbour the neighbour, {@code 0} up to {@link #getNeighbourCount()} excluded, in the topology's clockwise order
     * @return the neighbour's column, possibly outside the grid
     * @throws IndexOutOfBoundsException if the neighbour is outside its range
     */
    int getNeighbourColumn(int column, int row, int neighbour);

    /**
     * Reads the row of a neighbour of a cell.
     *
     * @param column    the column of the cell
     * @param row       the row of the cell
     * @param neighbour the neighbour, {@code 0} up to {@link #getNeighbourCount()} excluded, in the topology's clockwise order
     * @return the neighbour's row, possibly outside the grid
     * @throws IndexOutOfBoundsException if the neighbour is outside its range
     */
    int getNeighbourRow(int column, int row, int neighbour);
}
