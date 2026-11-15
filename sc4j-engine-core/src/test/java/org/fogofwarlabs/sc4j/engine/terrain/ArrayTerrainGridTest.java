package org.fogofwarlabs.sc4j.engine.terrain;

import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine default terrain storage: walk grid dimensions, cells outside the map, the tile and walk grids, and the state hash of
 * terrain changes.
 */
final class ArrayTerrainGridTest {
    private static final int TILE_SIZE = 32;
    private static final int WALK_CELL_SIZE = 8;
    private static final int WIDTH = 3;
    private static final int HEIGHT = 2;

    @Test
    void walk_grid_has_the_configured_resolution() {
        ArrayTerrainGrid terrain = new ArrayTerrainGrid(TILE_SIZE, WALK_CELL_SIZE, WIDTH, HEIGHT);

        Assertions.assertEquals(12, terrain.getWalkWidth());
        Assertions.assertEquals(8, terrain.getWalkHeight());
        Assertions.assertFalse(terrain.isWalkable(0, 0));
        Assertions.assertFalse(terrain.isBuildable(0, 0));

        terrain.setWalkable(11, 7, true);
        terrain.setElevation(11, 7, 2);
        terrain.setBuildable(2, 1, true);

        Assertions.assertTrue(terrain.isWalkable(11, 7));
        Assertions.assertEquals(2, terrain.getElevation(11, 7));
        Assertions.assertTrue(terrain.isBuildable(2, 1));
        Assertions.assertFalse(terrain.isWalkable(12, 7));
        Assertions.assertFalse(terrain.isWalkable(-1, 0));
        Assertions.assertEquals(0, terrain.getElevation(11, 8));
        Assertions.assertFalse(terrain.isBuildable(3, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> terrain.setWalkable(12, 0, true));
        Assertions.assertThrows(IllegalArgumentException.class, () -> terrain.setElevation(0, 0, ArrayTerrainGrid.MAXIMUM_ELEVATION + 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ArrayTerrainGrid(TILE_SIZE, 5, WIDTH, HEIGHT));
    }

    @Test
    void terrain_grid_gives_its_tile_and_walk_grids() {
        ArrayTerrainGrid terrain = new ArrayTerrainGrid(TILE_SIZE, WALK_CELL_SIZE, WIDTH, HEIGHT);
        GridTopology tiles = terrain.getTileGrid();
        GridTopology walkCells = terrain.getWalkGrid();

        Assertions.assertSame(tiles, terrain.getTileGrid());
        Assertions.assertEquals(WIDTH, tiles.getColumnCount());
        Assertions.assertEquals(HEIGHT, tiles.getRowCount());
        Assertions.assertEquals(terrain.getWalkWidth(), walkCells.getColumnCount());
        Assertions.assertEquals(terrain.getWalkHeight(), walkCells.getRowCount());
        Assertions.assertEquals(tiles.getWorldWidth(), walkCells.getWorldWidth());
        Assertions.assertEquals(WALK_CELL_SIZE, walkCells.getCellWidth());
    }

    @Test
    void terrain_changes_change_the_state_hash() {
        ArrayTerrainGrid terrain = new ArrayTerrainGrid(TILE_SIZE, WALK_CELL_SIZE, WIDTH, HEIGHT);
        StateHash before = new StateHash();
        StateHash after = new StateHash();

        terrain.contributeStateHash(before);
        terrain.setWalkable(1, 1, true);
        terrain.contributeStateHash(after);

        Assertions.assertNotEquals(before.getValue(), after.getValue());
    }
}
