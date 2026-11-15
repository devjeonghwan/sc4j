package org.fogofwarlabs.sc4j.engine.resource.text;

import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the glyph image of a font face: glyph rectangles in the image, lookup by code point and the checks.
 */
final class FontFaceTest {
    @Test
    void find_glyph_should_return_the_glyph_of_a_code_point_or_null() {
        FontGlyph space = new FontGlyph(' ', 3, 0, 0, 0, 0, 0, 0);
        FontGlyph letter = new FontGlyph('A', 5, 1, 2, 2, 3, 6, 1);
        FontFace face = new FontFace(9, 8, PixelLayer.ofAlpha(new byte[8 * 4]), List.of(space, letter));

        Assertions.assertSame(letter, face.findGlyph('A'));
        Assertions.assertSame(space, face.findGlyph(' '));
        Assertions.assertNull(face.findGlyph('B'));
        Assertions.assertEquals(8, face.getImageWidth());
        Assertions.assertEquals(4, face.getImageHeight());
    }

    @Test
    void constructor_should_reject_glyphs_outside_the_image_and_unsorted_glyphs() {
        PixelLayer image = PixelLayer.ofAlpha(new byte[8 * 4]);
        FontGlyph outside = new FontGlyph('A', 5, 0, 0, 3, 3, 6, 0);
        FontGlyph first = new FontGlyph('B', 5, 0, 0, 1, 1, 0, 0);
        FontGlyph second = new FontGlyph('A', 5, 0, 0, 1, 1, 0, 0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> new FontFace(9, 8, image, List.of(outside)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FontFace(9, 8, image, List.of(first, second)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FontFace(9, 7, image, List.of()));
    }
}
