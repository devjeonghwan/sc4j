package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.command.CommandTarget;
import org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests that the world picker picks by its pick order: a game's own order decides the picked unit, the local player's sight still applies.
 */
final class WorldPickerTest {
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final int KEEP_X = 208;
    private static final int KEEP_Y = 208;
    private static final float GROUND_X = 700;
    private static final float GROUND_Y = 500;
    private static final int FRAMES = 4;

    @Test
    void pick_should_use_the_pick_order() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        GameView view = skirmish.getSession().getView();

        skirmish.runFrames(FRAMES);

        Camera camera = new Camera(new SquareProjection(), view.getMapWidth(), view.getMapHeight());
        Unit keep = view.findUnitAt(KEEP_X, KEEP_Y);

        Assertions.assertNotNull(keep);

        WorldPicker picker = new WorldPicker(view, camera, (x, y) -> keep);

        camera.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        CommandTarget target = picker.pick(GROUND_X, GROUND_Y);

        Assertions.assertNotNull(target);
        Assertions.assertSame(keep, target.getUnit(), "the order picks the keep anywhere");
        Assertions.assertNull(new WorldPicker(view, camera, (x, y) -> null).pickUnit(KEEP_X, KEEP_Y), "an order that picks nothing");
        Assertions.assertSame(keep, new WorldPicker(view, camera).pickUnit(KEEP_X, KEEP_Y), "the default order is the view's");
    }
}
