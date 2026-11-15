package org.fogofwarlabs.sc4j.engine.client.audio;

import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests that the silent device's voices play for the length of their samples on its clock, loop until stopped and process queues in order.
 */
final class SilentAudioDeviceTest {
    private static final int SAMPLE_RATE = 1000;
    private static final int FRAMES = 500;
    private static final long CLIP_NANOSECONDS = 500_000_000L;
    private static final long STEP_NANOSECONDS = 1_000_000L;

    @Test
    void voice_plays_for_length_of_buffer() {
        long[] time = {0L};

        try (SilentAudioDevice device = new SilentAudioDevice(() -> time[0])) {
            AudioBuffer buffer = device.createBuffer(new SoundClip(SAMPLE_RATE, 1, new short[FRAMES]));
            AudioVoice voice = device.createVoice();

            Assertions.assertFalse(voice.isPlaying());

            voice.setBuffer(buffer);
            voice.start();
            time[0] = CLIP_NANOSECONDS - STEP_NANOSECONDS;

            Assertions.assertTrue(voice.isPlaying());

            time[0] = CLIP_NANOSECONDS;

            Assertions.assertFalse(voice.isPlaying());

            voice.setLooping(true);
            voice.start();
            time[0] = CLIP_NANOSECONDS * 3;

            Assertions.assertTrue(voice.isPlaying(), "a looping voice plays on");

            voice.stop();

            Assertions.assertFalse(voice.isPlaying());
        }
    }

    @Test
    void queue_plays_buffers_in_order_and_ends_after_last() {
        long[] time = {0L};

        try (SilentAudioDevice device = new SilentAudioDevice(() -> time[0])) {
            AudioVoice voice = device.createVoice();

            voice.queueBuffer(device.createBuffer(new SoundClip(SAMPLE_RATE, 2, new short[FRAMES * 2])));
            voice.queueBuffer(device.createBuffer(new SoundClip(SAMPLE_RATE, 2, new short[FRAMES * 2])));
            voice.start();
            time[0] = CLIP_NANOSECONDS;

            Assertions.assertEquals(2, voice.getQueuedBufferCount());
            Assertions.assertEquals(1, voice.getProcessedBufferCount());
            Assertions.assertTrue(voice.isPlaying());

            voice.unqueueBuffer().release();
            time[0] = CLIP_NANOSECONDS * 2;

            Assertions.assertEquals(1, voice.getProcessedBufferCount());
            Assertions.assertFalse(voice.isPlaying(), "the queue ended after its last buffer");
            Assertions.assertThrows(IllegalStateException.class, () -> {
                voice.unqueueBuffer();
                voice.unqueueBuffer();
            });
        }
    }

    @Test
    void closed_device_rejects_new_buffers_and_voices() {
        SilentAudioDevice device = new SilentAudioDevice(System::nanoTime);

        device.close();

        Assertions.assertThrows(IllegalStateException.class, device::createVoice);
        Assertions.assertThrows(IllegalStateException.class, () -> device.createBuffer(new SoundClip(SAMPLE_RATE, 1, new short[1])));
    }
}
