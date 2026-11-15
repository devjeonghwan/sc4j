package org.fogofwarlabs.sc4j.engine.client.audio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Checks the master fade when window activation changes.
 */
final class MasterFadeTest {
    private static final float DELTA = 1e-4f;

    @Test
    void update_should_fade_out_in_background_and_back_in_when_active() {
        MasterFade fade = new MasterFade();

        Assertions.assertEquals(1.0f, fade.update(0, true, false), DELTA);
        Assertions.assertEquals(1.0f, fade.update(1000, false, false), DELTA, "the fade starts at the full level");
        Assertions.assertEquals(0.25f, fade.update(1150, false, false), DELTA, "half way the level is 0.5 and the gain its square");
        Assertions.assertEquals(0.0f, fade.update(1300, false, false), DELTA);
        Assertions.assertEquals(0.0f, fade.update(2000, true, false), DELTA, "the fade in starts at the silent level");
        Assertions.assertEquals(0.36f, fade.update(2180, true, false), DELTA);
        Assertions.assertEquals(1.0f, fade.update(2400, true, false), DELTA);
    }

    @Test
    void update_should_keep_level_in_background_when_sound_in_background_is_on() {
        MasterFade fade = new MasterFade();

        fade.update(0, true, true);

        Assertions.assertEquals(1.0f, fade.update(500, false, true), DELTA);
        Assertions.assertEquals(1.0f, fade.update(5000, false, true), DELTA);
    }

    @Test
    void update_should_continue_reversed_fade_from_current_level() {
        MasterFade fade = new MasterFade();

        fade.update(0, true, false);
        fade.update(1000, false, false);
        fade.update(1210, false, false);

        Assertions.assertEquals(0.09f, fade.update(1210, false, false), DELTA, "level 0.3 after 210 ms of 300");
        Assertions.assertEquals(0.09f, fade.update(1210, true, false), DELTA, "the fade in starts from 0.3");
        Assertions.assertEquals(0.36f, fade.update(1300, true, false), DELTA);
    }
}
