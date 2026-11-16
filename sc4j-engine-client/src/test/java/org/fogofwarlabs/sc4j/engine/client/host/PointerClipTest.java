package org.fogofwarlabs.sc4j.engine.client.host;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the pointer clip of a window ({@link PointerClip}): its conversion from framebuffer pixels to window coordinates and the rules that apply
 * or release it.
 */
final class PointerClipTest {
    private static final int WINDOW_WIDTH = 1920;
    private static final int WINDOW_HEIGHT = 1080;

    @Test
    void to_window_bounds_should_keep_a_pillarboxed_frame_at_one_pixel_per_unit() {
        // A 4:3 frame in a 1920x1080 window: the bars of 240 columns are outside, the fractional edges of a fitted frame round.
        Assertions.assertArrayEquals(new int[]{240, 0, 1680, 1080},
                                     PointerClip.of(240, 0, 1680, 1080).toWindowBounds(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_WIDTH, WINDOW_HEIGHT));
        Assertions.assertArrayEquals(new int[]{0, 0, 1920, 1080},
                                     PointerClip.of(0.375f, 0, 1919.625f, 1080).toWindowBounds(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_WIDTH,
                                                                                               WINDOW_HEIGHT));
    }

    @Test
    void to_window_bounds_should_scale_framebuffer_pixels_to_window_coordinates() {
        // A framebuffer of twice the window size (display scale 2 on a platform that counts window coordinates in points).
        Assertions.assertArrayEquals(new int[]{120, 0, 840, 540},
                                     PointerClip.of(240, 0, 1680, 1080).toWindowBounds(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2, WINDOW_WIDTH,
                                                                                       WINDOW_HEIGHT));
    }

    @Test
    void to_window_bounds_should_stay_inside_the_content_area_and_be_null_when_empty() {
        Assertions.assertArrayEquals(new int[]{0, 0, 1920, 1080},
                                     PointerClip.of(-50, -10, 5000, 2000).toWindowBounds(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_WIDTH, WINDOW_HEIGHT));
        Assertions.assertNull(PointerClip.of(100, 100, 100.2f, 200).toWindowBounds(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_WIDTH, WINDOW_HEIGHT));
        Assertions.assertNull(PointerClip.of(2000, 0, 3000, 1080).toWindowBounds(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    @Test
    void of_should_reject_reversed_or_not_finite_edges() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PointerClip.of(10, 0, 5, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> PointerClip.of(0, 10, 10, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> PointerClip.of(Float.NaN, 0, 10, 10));
        Assertions.assertEquals(PointerClip.of(1, 2, 3, 4), PointerClip.of(1, 2, 3, 4));
        Assertions.assertEquals(PointerClip.of(1, 2, 3, 4).hashCode(), PointerClip.of(1, 2, 3, 4).hashCode());
        Assertions.assertNotEquals(PointerClip.of(1, 2, 3, 4), PointerClip.of(1, 2, 3, 5));
    }

    @Test
    void is_applied_should_hold_only_a_captured_pointer_of_a_focused_open_window() {
        PointerClip clip = PointerClip.of(240, 0, 1680, 1080);

        Assertions.assertTrue(PointerClip.isApplied(clip, PointerMode.CAPTURED, true, false, false));
        // Released: no clip, another pointer mode, focus loss, minimized, closed.
        Assertions.assertFalse(PointerClip.isApplied(null, PointerMode.CAPTURED, true, false, false));
        Assertions.assertFalse(PointerClip.isApplied(clip, PointerMode.NORMAL, true, false, false));
        Assertions.assertFalse(PointerClip.isApplied(clip, PointerMode.HIDDEN, true, false, false));
        Assertions.assertFalse(PointerClip.isApplied(clip, PointerMode.CAPTURED, false, false, false));
        Assertions.assertFalse(PointerClip.isApplied(clip, PointerMode.CAPTURED, true, true, false));
        Assertions.assertFalse(PointerClip.isApplied(clip, PointerMode.CAPTURED, true, false, true));
    }
}
