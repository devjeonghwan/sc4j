package org.fogofwarlabs.sc4j.engine.client.render.gl;

import org.fogofwarlabs.sc4j.engine.client.HiddenWindowScenario;
import org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Palette;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteFrame;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the OpenGL sprite batch default on a hidden window: palette colours, transparency, the team colour mask, the shadow,
 * mirroring around the anchor and the camera.
 */
final class GlSpriteBatchTest {
    private static final int SHEET_WIDTH = 4;
    private static final int SHEET_HEIGHT = 2;
    private static final int TEAM_COLOR = 0x0000FF;
    private static final int RED_PIXEL = 0xFF0000;
    private static final int GREEN_PIXEL = 0x00FF00;
    private static final int BLACK_PIXEL = 0;
    private static final byte FULL = (byte) 0xFF;
    private static final @NotNull Palette PALETTE = new Palette(new int[]{0x00000000, 0xFFFF0000, 0xFF00FF00});

    /**
     * Creates the test sheet: one 4 by 2 frame anchored at its top-left pixel, indices {@code 0 1 2 1 / 1 1 2 0}, a team colour
     * mask on the third pixel of the top row and a shadow under the bottom row.
     *
     * @return the created {@link SpriteSheet} instance
     */
    private static @NotNull SpriteSheet createSheet() {
        byte[] indices = {0, 1, 2, 1, 1, 1, 2, 0};
        byte[] mask = {0, 0, FULL, 0, 0, 0, 0, 0};
        byte[] shadow = {0, 0, 0, 0, FULL, FULL, FULL, FULL};

        return new SpriteSheet(SHEET_WIDTH, SHEET_HEIGHT, 0, 0, List.of(new SpriteFrame(0, 0, SHEET_WIDTH, SHEET_HEIGHT, 0)),
                               PixelLayer.ofIndexed(indices), PixelLayer.ofAlpha(mask), PixelLayer.ofAlpha(shadow));
    }

    @Test
    void draw_should_colour_indices_through_the_palette_and_mix_the_team_colour() {
        HiddenWindowScenario.run(() -> {
            try (GlSpriteBatch batch = new GlSpriteBatch()) {
                WorldViewport viewport = new WorldViewport(new SquareProjection());
                SpriteSheet sheet = createSheet();

                viewport.set(HiddenWindowScenario.SIZE, HiddenWindowScenario.SIZE, 0, 0, 1);
                batch.begin(viewport);
                batch.setPalette(PALETTE);
                batch.draw(sheet, 0, 10, 10, false, TEAM_COLOR);
                batch.draw(sheet, 0, 20, 20, true, TEAM_COLOR);
                batch.end();

                Assertions.assertEquals(BLACK_PIXEL, HiddenWindowScenario.readColor(10, 10), "index 0 is transparent");
                Assertions.assertEquals(RED_PIXEL, HiddenWindowScenario.readColor(11, 10));
                Assertions.assertEquals(TEAM_COLOR, HiddenWindowScenario.readColor(12, 10), "the mask takes the team colour");
                Assertions.assertEquals(GREEN_PIXEL, HiddenWindowScenario.readColor(12, 11));
                Assertions.assertEquals(BLACK_PIXEL, HiddenWindowScenario.readColor(13, 11), "the shadow is black under a transparent pixel");
                Assertions.assertEquals(BLACK_PIXEL, HiddenWindowScenario.readColor(19, 20), "mirrored: the first column ends left of the anchor");
                Assertions.assertEquals(RED_PIXEL, HiddenWindowScenario.readColor(18, 20));
                Assertions.assertEquals(TEAM_COLOR, HiddenWindowScenario.readColor(17, 20));
            }
        });
    }

    @Test
    void draw_should_place_sprites_through_the_camera_and_zoom() {
        HiddenWindowScenario.run(() -> {
            try (GlSpriteBatch batch = new GlSpriteBatch()) {
                WorldViewport viewport = new WorldViewport(new SquareProjection());

                viewport.set(HiddenWindowScenario.SIZE, HiddenWindowScenario.SIZE, 100, 200, 2);
                batch.begin(viewport);
                batch.setPalette(PALETTE);
                batch.draw(createSheet(), 0, 104, 204, false, TEAM_COLOR);
                batch.end();

                Assertions.assertEquals(RED_PIXEL, HiddenWindowScenario.readColor(10, 8));
                Assertions.assertEquals(RED_PIXEL, HiddenWindowScenario.readColor(11, 9));
                Assertions.assertEquals(TEAM_COLOR, HiddenWindowScenario.readColor(12, 8));
            }
        });
    }
}
