package org.fogofwarlabs.sc4j.engine.client.render.gl;

import org.fogofwarlabs.sc4j.engine.client.HiddenWindowScenario;
import org.fogofwarlabs.sc4j.engine.client.render.Texture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the OpenGL canvas default on a hidden window: filled rectangles, the clip, uploaded textures and lines.
 */
final class GlCanvasTest {
    private static final int FIELD_PAGE_SIZE = 64;
    private static final int RED = 0xFF0000FF;
    private static final int GREEN = 0x00FF00FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int RED_PIXEL = 0xFF0000;
    private static final int GREEN_PIXEL = 0x00FF00;
    private static final int BLUE_PIXEL = 0x0000FF;
    private static final int WHITE_PIXEL = 0xFFFFFF;
    private static final int BLACK_PIXEL = 0;
    private static final int RED_ARGB = 0xFFFF0000;
    private static final int BLUE_ARGB = 0xFF0000FF;

    @Test
    void fill_rectangle_should_draw_inside_the_clip_only() {
        HiddenWindowScenario.run(() -> {
            try (GlCanvas canvas = new GlCanvas(FIELD_PAGE_SIZE)) {
                canvas.begin(HiddenWindowScenario.SIZE, HiddenWindowScenario.SIZE);
                canvas.fillRectangle(2, 2, 4, 4, RED);
                canvas.setClip(10, 10, 4, 4);
                canvas.fillRectangle(8, 8, 8, 8, GREEN);
                canvas.setClip(0, 0, -1, -1);
                canvas.drawLine(20, 25, 30, 25, 2, WHITE);
                canvas.end();

                Assertions.assertEquals(HiddenWindowScenario.SIZE, canvas.getWidth());
                Assertions.assertEquals(RED_PIXEL, HiddenWindowScenario.readColor(3, 3));
                Assertions.assertEquals(BLACK_PIXEL, HiddenWindowScenario.readColor(6, 6));
                Assertions.assertEquals(GREEN_PIXEL, HiddenWindowScenario.readColor(11, 11));
                Assertions.assertEquals(BLACK_PIXEL, HiddenWindowScenario.readColor(9, 9));
                Assertions.assertEquals(WHITE_PIXEL, HiddenWindowScenario.readColor(25, 25));
            }
        });
    }

    @Test
    void upload_texture_should_draw_the_resource_model_colors() {
        HiddenWindowScenario.run(() -> {
            try (GlCanvas canvas = new GlCanvas(FIELD_PAGE_SIZE)) {
                Texture texture = canvas.uploadTexture(2, 1, new int[]{RED_ARGB, BLUE_ARGB});

                canvas.begin(HiddenWindowScenario.SIZE, HiddenWindowScenario.SIZE);
                canvas.drawTexture(texture, 0, 0, 8, 4, WHITE);
                canvas.end();

                Assertions.assertEquals(2, texture.getWidth());
                Assertions.assertEquals(RED_PIXEL, HiddenWindowScenario.readColor(1, 1));
                Assertions.assertEquals(BLUE_PIXEL, HiddenWindowScenario.readColor(6, 2));

                texture.close();
                texture.close();
            }
        });
    }
}
