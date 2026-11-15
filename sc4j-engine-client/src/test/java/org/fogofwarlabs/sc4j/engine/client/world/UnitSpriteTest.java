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
 * Tests the frame grid and the step of a unit sprite.
 */
final class UnitSpriteTest {
    private static final int FRAME_COUNT = 6;
    private static final int ROWS = 2;
    private static final int COLUMNS = 3;
    private static final int STEP_FRAMES = 5;

    private static @NotNull DetailSet<SpriteSheet> createSheets() {
        List<SpriteFrame> frames = new ArrayList<>();

        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            frames.add(new SpriteFrame(0, 0, 1, 1, frame));
        }

        return DetailSet.of(new SpriteSheet(1, 1, 0, 0, frames, PixelLayer.ofRgba(new int[FRAME_COUNT]), null, null));
    }

    @Test
    void getters_should_return_the_grid_of_the_sheet_and_the_step() {
        UnitSprite defaults = new UnitSprite.Builder(createSheets(), ROWS).setColumns(UnitPose.IDLE, 0).build();
        UnitSprite stepped = new UnitSprite.Builder(createSheets(), ROWS).setColumns(UnitPose.IDLE, 0).setStepFrames(STEP_FRAMES).build();

        Assertions.assertEquals(ROWS, defaults.getRowCount());
        Assertions.assertEquals(COLUMNS, defaults.getColumnCount());
        Assertions.assertEquals(UnitSprite.DEFAULT_STEP_FRAMES, defaults.getStepFrames());
        Assertions.assertEquals(STEP_FRAMES, stepped.getStepFrames());
    }
}
