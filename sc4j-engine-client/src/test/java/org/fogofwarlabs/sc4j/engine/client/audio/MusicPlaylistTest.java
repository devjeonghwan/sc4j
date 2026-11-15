package org.fogofwarlabs.sc4j.engine.client.audio;

import org.fogofwarlabs.sc4j.engine.animation.AnimationLibrary;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.ResourceNotFoundException;
import org.fogofwarlabs.sc4j.engine.resource.ResourceProvider;
import org.fogofwarlabs.sc4j.engine.resource.ResourceType;
import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogDefinition;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Palette;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TextureSheet;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TileGraphics;
import org.fogofwarlabs.sc4j.engine.resource.text.FontFace;
import org.fogofwarlabs.sc4j.engine.resource.text.StringTable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the engine music playlist on the silent device: a list plays from its first track with a fade-in, asking for the playing list keeps it,
 * an ended track gives way to the next and the list repeats, a track that cannot be read is skipped, and the volume sets the music bus.
 */
final class MusicPlaylistTest {
    private static final int SAMPLE_RATE = 1000;
    /**
     * The length of every test track in samples at {@value #SAMPLE_RATE} per second, and the time a track takes in nanoseconds.
     */
    private static final int TRACK_FRAMES = 100;
    private static final long TRACK_NANOSECONDS = 100_000_000L;
    private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;
    private static final float DELTA = 1e-4f;
    private static final float QUARTER = 0.25f;
    private static final @NotNull ResourceKey FIRST = ResourceKey.of("music-first");
    private static final @NotNull ResourceKey SECOND = ResourceKey.of("music-second");
    private static final @NotNull ResourceKey MISSING = ResourceKey.of("music-missing");

    /**
     * Offers sounds of a fixed length under their keys and records each load; everything else is missing.
     */
    private static final class TrackProvider implements ResourceProvider {
        private final @NotNull Map<@NotNull ResourceKey, @NotNull SoundClip> sounds;
        private final @NotNull List<@NotNull ResourceKey> loads;

        private TrackProvider(@NotNull ResourceKey @NotNull ... keys) {
            this.sounds = new HashMap<>();
            this.loads  = new ArrayList<>();

            for (ResourceKey key : keys) {
                this.sounds.put(key, new SoundClip(SAMPLE_RATE, 1, new short[TRACK_FRAMES]));
            }
        }

        @Override
        public @NotNull SpriteSheet loadSpriteSheet(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.SPRITE_SHEET, key);
        }

        @Override
        public @NotNull TextureSheet loadTextureSheet(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.TEXTURE_SHEET, key);
        }

        @Override
        public @NotNull Palette loadPalette(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.PALETTE, key);
        }

        @Override
        public @NotNull TileGraphics loadTileGraphics(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.TILE_GRAPHICS, key);
        }

        @Override
        public @NotNull AnimationLibrary loadAnimations() throws IOException {
            throw ResourceNotFoundException.create(ResourceType.ANIMATION_LIBRARY, null);
        }

        @Override
        public @NotNull SoundClip loadSound(@NotNull ResourceKey key) throws IOException {
            SoundClip sound = this.sounds.get(key);

            this.loads.add(key);

            if (sound == null) {
                throw ResourceNotFoundException.create(ResourceType.SOUND, key);
            }

            return sound;
        }

        @Override
        public @NotNull StringTable loadStringTable(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.STRING_TABLE, key);
        }

        @Override
        public @NotNull FontFace loadFont(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.FONT, key);
        }

        @Override
        public @NotNull DialogDefinition loadDialog(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.DIALOG, key);
        }
    }

    @Test
    void play_should_start_the_first_track_with_a_fade_in_and_keep_the_list_that_plays() {
        long[] time = {0};
        TrackProvider tracks = new TrackProvider(FIRST, SECOND);

        try (AudioMixer mixer = new AudioMixer(new SilentAudioDevice(() -> time[0]));
             MusicPlaylist music = new MusicPlaylist(mixer)) {
            music.play(tracks, List.of(FIRST, SECOND));

            Assertions.assertEquals(FIRST, music.getTrack());
            Assertions.assertTrue(music.getPlayer().isPlaying());
            Assertions.assertEquals(0, music.getBus().getGain(), DELTA, "a new list fades in");

            music.update(1);
            music.update(1 + MusicPlayer.DEFAULT_FADE_PERIOD_MILLISECONDS * 100);
            music.play(tracks, List.of(FIRST, SECOND));

            Assertions.assertEquals(List.of(FIRST), tracks.loads, "the list that plays is not started again");
            Assertions.assertEquals(1, music.getBus().getGain(), DELTA, "the fade-in reached the volume");
        }
    }

    @Test
    void music_moves_to_the_next_track_when_one_ends_and_repeats_the_list() {
        long[] time = {0};
        TrackProvider tracks = new TrackProvider(FIRST, SECOND);

        try (AudioMixer mixer = new AudioMixer(new SilentAudioDevice(() -> time[0]));
             MusicPlaylist music = new MusicPlaylist(mixer)) {
            music.play(tracks, List.of(FIRST, SECOND));
            time[0] += TRACK_NANOSECONDS + 1;
            music.update(time[0] / NANOSECONDS_PER_MILLISECOND);

            Assertions.assertEquals(SECOND, music.getTrack());

            time[0] += TRACK_NANOSECONDS + 1;
            music.update(time[0] / NANOSECONDS_PER_MILLISECOND);

            Assertions.assertEquals(FIRST, music.getTrack(), "the list repeats");
            Assertions.assertEquals(List.of(FIRST, SECOND, FIRST), tracks.loads);
        }
    }

    @Test
    void a_track_that_cannot_be_read_is_skipped_and_a_list_without_playable_track_stops() {
        long[] time = {0};
        TrackProvider tracks = new TrackProvider(SECOND);

        try (AudioMixer mixer = new AudioMixer(new SilentAudioDevice(() -> time[0]));
             MusicPlaylist music = new MusicPlaylist(mixer)) {
            music.play(tracks, List.of(MISSING, SECOND));

            Assertions.assertEquals(SECOND, music.getTrack(), "the missing track is skipped");

            music.play(tracks, List.of(MISSING));

            Assertions.assertNull(music.getTrack(), "a list without playable track stops the music");
            Assertions.assertTrue(music.getTracks().isEmpty());
            Assertions.assertFalse(music.getPlayer().isPlaying());
        }
    }

    @Test
    void set_volume_should_set_the_music_bus_and_silence_it_at_zero() {
        long[] time = {0};

        try (AudioMixer mixer = new AudioMixer(new SilentAudioDevice(() -> time[0]));
             MusicPlaylist music = new MusicPlaylist(mixer)) {
            music.setVolume(QUARTER);

            Assertions.assertEquals(QUARTER, music.getVolume(), DELTA);
            Assertions.assertEquals(QUARTER, music.getBus().getGain(), DELTA);

            music.setVolume(0);

            Assertions.assertEquals(0, music.getBus().getGain(), DELTA);
        }
    }
}
