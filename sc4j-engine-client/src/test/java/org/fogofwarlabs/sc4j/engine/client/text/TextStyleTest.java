package org.fogofwarlabs.sc4j.engine.client.text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the text style values: the defaults, deriving a style from another and the scale check.
 */
final class TextStyleTest {
    private static final float SCALE = 1.5f;

    @Test
    void default_style_has_no_color_codes_kerning_on_unit_scale_and_exact_glyphs() {
        Assertions.assertSame(TextColorHook.NONE, TextStyle.DEFAULT.getColors());
        Assertions.assertTrue(TextStyle.DEFAULT.isKerning());
        Assertions.assertEquals(TextStyle.UNIT_SCALE, TextStyle.DEFAULT.getScale());
        Assertions.assertFalse(TextStyle.DEFAULT.isPixelSnapped());
    }

    @Test
    void builder_should_start_from_the_values_of_a_style() {
        TextColorTable colors = TextColorTable.builder().setRestoreCode(1).build();
        TextStyle style = TextStyle.builder().setColors(colors).setKerning(false).setScale(SCALE).setPixelSnapped(true).build();
        TextStyle derived = TextStyle.builder(style).setKerning(true).build();

        Assertions.assertSame(colors, derived.getColors());
        Assertions.assertTrue(derived.isKerning());
        Assertions.assertEquals(SCALE, derived.getScale());
        Assertions.assertTrue(derived.isPixelSnapped());
        Assertions.assertFalse(style.isKerning());
    }

    @Test
    void build_should_refuse_a_scale_that_is_not_positive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> TextStyle.builder().setScale(0).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> TextStyle.builder().setScale(Float.NaN).build());
    }
}
