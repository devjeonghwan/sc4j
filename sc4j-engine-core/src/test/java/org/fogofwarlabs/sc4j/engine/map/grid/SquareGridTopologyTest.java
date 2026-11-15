package org.fogofwarlabs.sc4j.engine.map.grid;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the square grid over the world plane: cell lookups of world positions, cell centres and the clockwise neighbours.
 */
final class SquareGridTopologyTest {
    private static final int CELL_SIZE = 32;
    private static final int COLUMNS = 4;
    private static final int ROWS = 3;

    @Test
    void square_grid_finds_the_cell_of_a_world_position_and_its_centre() {
        SquareGridTopology grid = new SquareGridTopology(CELL_SIZE, COLUMNS, ROWS);

        Assertions.assertEquals(COLUMNS * CELL_SIZE, grid.getWorldWidth());
        Assertions.assertEquals(ROWS * CELL_SIZE, grid.getWorldHeight());

        for (int worldY = -CELL_SIZE; worldY < grid.getWorldHeight() + CELL_SIZE; worldY++) {
            for (int worldX = -CELL_SIZE; worldX < grid.getWorldWidth() + CELL_SIZE; worldX++) {
                int column = grid.findColumn(worldX, worldY);
                int row = grid.findRow(worldX, worldY);

                Assertions.assertEquals(Math.floorDiv(worldX, CELL_SIZE), column);
                Assertions.assertEquals(Math.floorDiv(worldY, CELL_SIZE), row);
                Assertions.assertEquals(worldX >= 0 && worldY >= 0 && worldX < grid.getWorldWidth() && worldY < grid.getWorldHeight(),
                                        grid.contains(column, row));
            }
        }

        Assertions.assertEquals(2 * CELL_SIZE + CELL_SIZE / 2, grid.getCenterX(2, 1));
        Assertions.assertEquals(CELL_SIZE + CELL_SIZE / 2, grid.getCenterY(2, 1));
        Assertions.assertEquals(2, grid.findColumn(grid.getCenterX(2, 1), grid.getCenterY(2, 1)));
        Assertions.assertEquals(1, grid.findRow(grid.getCenterX(2, 1), grid.getCenterY(2, 1)));
    }

    @Test
    void square_grid_lists_eight_neighbours_clockwise_from_the_top() {
        SquareGridTopology grid = new SquareGridTopology(CELL_SIZE, COLUMNS, ROWS);
        int[][] expected = {{1, 0}, {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {0, 1}, {0, 0}};

        Assertions.assertEquals(expected.length, grid.getNeighbourCount());

        for (int neighbour = 0; neighbour < expected.length; neighbour++) {
            Assertions.assertEquals(expected[neighbour][0], grid.getNeighbourColumn(1, 1, neighbour));
            Assertions.assertEquals(expected[neighbour][1], grid.getNeighbourRow(1, 1, neighbour));
        }

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> grid.getNeighbourColumn(1, 1, expected.length));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SquareGridTopology(0, COLUMNS, ROWS));
    }
}
