package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteFrame;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the steps of a projectile sprite: the game loops per flight column and per impact frame.
 */
final class ProjectileSpriteTest {
    private static final int FRAME_COUNT = 4;
    private static final int ROWS = 2;
    private static final int STEP_FRAMES = 3;
    private static final int IMPACT_STEP_FRAMES = 2;

    private static @NotNull DetailSet<SpriteSheet> createSheets() {
        List<SpriteFrame> frames = new ArrayList<>();

        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            frames.add(new SpriteFrame(0, 0, 1, 1, frame));
        }

        return DetailSet.of(new SpriteSheet(1, 1, 0, 0, frames, PixelLayer.ofRgba(new int[FRAME_COUNT]), null, null));
    }

    @Test
    void getters_should_return_the_steps_of_the_flight_and_the_impact() {
        ProjectileSprite sprite = new ProjectileSprite(createSheets(), ROWS, STEP_FRAMES, null, IMPACT_STEP_FRAMES);

        Assertions.assertEquals(STEP_FRAMES, sprite.getStepFrames());
        Assertions.assertEquals(IMPACT_STEP_FRAMES, sprite.getImpactStepFrames());
        Assertions.assertNull(sprite.getImpactSheet());
    }
}
