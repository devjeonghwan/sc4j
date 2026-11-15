package org.fogofwarlabs.sc4j.engine.client.audio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the sound positioning: the volume fades with the distance outside the screen down to the sound's minimum, and the pan follows the
 * place on the screen within the stereo width.
 */
final class SoundPositioningTest {
    private static final float DELTA = 1e-6f;

    @Test
    void positioning_fades_with_distance_off_screen_and_pans_by_place() {
        SoundPositioning positioning = new SoundPositioning(100, 500, 1.0f);

        Assertions.assertEquals(100, positioning.getVolume(0, 50, 50, 0, 0, 100, 100));
        Assertions.assertEquals(50, positioning.getVolume(0, 300, 150, 0, 0, 100, 100), "200 + 50 outside the screen");
        Assertions.assertEquals(20, positioning.getVolume(20, 900, 50, 0, 0, 100, 100), "the sound's minimum volume");
        Assertions.assertEquals(0.0f, positioning.getPan(50, 0, 100), DELTA);
        Assertions.assertEquals(-1.0f, positioning.getPan(-30, 0, 100), DELTA);
        Assertions.assertEquals(0.0f, new SoundPositioning(100, 500, 0.0f).getPan(100, 0, 100), DELTA, "no stereo width, no panning");
    }
}
