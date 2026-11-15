package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the pixels of a platform cursor: a picture scaled by whole platform pixels, built from the detail picture of the scale.
 */
final class GlfwCursorSurfaceTest {
    private static final int RED = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;
    private static final int BLUE = 0xFF0000FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DOUBLE = 2;
    private static final int TRIPLE = 3;

    @Test
    void create_pixels_should_scale_the_picture_or_take_the_detail_of_the_scale() {
        Picture picture = new Picture(1, 1, PixelLayer.ofRgba(new int[]{RED}));
        Picture detail = new Picture(DOUBLE, DOUBLE, PixelLayer.ofRgba(new int[]{GREEN, BLUE, WHITE, RED}));
        DetailSet<Picture> pictures = new DetailSet<>(picture, List.of(detail), new int[]{DOUBLE});

        Assertions.assertArrayEquals(new int[]{RED}, GlfwCursorSurface.createPixels(pictures, 1));
        Assertions.assertArrayEquals(new int[]{GREEN, BLUE, WHITE, RED}, GlfwCursorSurface.createPixels(pictures, DOUBLE),
                                     "at two platform pixels per picture pixel the detail shows as it is");
        Assertions.assertArrayEquals(new int[]{RED, RED, RED, RED}, GlfwCursorSurface.createPixels(DetailSet.of(picture), DOUBLE),
                                     "without detail each picture pixel covers a square");
        Assertions.assertArrayEquals(new int[]{GREEN, GREEN, BLUE, GREEN, GREEN, BLUE, WHITE, WHITE, RED},
                                     GlfwCursorSurface.createPixels(pictures, TRIPLE), "the largest detail, sampled at the scale");
    }
}
