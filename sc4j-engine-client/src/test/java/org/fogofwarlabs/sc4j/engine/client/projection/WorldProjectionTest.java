package org.fogofwarlabs.sc4j.engine.client.projection;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine world projections: world to screen to world round trips, the isometric diamond of a cell, depth order, map bounds and
 * facing directions, and the identity of the top-down square projection.
 */
final class WorldProjectionTest {
    private static final int CELL_SIZE = 32;
    private static final int TILE_WIDTH = 64;
    private static final int TILE_HEIGHT = 32;
    private static final int MAP_SIZE = 2048;
    private static final int STEP = 37;
    private static final float DELTA = 0.01f;
    private static final int RIGHT = 64;
    private static final int DOWN = 128;
    private static final int LEFT = 192;
    private static final int DOWN_RIGHT = 96;
    private static final int DOWN_LEFT = 160;
    private static final int UP_LEFT = 224;
    private static final int UP_RIGHT = 32;

    private static void assertRoundTrip(@NotNull WorldProjection projection) {
        for (int worldY = -MAP_SIZE; worldY <= MAP_SIZE; worldY += STEP) {
            for (int worldX = -MAP_SIZE; worldX <= MAP_SIZE; worldX += STEP) {
                float projectedX = projection.toProjectedX(worldX, worldY);
                float projectedY = projection.toProjectedY(worldX, worldY);

                Assertions.assertEquals(worldX, projection.toWorldX(projectedX, projectedY), DELTA);
                Assertions.assertEquals(worldY, projection.toWorldY(projectedX, projectedY), DELTA);
            }
        }
    }

    @Test
    void square_projection_shows_the_world_plane_as_it_is() {
        SquareProjection projection = new SquareProjection();
        float[] corners = new float[WorldProjection.OUTLINE_LENGTH];

        assertRoundTrip(projection);
        Assertions.assertEquals(100, projection.toProjectedX(100, 200));
        Assertions.assertEquals(200, projection.toProjectedY(100, 200));
        Assertions.assertTrue(projection.getDepth(0, 10) > projection.getDepth(1000, 9), "lower on the screen draws later");
        Assertions.assertEquals(0, projection.getProjectedLeft(MAP_SIZE, MAP_SIZE / 2f));
        Assertions.assertEquals(0, projection.getProjectedTop(MAP_SIZE, MAP_SIZE / 2f));
        Assertions.assertEquals(MAP_SIZE, projection.getProjectedRight(MAP_SIZE, MAP_SIZE / 2f));
        Assertions.assertEquals(MAP_SIZE / 2f, projection.getProjectedBottom(MAP_SIZE, MAP_SIZE / 2f));

        projection.outlineFootprint(1, 2, 3, 4, corners);

        Assertions.assertArrayEquals(new float[]{1, 2, 3, 2, 3, 4, 1, 4}, corners);

        for (int direction = 0; direction < WorldProjection.DIRECTION_COUNT; direction++) {
            Assertions.assertEquals(direction, projection.toScreenDirection(direction));
        }
    }

    @Test
    void isometric_projection_shows_a_cell_as_a_diamond_and_round_trips() {
        IsometricProjection projection = new IsometricProjection(CELL_SIZE, TILE_WIDTH, TILE_HEIGHT);
        float[] corners = new float[WorldProjection.OUTLINE_LENGTH];
        float centerX = projection.toProjectedX(CELL_SIZE * 2.5f, CELL_SIZE * 1.5f);
        float centerY = projection.toProjectedY(CELL_SIZE * 2.5f, CELL_SIZE * 1.5f);

        assertRoundTrip(projection);
        projection.outlineFootprint(CELL_SIZE * 2, CELL_SIZE, CELL_SIZE * 3, CELL_SIZE * 2, corners);

        Assertions.assertEquals(centerX, corners[0], DELTA, "the world top-left corner is the diamond's top");
        Assertions.assertEquals(centerY - TILE_HEIGHT / 2f, corners[1], DELTA);
        Assertions.assertEquals(centerX + TILE_WIDTH / 2f, corners[2], DELTA, "the world top-right corner is the diamond's right");
        Assertions.assertEquals(centerY, corners[3], DELTA);
        Assertions.assertEquals(centerX, corners[4], DELTA, "the world bottom-right corner is the diamond's bottom");
        Assertions.assertEquals(centerY + TILE_HEIGHT / 2f, corners[5], DELTA);
        Assertions.assertEquals(centerX - TILE_WIDTH / 2f, corners[6], DELTA, "the world bottom-left corner is the diamond's left");
        Assertions.assertEquals(centerY, corners[7], DELTA);
    }

    @Test
    void isometric_projection_orders_depth_down_the_screen_and_bounds_the_map_diamond() {
        IsometricProjection projection = new IsometricProjection(CELL_SIZE, TILE_WIDTH, TILE_HEIGHT);

        for (int worldY = 0; worldY < MAP_SIZE; worldY += STEP) {
            for (int worldX = 0; worldX < MAP_SIZE; worldX += STEP) {
                boolean lower = projection.toProjectedY(worldX + STEP, worldY) > projection.toProjectedY(worldX, worldY);

                Assertions.assertEquals(lower, projection.getDepth(worldX + STEP, worldY) > projection.getDepth(worldX, worldY));
            }
        }

        Assertions.assertEquals(-MAP_SIZE, projection.getProjectedLeft(MAP_SIZE, MAP_SIZE), DELTA);
        Assertions.assertEquals(0, projection.getProjectedTop(MAP_SIZE, MAP_SIZE), DELTA);
        Assertions.assertEquals(MAP_SIZE, projection.getProjectedRight(MAP_SIZE, MAP_SIZE), DELTA);
        Assertions.assertEquals(MAP_SIZE, projection.getProjectedBottom(MAP_SIZE, MAP_SIZE), DELTA);
    }

    @Test
    void isometric_projection_turns_world_facings_by_an_eighth() {
        IsometricProjection projection = new IsometricProjection(CELL_SIZE, TILE_WIDTH, TILE_HEIGHT);

        Assertions.assertEquals(CELL_SIZE, projection.getCellSize());
        Assertions.assertEquals(TILE_WIDTH, projection.getTileWidth());
        Assertions.assertEquals(TILE_HEIGHT, projection.getTileHeight());

        Assertions.assertEquals(RIGHT, projection.toScreenDirection(UP_RIGHT), "world up-right shows to the right");
        Assertions.assertEquals(DOWN, projection.toScreenDirection(DOWN_RIGHT), "world down-right shows downward");
        Assertions.assertEquals(LEFT, projection.toScreenDirection(DOWN_LEFT), "world down-left shows to the left");
        Assertions.assertEquals(0, projection.toScreenDirection(UP_LEFT), "world up-left shows upward");
        Assertions.assertTrue(projection.toScreenDirection(0) > 0 && projection.toScreenDirection(0) < RIGHT, "world up shows up-right");
        Assertions.assertTrue(projection.toScreenDirection(RIGHT) > RIGHT && projection.toScreenDirection(RIGHT) < DOWN,
                              "world right shows down-right");
    }
}
