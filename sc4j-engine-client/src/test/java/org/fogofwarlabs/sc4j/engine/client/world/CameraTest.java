package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.projection.IsometricProjection;
import org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine camera: it stays inside the map and centres a small map, keeps the centre of the shown area over an isometric map's
 * diamond, zooms within its limits around the centre, and gives the range of its top-left corner to the camera scrolls.
 */
final class CameraTest {
    private static final int MAP_WIDTH = 2000;
    private static final int MAP_HEIGHT = 1000;
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final float DELTA = 0.001f;
    private static final int TILE_SIZE = 32;
    private static final int ISOMETRIC_TILE_WIDTH = 64;
    private static final int ISOMETRIC_TILE_HEIGHT = 32;

    @Test
    void camera_stays_inside_the_map_and_centres_a_smaller_map() {
        Camera camera = new Camera(new SquareProjection(), MAP_WIDTH, MAP_HEIGHT);

        camera.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        camera.moveTo(-50, -50);

        Assertions.assertEquals(0, camera.getX(), DELTA);
        Assertions.assertEquals(0, camera.getY(), DELTA);

        camera.centerOn(MAP_WIDTH, MAP_HEIGHT);

        Assertions.assertEquals(MAP_WIDTH - SCREEN_WIDTH, camera.getX(), DELTA);
        Assertions.assertEquals(MAP_HEIGHT - SCREEN_HEIGHT, camera.getY(), DELTA);

        camera.setZoom(0.5f);

        Assertions.assertEquals(MAP_WIDTH - 2 * SCREEN_WIDTH, camera.getX(), DELTA, "a zoom out keeps the shown area inside");
        Assertions.assertEquals((MAP_HEIGHT - 2 * SCREEN_HEIGHT) / 2f, camera.getY(), DELTA, "a map lower than the shown area is centred");

        WorldViewport viewport = new WorldViewport(new SquareProjection());

        camera.applyTo(viewport);

        Assertions.assertEquals(SCREEN_WIDTH, viewport.getWidth());
        Assertions.assertEquals(camera.getX(), viewport.getCameraX(), DELTA);
        Assertions.assertEquals(0.5f, viewport.getZoom(), DELTA);
        Assertions.assertEquals(camera.getX() + 20, camera.toWorldX(10, 10), DELTA);
    }

    @Test
    void camera_keeps_to_the_projected_diamond_of_an_isometric_map() {
        IsometricProjection projection = new IsometricProjection(TILE_SIZE, ISOMETRIC_TILE_WIDTH, ISOMETRIC_TILE_HEIGHT);
        Camera camera = new Camera(projection, MAP_WIDTH, MAP_WIDTH);

        camera.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        Assertions.assertEquals(-MAP_WIDTH, camera.getBoundsLeft(), DELTA);
        Assertions.assertEquals(MAP_WIDTH, camera.getBoundsRight(), DELTA);
        Assertions.assertEquals(MAP_WIDTH, camera.getBoundsBottom(), DELTA);

        camera.moveTo(-10 * MAP_WIDTH, -10 * MAP_WIDTH);

        float centerX = camera.toWorldX(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f);
        float centerY = camera.toWorldY(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f);

        Assertions.assertTrue(camera.getX() >= -MAP_WIDTH && camera.getY() >= 0, "the shown area stays inside the bounds");
        Assertions.assertTrue(centerX >= -DELTA && centerX <= MAP_WIDTH + DELTA && centerY >= -DELTA && centerY <= MAP_WIDTH + DELTA,
                              "the centre of the shown area stays over the diamond at the corner of its box");
        Assertions.assertTrue(camera.getX() > -MAP_WIDTH, "which moves the camera in from the box's corner");

        camera.centerOn(MAP_WIDTH / 2f, MAP_WIDTH / 2f);

        Assertions.assertEquals(0, camera.getX() + camera.getViewWidth() / 2, DELTA, "the map centre is below the top corner");
        Assertions.assertEquals(MAP_WIDTH / 2f, camera.getY() + camera.getViewHeight() / 2, DELTA);
        Assertions.assertEquals(MAP_WIDTH / 2f, camera.toWorldX(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f), DELTA);
        Assertions.assertEquals(MAP_WIDTH / 2f, camera.toWorldY(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f), DELTA);

        WorldViewport viewport = new WorldViewport(projection);

        camera.applyTo(viewport);

        Assertions.assertEquals(SCREEN_WIDTH / 2f, viewport.worldToScreenX(MAP_WIDTH / 2f, MAP_WIDTH / 2f), DELTA);
        Assertions.assertEquals(SCREEN_HEIGHT / 2f, viewport.worldToScreenY(MAP_WIDTH / 2f, MAP_WIDTH / 2f), DELTA);
    }

    @Test
    void zoom_stays_within_limits_and_keeps_the_centre() {
        Camera camera = new Camera(new SquareProjection(), MAP_WIDTH, MAP_WIDTH);

        camera.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        camera.centerOn(1000, 1000);
        camera.setZoom(10);

        Assertions.assertEquals(Camera.DEFAULT_MAXIMUM_ZOOM, camera.getZoom(), DELTA);
        Assertions.assertEquals(1000, camera.getX() + camera.getViewWidth() / 2, DELTA);
        Assertions.assertEquals(1000, camera.getY() + camera.getViewHeight() / 2, DELTA);

        camera.setZoomLimits(1, 1.5f);

        Assertions.assertEquals(1.5f, camera.getZoom(), DELTA);
        Assertions.assertThrows(IllegalArgumentException.class, () -> camera.setZoomLimits(2, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> camera.setZoom(0));
    }

    @Test
    void camera_gives_the_range_of_its_top_left_corner() {
        Camera camera = new Camera(new SquareProjection(), MAP_WIDTH, MAP_HEIGHT);

        camera.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        Assertions.assertEquals(0, camera.getMinimumX(), DELTA);
        Assertions.assertEquals(MAP_WIDTH - SCREEN_WIDTH, camera.getMaximumX(), DELTA);
        Assertions.assertEquals(0, camera.getMinimumY(), DELTA);
        Assertions.assertEquals(MAP_HEIGHT - SCREEN_HEIGHT, camera.getMaximumY(), DELTA);

        camera.setZoom(0.5f);

        Assertions.assertEquals(MAP_WIDTH - 2 * SCREEN_WIDTH, camera.getMaximumX(), DELTA, "the range follows the zoom");
        Assertions.assertEquals((MAP_HEIGHT - 2 * SCREEN_HEIGHT) / 2f, camera.getMinimumY(), DELTA, "a map lower than the shown area is centred");
        Assertions.assertEquals(camera.getMinimumY(), camera.getMaximumY(), DELTA);

        camera.moveTo(camera.getMaximumX() + 1, 0);

        Assertions.assertEquals(camera.getMaximumX(), camera.getX(), DELTA);
        Assertions.assertEquals(camera.getMinimumY(), camera.getY(), DELTA);
    }
}
