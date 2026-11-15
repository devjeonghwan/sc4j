package org.fogofwarlabs.sc4j.engine.client.render;

import org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the world view's layer order and the viewport's world to screen mapping.
 */
final class WorldViewTest {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private static final float CAMERA_X = 96;
    private static final float CAMERA_Y = 64;
    private static final float ZOOM = 2;

    @Test
    void draw_should_run_the_layers_bottom_first_with_the_frame_viewport() throws IOException {
        List<String> drawn = new ArrayList<>();
        WorldViewport viewport = new WorldViewport(new SquareProjection());
        WorldView view = new WorldView(List.of(frame -> drawn.add("terrain " + frame.getWidth()),
                                               frame -> drawn.add("sprites " + frame.getZoom()),
                                               frame -> drawn.add("fog")));

        viewport.set(WIDTH, HEIGHT, CAMERA_X, CAMERA_Y, ZOOM);
        view.draw(viewport);

        Assertions.assertEquals(List.of("terrain 640", "sprites 2.0", "fog"), drawn);
        Assertions.assertEquals(3, view.getLayers().size());
    }

    @Test
    void viewport_maps_world_positions_through_the_camera_and_zoom() {
        WorldViewport viewport = new WorldViewport(new SquareProjection());

        viewport.set(WIDTH, HEIGHT, CAMERA_X, CAMERA_Y, ZOOM);

        Assertions.assertEquals(8, viewport.toScreenX(100));
        Assertions.assertEquals(-16, viewport.toScreenY(56));
        Assertions.assertThrows(IllegalArgumentException.class, () -> viewport.set(0, HEIGHT, 0, 0, 1));
    }
}
