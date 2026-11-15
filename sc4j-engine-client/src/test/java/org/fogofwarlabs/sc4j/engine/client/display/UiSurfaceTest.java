package org.fogofwarlabs.sc4j.engine.client.display;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the logical surface of a framebuffer at a display scale: the logical size, the pixel scale, the mapping of positions both ways and the
 * fit to a larger logical area.
 */
final class UiSurfaceTest {
    private static final int LOGICAL_WIDTH = 1280;
    private static final int LOGICAL_HEIGHT = 720;
    private static final float POINT_X = 640.5f;
    private static final float POINT_Y = 200.25f;
    private static final float TOLERANCE = 0.001f;
    private static final float USER_SCALE = 1.25f;

    @ParameterizedTest
    @ValueSource(floats = {1.0f, 1.5f, 2.0f})
    void surface_maps_logical_units_onto_the_framebuffer_at_the_display_scale(float scale) {
        int framebufferWidth = Math.round(LOGICAL_WIDTH * scale);
        int framebufferHeight = Math.round(LOGICAL_HEIGHT * scale);
        UiSurface surface = UiSurface.of(framebufferWidth, framebufferHeight, DisplayScale.of(scale, 1));

        Assertions.assertEquals(LOGICAL_WIDTH, surface.getWidth());
        Assertions.assertEquals(LOGICAL_HEIGHT, surface.getHeight());
        Assertions.assertEquals(scale, surface.getPixelScaleX(), TOLERANCE);
        Assertions.assertEquals(scale, surface.getPixelScaleY(), TOLERANCE);
        Assertions.assertEquals(scale == 1, surface.isPixelExact());
        Assertions.assertEquals(POINT_X, surface.toLogicalX(POINT_X * scale), TOLERANCE);
        Assertions.assertEquals(POINT_Y, surface.toLogicalY(POINT_Y * scale), TOLERANCE);
        Assertions.assertEquals(POINT_X * scale, surface.toFramebufferX(POINT_X), TOLERANCE);
        Assertions.assertEquals(POINT_Y * scale, surface.toFramebufferY(POINT_Y), TOLERANCE);
    }

    @Test
    void surface_multiplies_the_content_scale_with_the_user_scale() {
        DisplayScale scale = DisplayScale.of(2, DisplayScale.parseUserScale("125"));
        UiSurface surface = UiSurface.of(Math.round(LOGICAL_WIDTH * 2 * USER_SCALE), Math.round(LOGICAL_HEIGHT * 2 * USER_SCALE), scale);

        Assertions.assertEquals(2 * USER_SCALE, scale.getScale(), TOLERANCE);
        Assertions.assertEquals(3, scale.getWholeScale());
        Assertions.assertEquals(LOGICAL_WIDTH, surface.getWidth());
        Assertions.assertEquals(LOGICAL_HEIGHT, surface.getHeight());
        Assertions.assertEquals(1, DisplayScale.ONE.getScale());
    }

    @Test
    void surface_keeps_at_least_one_unit_and_rejects_invalid_values() {
        Assertions.assertEquals(1, UiSurface.of(1, 1, DisplayScale.of(2, 2)).getWidth());
        Assertions.assertThrows(IllegalArgumentException.class, () -> UiSurface.of(0, 1, DisplayScale.ONE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> DisplayScale.of(0, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> DisplayScale.of(Float.NaN, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> DisplayScale.parseUserScale("90"));
    }

    @Test
    void fit_to_should_lower_the_scale_until_the_area_fits_and_keep_a_large_enough_surface() {
        UiSurface surface = UiSurface.of(LOGICAL_WIDTH * 2, LOGICAL_HEIGHT * 2, DisplayScale.of(2, DisplayScale.parseUserScale("125")));
        UiSurface fitted = surface.fitTo(UiSurface.MINIMUM_WIDTH * 2, UiSurface.MINIMUM_HEIGHT);
        UiSurface tall = surface.fitTo(UiSurface.MINIMUM_WIDTH, LOGICAL_HEIGHT);

        Assertions.assertSame(surface, surface.fitTo(UiSurface.MINIMUM_WIDTH, UiSurface.MINIMUM_HEIGHT), "a large enough surface stays");
        Assertions.assertEquals(UiSurface.MINIMUM_WIDTH * 2, fitted.getWidth(), "the narrow axis fits exactly");
        Assertions.assertEquals(UiSurface.MINIMUM_HEIGHT * 2, fitted.getHeight(), "the other axis keeps the proportions");
        Assertions.assertEquals(surface.getFramebufferWidth(), fitted.getFramebufferWidth());
        Assertions.assertEquals(2, fitted.getScale().getContentScale(), TOLERANCE);
        Assertions.assertEquals(LOGICAL_WIDTH, tall.getWidth());
        Assertions.assertEquals(LOGICAL_HEIGHT, tall.getHeight());
    }
}
