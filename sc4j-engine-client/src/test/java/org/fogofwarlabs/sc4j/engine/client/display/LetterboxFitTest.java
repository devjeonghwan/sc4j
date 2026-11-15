package org.fogofwarlabs.sc4j.engine.client.display;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the fit of a fixed design area into a logical area: the aspect ratio is kept, the area is centred or aligned with bars, points map both
 * ways, and a fit on a scaled interface surface is the fit to its framebuffer.
 */
final class LetterboxFitTest {
    private static final float DESIGN_WIDTH = 640;
    private static final float DESIGN_HEIGHT = 480;
    private static final float WIDE_WIDTH = 1280;
    private static final float WIDE_HEIGHT = 720;
    private static final float WIDE_SCALE = 1.5f;
    private static final float SIDE_BAR = 160;
    private static final float DESIGN_POINT = 320;
    private static final float TOLERANCE = 0.001f;
    private static final int FRAMEBUFFER_WIDTH = 2560;
    private static final int FRAMEBUFFER_HEIGHT = 1440;
    private static final float DOUBLE_SCALE = 2;
    private static final float THREE_HALVES_SCALE = 1.5f;
    /**
     * A framebuffer whose logical size at {@code 1.5} is rounded ({@code 667x501} units).
     */
    private static final int ODD_WIDTH = 1001;
    private static final int ODD_HEIGHT = 751;

    @Test
    void fit_centres_the_design_area_between_side_bars_in_a_wide_area() {
        LetterboxFit fit = LetterboxFit.of(WIDE_WIDTH, WIDE_HEIGHT, DESIGN_WIDTH, DESIGN_HEIGHT);

        Assertions.assertEquals(WIDE_SCALE, fit.getScale(), TOLERANCE);
        Assertions.assertEquals(SIDE_BAR, fit.getLeft(), TOLERANCE);
        Assertions.assertEquals(0, fit.getTop(), TOLERANCE);
        Assertions.assertEquals(WIDE_WIDTH - SIDE_BAR, fit.toLogicalX(DESIGN_WIDTH), TOLERANCE);
        Assertions.assertEquals(WIDE_HEIGHT, fit.toLogicalY(DESIGN_HEIGHT), TOLERANCE);
        Assertions.assertEquals(DESIGN_POINT, fit.toDesignX(fit.toLogicalX(DESIGN_POINT)), TOLERANCE);
        Assertions.assertTrue(fit.toDesignX(0) < 0, "a point in the bar is outside the design area");
    }

    @Test
    void fit_puts_bars_above_and_below_in_a_tall_area() {
        LetterboxFit fit = LetterboxFit.of(DESIGN_WIDTH, DESIGN_WIDTH, DESIGN_WIDTH, DESIGN_HEIGHT);

        Assertions.assertEquals(1, fit.getScale(), TOLERANCE);
        Assertions.assertEquals(0, fit.getLeft(), TOLERANCE);
        Assertions.assertEquals((DESIGN_WIDTH - DESIGN_HEIGHT) / 2, fit.getTop(), TOLERANCE);
        Assertions.assertEquals(DESIGN_POINT, fit.toDesignY(fit.toLogicalY(DESIGN_POINT)), TOLERANCE);
        Assertions.assertThrows(IllegalArgumentException.class, () -> LetterboxFit.of(0, 1, 1, 1));
    }

    @Test
    void fit_aligned_to_the_end_stands_on_the_bottom_edge() {
        LetterboxFit fit = LetterboxFit.of(DESIGN_WIDTH, DESIGN_WIDTH, DESIGN_WIDTH, DESIGN_HEIGHT, LetterboxFit.CENTER, LetterboxFit.END);

        Assertions.assertEquals(0, fit.getLeft(), TOLERANCE);
        Assertions.assertEquals(DESIGN_WIDTH - DESIGN_HEIGHT, fit.getTop(), TOLERANCE);
        Assertions.assertEquals(DESIGN_WIDTH, fit.toLogicalY(DESIGN_HEIGHT), TOLERANCE);
        Assertions.assertEquals(0, LetterboxFit.of(WIDE_WIDTH, WIDE_HEIGHT, DESIGN_WIDTH, DESIGN_HEIGHT, LetterboxFit.START, LetterboxFit.START)
                                               .getLeft());
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> LetterboxFit.of(WIDE_WIDTH, WIDE_HEIGHT, DESIGN_WIDTH, DESIGN_HEIGHT, LetterboxFit.END + 1, LetterboxFit.CENTER));
    }

    @Test
    void to_framebuffer_should_give_the_fit_to_the_framebuffer_at_any_display_scale() {
        LetterboxFit framebufferFit = LetterboxFit.of(FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT, DESIGN_WIDTH, DESIGN_HEIGHT);

        // At one pixel per unit the fit on the surface is the fit to the framebuffer itself.
        UiSurface exact = UiSurface.of(FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT, DisplayScale.ONE);
        LetterboxFit exactFit = LetterboxFit.of(exact, DESIGN_WIDTH, DESIGN_HEIGHT, LetterboxFit.CENTER, LetterboxFit.CENTER);

        Assertions.assertSame(exactFit, exactFit.toFramebuffer(exact));
        Assertions.assertEquals(framebufferFit.getScale(), exactFit.getScale());
        Assertions.assertEquals(framebufferFit.getLeft(), exactFit.getLeft());

        // At 2 the logical fit is half the framebuffer fit, and doubled back bit for bit.
        UiSurface doubled = UiSurface.of(FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT, DisplayScale.of(DOUBLE_SCALE, 1));
        LetterboxFit doubledFit = LetterboxFit.of(doubled, DESIGN_WIDTH, DESIGN_HEIGHT, LetterboxFit.CENTER, LetterboxFit.CENTER);

        Assertions.assertEquals(framebufferFit.getScale() / DOUBLE_SCALE, doubledFit.getScale());
        Assertions.assertEquals(framebufferFit.getLeft() / DOUBLE_SCALE, doubledFit.getLeft());
        Assertions.assertEquals(framebufferFit.getScale(), doubledFit.toFramebuffer(doubled).getScale());
        Assertions.assertEquals(framebufferFit.getLeft(), doubledFit.toFramebuffer(doubled).getLeft());
        Assertions.assertEquals(framebufferFit.getTop(), doubledFit.toFramebuffer(doubled).getTop());

        // At 1.5 with a rounded logical size the fit uses the exact logical extent: one scale across and down.
        UiSurface odd = UiSurface.of(ODD_WIDTH, ODD_HEIGHT, DisplayScale.of(THREE_HALVES_SCALE, 1));
        LetterboxFit oddFit = LetterboxFit.of(odd, DESIGN_WIDTH, DESIGN_HEIGHT, LetterboxFit.CENTER, LetterboxFit.CENTER).toFramebuffer(odd);
        LetterboxFit oddFramebufferFit = LetterboxFit.of(ODD_WIDTH, ODD_HEIGHT, DESIGN_WIDTH, DESIGN_HEIGHT);

        Assertions.assertEquals(oddFramebufferFit.getScale(), oddFit.getScale(), TOLERANCE);
        Assertions.assertEquals(oddFramebufferFit.getLeft(), oddFit.getLeft(), TOLERANCE);
        Assertions.assertEquals(oddFramebufferFit.getTop(), oddFit.getTop(), TOLERANCE);
        Assertions.assertEquals(DESIGN_POINT, oddFit.toDesignX(oddFit.toLogicalX(DESIGN_POINT)), TOLERANCE);
    }
}
