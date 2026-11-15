package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.console.ConsoleCommandRegistry;
import org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the engine camera slash commands: {@code /zoom} and {@code /camera} change the camera, {@code /edgescroll} and {@code /keyscroll} its
 * scroll and continuous rate, and all are listed with their help.
 */
final class CameraCommandUtilsTest {
    private static final int MAP_SIZE = 2000;
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final float DELTA = 0.001f;
    private static final int PAGE_LINES = 100;
    private static final float KEY_SCROLL_SPEED = 1500;

    @Test
    void register_should_add_camera_commands_that_change_the_camera() {
        Camera camera = new Camera(new SquareProjection(), MAP_SIZE, MAP_SIZE);
        ContinuousScrollRate rate = new ContinuousScrollRate();
        CameraScroll scroll = new CameraScroll(CameraScroll.DEFAULT_EDGE_WIDTH, rate);
        ConsoleCommandRegistry registry = new ConsoleCommandRegistry(PAGE_LINES);

        camera.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        CameraCommandUtils.register(registry, camera, scroll, rate);

        Assertions.assertEquals("Zoom 150%, limits 50% to 200%.", registry.run("/zoom 150"));
        Assertions.assertEquals("Zoom 100%, limits 50% to 200%.", registry.run("/zoom reset"));
        Assertions.assertEquals("Zoom 100%, limits 80% to 120%.", registry.run("/zoom limits 80 120"));
        Assertions.assertEquals("Zoom 120%, limits 80% to 120%.", registry.run("/zoom 400"));
        Assertions.assertEquals("Not a zoom percent: x.", registry.run("/zoom x"));
        Assertions.assertTrue(registry.run("/camera 1000 1000").startsWith("Camera at "));
        Assertions.assertEquals(1000, camera.getX() + camera.getViewWidth() / 2, DELTA);
        Assertions.assertEquals("Edge scrolling off, 900 px/s within 4 px of an edge.", registry.run("/edgescroll off"));
        Assertions.assertFalse(scroll.isEdgeScrolling());
        Assertions.assertEquals("Edge scrolling off, 600 px/s within 4 px of an edge.", registry.run("/edgescroll 600"));
        Assertions.assertEquals("Key scrolling on, 900 px/s.", registry.run("/keyscroll"));
        Assertions.assertEquals("Key scrolling on, 1500 px/s.", registry.run("/keyscroll 1500"));
        Assertions.assertEquals("Key scrolling off, 1500 px/s.", registry.run("/keyscroll off"));
        Assertions.assertFalse(scroll.isKeyScrolling());
        Assertions.assertEquals(KEY_SCROLL_SPEED, rate.getKeySpeed(), DELTA);
        Assertions.assertEquals("Not a key scroll speed: fast.", registry.run("/keyscroll fast"));

        List<String> help = registry.getCommandHelpLines("zoom");

        Assertions.assertTrue(help.stream().anyMatch(line -> line.contains("limits <minimum> <maximum>")), String.join("\n", help));
        Assertions.assertTrue(registry.getHelpLines().stream().anyMatch(line -> line.startsWith("/edgescroll")));
    }
}
