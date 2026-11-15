package org.fogofwarlabs.sc4j.engine.client.audio;

import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine music player on the silent device: the bus gain from volume, ducking and fades, and playback state.
 */
final class MusicPlayerTest {
    private static final float DELTA = 1e-4f;
    private static final int SAMPLE_RATE = 22_050;
    private static final int FRAMES = 22_050;
    private static final float VOLUME = 0.8f;
    private static final long FADE_PERIOD = MusicPlayer.DEFAULT_FADE_PERIOD_MILLISECONDS;
    private static final long START = 1000;

    @Test
    void gain_combines_volume_ducking_and_fade_in() {
        long[] time = {0};

        try (AudioMixer mixer = new AudioMixer(new SilentAudioDevice(() -> time[0]))) {
            AudioBus bus = mixer.createBus();
            MusicPlayer player = new MusicPlayer(mixer.createMusicChannel(bus), bus);

            player.setVolume(VOLUME, true);

            Assertions.assertEquals(VOLUME, bus.getGain(), DELTA);

            player.setDucked(true);

            Assertions.assertEquals(VOLUME * MusicPlayer.DEFAULT_DUCKED_LEVEL, bus.getGain(), DELTA);

            player.clearDucking();
            player.startFadeIn();

            Assertions.assertEquals(0, bus.getGain(), DELTA);

            player.update(START);
            player.update(START + 2 * FADE_PERIOD);

            Assertions.assertEquals(VOLUME * 2 * MusicPlayer.DEFAULT_FADE_IN_STEP, bus.getGain(), DELTA);

            player.update(START + 100 * FADE_PERIOD);

            Assertions.assertEquals(VOLUME, bus.getGain(), DELTA);

            player.close();
        }
    }

    @Test
    void play_and_stop_should_track_the_active_state() {
        long[] time = {0};

        try (AudioMixer mixer = new AudioMixer(new SilentAudioDevice(() -> time[0]))) {
            AudioBus bus = mixer.createBus();
            MusicPlayer player = new MusicPlayer(mixer.createMusicChannel(bus), bus);

            player.play(mixer.createBuffer(new SoundClip(SAMPLE_RATE, 1, new short[FRAMES])), true);

            Assertions.assertTrue(player.isActive());
            Assertions.assertTrue(player.isPlaying());

            player.stop();

            Assertions.assertFalse(player.isActive());
            Assertions.assertFalse(player.isPlaying());

            player.close();
        }
    }

    @Test
    void player_without_a_channel_keeps_state_and_plays_nothing() {
        MusicPlayer player = new MusicPlayer(null, null);

        player.setVolume(VOLUME, true);
        player.fadeOut();

        Assertions.assertFalse(player.hasChannel());
        Assertions.assertFalse(player.isActive());
        Assertions.assertFalse(player.isPlaying());
        Assertions.assertEquals(VOLUME, player.getVolume(), DELTA);
    }
}
