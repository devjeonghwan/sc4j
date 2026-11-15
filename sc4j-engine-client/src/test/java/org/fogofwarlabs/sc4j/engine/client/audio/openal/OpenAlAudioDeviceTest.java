package org.fogofwarlabs.sc4j.engine.client.audio.openal;

import org.fogofwarlabs.sc4j.engine.client.audio.AudioBuffer;
import org.fogofwarlabs.sc4j.engine.client.audio.AudioVoice;
import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * Tests the OpenAL default device on a loopback device, which mixes into memory and plays nothing audible: voices sound at their gain and
 * stereo position, end with their samples and stop, and a queue releases its played buffers.
 */
final class OpenAlAudioDeviceTest {
    private static final int SAMPLE_RATE = 48000;
    private static final int CLIP_FRAMES = 4800;
    private static final short LEVEL = 8000;
    private static final int RENDER_FRAMES = 1024;
    private static final int STEREO = 2;
    /**
     * The rendered blocks after which a voice of one block has ended.
     */
    private static final int PLAYED_BLOCKS = 4;
    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;
    private static final float HALF_GAIN = 0.5f;
    private static final float RIGHT = 1.0f;
    private static final double HALF_RATIO_TOLERANCE = 0.05;
    private static final double PAN_DOMINANCE = 4.0;
    /**
     * The highest mean level of silence: the output dither of OpenAL is about one sample step.
     */
    private static final double SILENCE = 2.0;

    /**
     * Creates a {@link SoundClip} instance: a mono clip of a constant level.
     *
     * @param frames the number of sample frames
     * @return the created {@link SoundClip} instance
     */
    private static @NotNull SoundClip createLevelClip(int frames) {
        short[] samples = new short[frames];

        Arrays.fill(samples, LEVEL);

        return new SoundClip(SAMPLE_RATE, 1, samples);
    }

    /**
     * Renders one block and returns the mean absolute level of one stereo channel over its second half, after the gain ramps.
     *
     * @param device  the loopback device
     * @param channel {@code 0} for left, {@code 1} for right
     * @return the mean absolute sample
     */
    private static double renderLevel(@NotNull OpenAlAudioDevice device, int channel) {
        short[] samples = new short[RENDER_FRAMES * STEREO];
        long sum = 0;

        device.render(samples);

        for (int frame = RENDER_FRAMES / 2; frame < RENDER_FRAMES; frame++) {
            sum += Math.abs(samples[frame * STEREO + channel]);
        }

        return sum / (double) (RENDER_FRAMES / 2);
    }

    @Test
    void voice_sounds_at_its_gain_and_position_until_stopped() {
        try (OpenAlAudioDevice device = OpenAlAudioDevice.openLoopback(SAMPLE_RATE)) {
            AudioBuffer buffer = device.createBuffer(createLevelClip(CLIP_FRAMES));
            AudioVoice voice = device.createVoice();

            Assertions.assertTrue(device.isLoopback());
            Assertions.assertTrue(renderLevel(device, 0) < SILENCE, "nothing plays yet");

            voice.setBuffer(buffer);
            voice.setLooping(true);
            voice.start();

            double full = renderLevel(device, 0);

            Assertions.assertTrue(voice.isPlaying());
            Assertions.assertTrue(full > SILENCE, "the voice sounds");

            voice.setGain(HALF_GAIN);
            renderLevel(device, 0);

            Assertions.assertEquals(HALF_GAIN, renderLevel(device, 0) / full, HALF_RATIO_TOLERANCE, "half the gain is half the level");

            voice.setGain(1.0f);
            voice.setPan(RIGHT);
            renderLevel(device, 1);

            Assertions.assertTrue(renderLevel(device, 1) > PAN_DOMINANCE * renderLevel(device, 0), "a right pan plays on the right");

            voice.stop();

            Assertions.assertFalse(voice.isPlaying());

            renderLevel(device, 0);

            Assertions.assertTrue(renderLevel(device, 0) < SILENCE, "a stopped voice is silent");

            voice.setBuffer(null);
            voice.release();
            buffer.release();
        }
    }

    @Test
    void voice_ends_with_its_samples_and_time_follows_rendering() {
        try (OpenAlAudioDevice device = OpenAlAudioDevice.openLoopback(SAMPLE_RATE)) {
            AudioBuffer buffer = device.createBuffer(createLevelClip(RENDER_FRAMES));
            AudioVoice voice = device.createVoice();

            voice.setBuffer(buffer);
            voice.start();

            for (int block = 0; block < PLAYED_BLOCKS; block++) {
                renderLevel(device, 0);
            }

            Assertions.assertFalse(voice.isPlaying(), "the voice ended with its samples");
            Assertions.assertEquals(PLAYED_BLOCKS * (long) RENDER_FRAMES * NANOSECONDS_PER_SECOND / SAMPLE_RATE, device.getTime());

            voice.setBuffer(null);
            voice.release();
            buffer.release();
        }
    }

    @Test
    void queue_processes_buffers_in_order() {
        try (OpenAlAudioDevice device = OpenAlAudioDevice.openLoopback(SAMPLE_RATE)) {
            AudioVoice voice = device.createVoice();
            AudioBuffer first = device.createBuffer(createLevelClip(RENDER_FRAMES / 2));
            AudioBuffer second = device.createBuffer(createLevelClip(RENDER_FRAMES * PLAYED_BLOCKS));

            voice.queueBuffer(first);
            voice.queueBuffer(second);
            voice.start();
            renderLevel(device, 0);
            renderLevel(device, 0);

            Assertions.assertEquals(2, voice.getQueuedBufferCount());
            Assertions.assertEquals(1, voice.getProcessedBufferCount());
            Assertions.assertSame(first, voice.unqueueBuffer());

            voice.stop();

            Assertions.assertEquals(1, voice.getProcessedBufferCount());
            Assertions.assertSame(second, voice.unqueueBuffer());

            voice.release();
            first.release();
            second.release();
        }
    }

    @Test
    void device_rejects_clips_of_more_than_two_channels() {
        try (OpenAlAudioDevice device = OpenAlAudioDevice.openLoopback(SAMPLE_RATE)) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> device.createBuffer(new SoundClip(SAMPLE_RATE, 4, new short[4])));
        }
    }
}
