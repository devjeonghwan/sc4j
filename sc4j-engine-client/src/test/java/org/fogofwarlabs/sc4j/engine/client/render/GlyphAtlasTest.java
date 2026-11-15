package org.fogofwarlabs.sc4j.engine.client.render;

import org.fogofwarlabs.sc4j.engine.client.HiddenWindowScenario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the shelf placement of the distance field glyph atlas on a hidden window.
 */
final class GlyphAtlasTest {
    private static final int FIELD_PAGE_SIZE = 64;
    private static final int TALL = 10;
    private static final int SHORT = 5;
    private static final int MARGIN = 2;

    @Test
    void add_should_fill_shelves_and_open_one_below_for_taller_glyphs() {
        HiddenWindowScenario.run(() -> {
            try (GpuCanvas canvas = GraphicsDevice.getCurrent().createCanvas(FIELD_PAGE_SIZE);
                 GlyphAtlas atlas = new GlyphAtlas(canvas)) {
                GlyphAtlas.Slot first = atlas.add(new byte[TALL * TALL], TALL, TALL);
                GlyphAtlas.Slot second = atlas.add(new byte[SHORT * SHORT], SHORT, SHORT);
                GlyphAtlas.Slot third = atlas.add(new byte[TALL * (TALL + 1)], TALL, TALL + 1);

                Assertions.assertNotNull(first);
                Assertions.assertNotNull(second);
                Assertions.assertNotNull(third);
                Assertions.assertEquals(0, first.getX());
                Assertions.assertEquals(0, first.getY());
                Assertions.assertEquals(TALL, second.getX());
                Assertions.assertEquals(0, second.getY());
                Assertions.assertEquals(0, third.getX());
                Assertions.assertEquals(TALL + MARGIN, third.getY());
                Assertions.assertEquals(first.getTexture(), third.getTexture());
                Assertions.assertNull(atlas.add(new byte[GlyphAtlas.PAGE_SIZE], GlyphAtlas.PAGE_SIZE, 1));

                atlas.upload();
            }
        });
    }
}
