package org.fogofwarlabs.sc4j.engine.client.skin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the fill and outline of a colour box style.
 */
final class ColorBoxStyleTest {
    private static final int FILL_COLOR = 0x112233FF;
    private static final int FRAME_COLOR = 0x445566FF;
    private static final float FRAME_THICKNESS = 2;

    @Test
    void getters_should_return_the_fill_and_the_outline() {
        ColorBoxStyle outlined = new ColorBoxStyle(FILL_COLOR, FRAME_COLOR, FRAME_THICKNESS);
        ColorBoxStyle filled = new ColorBoxStyle(FILL_COLOR);

        Assertions.assertEquals(FILL_COLOR, outlined.getFillColor());
        Assertions.assertEquals(FRAME_COLOR, outlined.getFrameColor());
        Assertions.assertEquals(FRAME_THICKNESS, outlined.getFrameThickness());
        Assertions.assertEquals(FILL_COLOR, filled.getFillColor());
        Assertions.assertEquals(0, filled.getFrameThickness(), "a fill alone has no outline");
    }
}
