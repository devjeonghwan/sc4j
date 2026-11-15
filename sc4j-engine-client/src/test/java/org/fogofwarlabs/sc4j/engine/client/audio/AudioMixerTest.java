package org.fogofwarlabs.sc4j.engine.client.audio;

import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine mixer on a silent device: bus gains, the priority and oldest channel policies with their cues, owned buffers, music
 * channels, streams and closing.
 */
final class AudioMixerTest {
    private static final int SAMPLE_RATE = 1000;
    private static final int FRAMES = 1000;
    private static final long SECOND = 1_000_000_000L;
    private static final float DELTA = 1e-6f;
    private static final int LOW = 1;
    private static final int HIGH = 5;

    /**
     * A silent device that keeps the voices it created and the buffers it released.
     */
    private static final class RecordingDevice implements AudioDevice {
        private final @NotNull SilentAudioDevice device;
        private final @NotNull List<RecordingVoice> voices;

        private int releasedBuffers;

        private RecordingDevice(long @NotNull [] time) {
            this.device = new SilentAudioDevice(() -> time[0]);
            this.voices = new ArrayList<>();

            this.releasedBuffers = 0;
        }

        @Override
        public long getTime() {
            return this.device.getTime();
        }

        @Override
        public @NotNull AudioBuffer createBuffer(@NotNull SoundClip clip) {
            return new RecordingBuffer(this, this.device.createBuffer(clip));
        }

        @Override
        public @NotNull AudioVoice createVoice() {
            RecordingVoice voice = new RecordingVoice(this.device.createVoice());

            this.voices.add(voice);

            return voice;
        }

        @Override
        public void close() {
            this.device.close();
        }
    }

    /**
     * A silent buffer that counts its release.
     */
    private static final class RecordingBuffer implements AudioBuffer {
        private final @NotNull RecordingDevice owner;
        private final @NotNull AudioBuffer buffer;

        private RecordingBuffer(@NotNull RecordingDevice owner, @NotNull AudioBuffer buffer) {
            this.owner  = owner;
            this.buffer = buffer;
        }

        @Override
        public int getSampleRate() {
            return this.buffer.getSampleRate();
        }

        @Override
        public int getChannelCount() {
            return this.buffer.getChannelCount();
        }

        @Override
        public int getFrameCount() {
            return this.buffer.getFrameCount();
        }

        @Override
        public void release() {
            this.owner.releasedBuffers++;
            this.buffer.release();
        }
    }

    /**
     * A silent voice that keeps its gain, position and release.
     */
    private static final class RecordingVoice implements AudioVoice {
        private final @NotNull AudioVoice voice;
        private final @NotNull List<RecordingBuffer> queue;

        private float gain;
        private float pan;
        private boolean released;

        private RecordingVoice(@NotNull AudioVoice voice) {
            this.voice = voice;
            this.queue = new ArrayList<>();

            this.gain     = 1.0f;
            this.pan      = 0.0f;
            this.released = false;
        }

        @Override
        public boolean isPlaying() {
            return this.voice.isPlaying();
        }

        @Override
        public void setBuffer(@Nullable AudioBuffer buffer) {
            this.voice.setBuffer(buffer == null ? null : ((RecordingBuffer) buffer).buffer);
        }

        @Override
        public void setLooping(boolean looping) {
            this.voice.setLooping(looping);
        }

        @Override
        public void setGain(float gain) {
            this.gain = gain;
        }

        @Override
        public void setPan(float pan) {
            this.pan = pan;
        }

        @Override
        public void start() {
            this.voice.start();
        }

        @Override
        public void stop() {
            this.voice.stop();
        }

        @Override
        public void queueBuffer(@NotNull AudioBuffer buffer) {
            this.voice.queueBuffer(((RecordingBuffer) buffer).buffer);
            this.queue.add((RecordingBuffer) buffer);
        }

        @Override
        public int getQueuedBufferCount() {
            return this.voice.getQueuedBufferCount();
        }

        @Override
        public int getProcessedBufferCount() {
            return this.voice.getProcessedBufferCount();
        }

        @Override
        public @NotNull AudioBuffer unqueueBuffer() {
            this.voice.unqueueBuffer();

            return this.queue.removeFirst();
        }

        @Override
        public void release() {
            this.released = true;
            this.voice.release();
        }
    }

    /**
     * A cue that no playing sound of the same name may share a channel time with.
     */
    private static final class NamedCue implements SoundCue {
        private final @NotNull String name;
        private final int priority;
        private final boolean preemptible;

        private NamedCue(@NotNull String name, int priority, boolean preemptible) {
            this.name        = name;
            this.priority    = priority;
            this.preemptible = preemptible;
        }

        @Override
        public int getPriority() {
            return this.priority;
        }

        @Override
        public boolean isPreemptible() {
            return this.preemptible;
        }

        @Override
        public boolean conflictsWith(@NotNull SoundCue playing) {
            return playing instanceof NamedCue other && other.name.equals(this.name);
        }
    }

    private static @NotNull SoundClip createClip(int frames) {
        return new SoundClip(SAMPLE_RATE, 1, new short[frames]);
    }

    @Test
    void sound_plays_at_its_gain_times_bus_and_master_gains() {
        long[] time = {0L};
        RecordingDevice device = new RecordingDevice(time);

        try (AudioMixer mixer = new AudioMixer(device)) {
            AudioBus effects = mixer.createBus();
            SoundChannels channels = mixer.createChannels(effects, 1, new PriorityChannelPolicy());
            AudioBuffer buffer = mixer.createBuffer(createClip(FRAMES));

            Assertions.assertNotNull(channels.play(SoundCue.of(LOW), buffer, false, 0.5f, -0.25f));

            RecordingVoice voice = device.voices.getFirst();

            Assertions.assertEquals(0.5f, voice.gain, DELTA);
            Assertions.assertEquals(-0.25f, voice.pan, DELTA);

            effects.setGain(0.5f);
            mixer.getMasterBus().setGain(0.5f);

            Assertions.assertEquals(0.125f, voice.gain, DELTA);
            Assertions.assertEquals(0.25f, effects.mix(1.0f), DELTA);
            Assertions.assertEquals(0.5f, mixer.getMasterBus().mix(1.0f), DELTA, "the master bus applies only the master gain");

            effects.setGain(2.0f);

            Assertions.assertEquals(1.0f, effects.getGain(), "a bus gain is clamped");
        }
    }

    @Test
    void priority_policy_takes_free_channel_then_replaces_lowest_preemptible_sound() {
        long[] time = {0L};
        RecordingDevice device = new RecordingDevice(time);

        try (AudioMixer mixer = new AudioMixer(device)) {
            SoundChannels channels = mixer.createChannels(mixer.createBus(), 2, new PriorityChannelPolicy());
            AudioBuffer buffer = mixer.createBuffer(createClip(FRAMES));

            PlayingSound first = channels.play(new NamedCue("a", LOW, true), buffer, false, 1.0f, 0.0f);
            PlayingSound second = channels.play(new NamedCue("b", LOW, false), buffer, false, 1.0f, 0.0f);

            Assertions.assertNotNull(first);
            Assertions.assertNotNull(second);
            Assertions.assertEquals(1, first.getChannel(), "the highest-numbered free channel");
            Assertions.assertEquals(0, second.getChannel());
            Assertions.assertNull(channels.play(new NamedCue("a", HIGH, true), buffer, false, 1.0f, 0.0f), "a conflicting sound waits");
            Assertions.assertNull(channels.play(new NamedCue("c", LOW, true), buffer, false, 1.0f, 0.0f), "an equal priority replaces nothing");

            PlayingSound third = channels.play(new NamedCue("c", HIGH, true), buffer, false, 1.0f, 0.0f);

            Assertions.assertNotNull(third);
            Assertions.assertEquals(1, third.getChannel(), "only the preemptible sound gives way");
            Assertions.assertFalse(first.isPlaying());
            Assertions.assertTrue(second.isPlaying());

            time[0] = SECOND;

            Assertions.assertFalse(third.isPlaying(), "the sound ended with its samples");
            Assertions.assertEquals(0, device.releasedBuffers, "borrowed buffers stay with the caller");
        }
    }

    @Test
    void oldest_policy_replaces_oldest_sound_and_releases_owned_buffers() {
        long[] time = {0L};
        RecordingDevice device = new RecordingDevice(time);

        try (AudioMixer mixer = new AudioMixer(device)) {
            SoundChannels channels = mixer.createChannels(mixer.createBus(), 2, new OldestChannelPolicy());
            PlayingSound first = channels.play(SoundCue.of(0), mixer.createBuffer(createClip(FRAMES)), true, 1.0f, 0.0f);
            PlayingSound second = channels.play(SoundCue.of(0), mixer.createBuffer(createClip(FRAMES)), true, 1.0f, 0.0f);
            PlayingSound third = channels.play(SoundCue.of(0), mixer.createBuffer(createClip(FRAMES)), true, 1.0f, 0.0f);

            Assertions.assertNotNull(first);
            Assertions.assertNotNull(second);
            Assertions.assertNotNull(third);
            Assertions.assertEquals(0, first.getChannel(), "the first free channel");
            Assertions.assertEquals(0, third.getChannel(), "the oldest sound gives way");
            Assertions.assertEquals(1, device.releasedBuffers);

            first.stop();

            Assertions.assertTrue(third.isPlaying(), "a replaced sound stops nothing");

            second.stop();

            Assertions.assertFalse(second.isPlaying());
            Assertions.assertEquals(2, device.releasedBuffers);
        }

        Assertions.assertEquals(3, device.releasedBuffers, "closing releases the last owned buffer");
        Assertions.assertTrue(device.voices.stream().allMatch(voice -> voice.released));
    }

    @Test
    void music_channel_replaces_and_releases_its_tracks() {
        long[] time = {0L};
        RecordingDevice device = new RecordingDevice(time);

        try (AudioMixer mixer = new AudioMixer(device)) {
            AudioBus music = mixer.createBus();
            MusicChannel channel = mixer.createMusicChannel(music);

            Assertions.assertFalse(channel.isPlaying());
            Assertions.assertTrue(device.voices.isEmpty(), "the voice comes with the first track");

            channel.play(mixer.createBuffer(createClip(FRAMES)), true);
            time[0] = SECOND * 2;

            Assertions.assertTrue(channel.isPlaying(), "a repeating track plays on");

            music.setGain(0.25f);

            Assertions.assertEquals(0.25f, device.voices.getFirst().gain, DELTA);

            channel.play(mixer.createBuffer(createClip(FRAMES)), false);

            Assertions.assertEquals(1, device.releasedBuffers);

            channel.stop();

            Assertions.assertFalse(channel.isPlaying());
            Assertions.assertEquals(2, device.releasedBuffers);
        }
    }

    @Test
    void stream_plays_queued_clips_and_releases_played_ones() {
        long[] time = {0L};
        RecordingDevice device = new RecordingDevice(time);

        try (AudioMixer mixer = new AudioMixer(device)) {
            AudioStream stream = mixer.createStream(mixer.getMasterBus());

            stream.queue(createClip(FRAMES), 1.0f);
            stream.queue(createClip(FRAMES), 0.5f);

            Assertions.assertEquals(2, stream.getPendingClipCount());
            Assertions.assertEquals(0.5f, device.voices.getFirst().gain, DELTA);

            time[0] = SECOND;
            stream.queue(createClip(FRAMES), 0.5f);

            Assertions.assertEquals(1, device.releasedBuffers, "the played clip is released");
            Assertions.assertEquals(2, stream.getPendingClipCount());

            stream.close();

            Assertions.assertEquals(0, stream.getPendingClipCount());
        }
    }

    @Test
    void closed_mixer_closes_its_parts_and_device() {
        long[] time = {0L};
        RecordingDevice device = new RecordingDevice(time);
        AudioMixer mixer = new AudioMixer(device);
        SoundChannels channels = mixer.createChannels(mixer.createBus(), 1, new PriorityChannelPolicy());
        AudioBuffer buffer = mixer.createBuffer(createClip(FRAMES));

        mixer.close();

        Assertions.assertTrue(mixer.isClosed());
        Assertions.assertNull(channels.play(SoundCue.of(LOW), buffer, false, 1.0f, 0.0f));
        Assertions.assertFalse(channels.isPlaying(0));
        Assertions.assertThrows(IllegalStateException.class, () -> mixer.createBuffer(createClip(1)));
        Assertions.assertThrows(IllegalStateException.class, device::createVoice, "the device is closed");
    }
}
