package org.fogofwarlabs.sc4j.engine.client.video;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine default video stream on small WebM clips: size, frame period, frames in order with their alpha, every column of a frame whose
 * width is not a multiple of eight, looping, timed playback and the audio track, and streams that are opened and closed repeatedly or are open
 * at once.
 */
final class VideoStreamTest {
    private static final int RGBA_BYTES = 4;
    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;
    private static final int ALPHA = 3;
    private static final int FULL = 255;
    /**
     * The largest difference of a decoded channel from the encoded value (VP8 is lossy).
     */
    private static final int CHANNEL_TOLERANCE = 8;
    private static final long BETWEEN_FIRST_FRAMES = 50;
    private static final long PAST_LAST_FRAME = 250;
    private static final long AUDIO_END = 300;
    /**
     * The streams of the lifetime checks: repeated open and close, and open at once.
     */
    private static final int ROUNDS = 8;
    private static final int OPEN_AT_ONCE = 8;

    private static void assertColor(byte @Nullable [] pixels, int red, int green, int blue, int alpha) {
        Assertions.assertNotNull(pixels);
        Assertions.assertEquals(VideoTestResources.COLOR_CLIP_SIZE * VideoTestResources.COLOR_CLIP_SIZE * RGBA_BYTES, pixels.length);

        int[] expected = {red, green, blue, alpha};

        for (int channel = RED; channel <= ALPHA; channel++) {
            int actual = Byte.toUnsignedInt(pixels[channel]);

            Assertions.assertTrue(Math.abs(actual - expected[channel]) <= CHANNEL_TOLERANCE, "channel " + channel + " is " + actual);
        }
    }

    @Test
    void open_should_read_size_frame_period_and_audio_format() throws IOException {
        try (VideoStream stream = VideoStream.open(VideoTestResources.readColorClip(), true)) {
            Assertions.assertEquals(VideoTestResources.COLOR_CLIP_SIZE, stream.getWidth());
            Assertions.assertEquals(VideoTestResources.COLOR_CLIP_SIZE, stream.getHeight());
            Assertions.assertEquals(VideoTestResources.COLOR_CLIP_FRAME_MILLISECONDS, stream.getFrameMilliseconds());
            Assertions.assertEquals(1, stream.getAudioChannels());
            Assertions.assertEquals(VideoTestResources.COLOR_CLIP_SAMPLE_RATE, stream.getAudioSampleRate());
        }
    }

    @Test
    void open_should_skip_the_audio_track_when_not_asked() throws IOException {
        try (VideoStream stream = VideoStream.open(VideoTestResources.readColorClip(), false)) {
            Assertions.assertEquals(0, stream.getAudioChannels());
            Assertions.assertEquals(0, stream.getAudioSampleRate());
            Assertions.assertEquals(0, stream.readAudio(AUDIO_END).length);
        }
    }

    @Test
    void open_should_reject_bytes_that_are_no_clip() {
        Assertions.assertThrows(IOException.class, () -> VideoStream.open(new byte[]{1, 2, 3, 4}, false));
    }

    @Test
    void read_next_frame_should_return_the_frames_in_order_then_null() throws IOException {
        try (VideoStream stream = VideoStream.open(VideoTestResources.readColorClip(), false)) {
            assertColor(stream.readNextFrame(), FULL, 0, 0, FULL);
            Assertions.assertEquals(0, stream.getLastFrameMilliseconds());

            assertColor(stream.readNextFrame(), 0, FULL, 0, FULL);
            Assertions.assertEquals(VideoTestResources.COLOR_CLIP_FRAME_MILLISECONDS, stream.getLastFrameMilliseconds());

            assertColor(stream.readNextFrame(), 0, 0, FULL, VideoTestResources.TRANSLUCENT_ALPHA);
            Assertions.assertNull(stream.readNextFrame());
        }
    }

    @Test
    void read_next_frame_should_fill_every_column_of_a_width_that_is_not_a_multiple_of_eight() throws IOException {
        int[][] colors = {{FULL, 0, 0}, {0, FULL, 0}, {0, 0, FULL}};

        try (VideoStream stream = VideoStream.open(VideoTestResources.readWideColorClip(), false)) {
            Assertions.assertEquals(VideoTestResources.WIDE_COLOR_CLIP_WIDTH, stream.getWidth());

            for (int[] color : colors) {
                byte[] pixels = stream.readNextFrame();

                Assertions.assertNotNull(pixels);
                Assertions.assertEquals(VideoTestResources.WIDE_COLOR_CLIP_WIDTH * VideoTestResources.WIDE_COLOR_CLIP_HEIGHT * RGBA_BYTES,
                                        pixels.length);

                for (int pixel = 0; pixel < pixels.length / RGBA_BYTES; pixel++) {
                    int[] expected = {color[RED], color[GREEN], color[BLUE], FULL};

                    for (int channel = RED; channel <= ALPHA; channel++) {
                        int actual = Byte.toUnsignedInt(pixels[pixel * RGBA_BYTES + channel]);

                        Assertions.assertTrue(Math.abs(actual - expected[channel]) <= CHANNEL_TOLERANCE,
                                              "column " + pixel % VideoTestResources.WIDE_COLOR_CLIP_WIDTH + " row " +
                                              pixel / VideoTestResources.WIDE_COLOR_CLIP_WIDTH + " channel " + channel + " is " + actual);
                    }
                }
            }
        }
    }

    @Test
    void read_next_looped_frame_should_start_over_after_the_last_frame() throws IOException {
        try (VideoStream stream = VideoStream.open(VideoTestResources.readColorClip(), false)) {
            for (int frame = 0; frame < VideoTestResources.COLOR_CLIP_FRAMES; frame++) {
                Assertions.assertNotNull(stream.readNextLoopedFrame());
            }

            assertColor(stream.readNextLoopedFrame(), FULL, 0, 0, FULL);
        }
    }

    @Test
    void read_frame_at_should_return_the_due_frame_and_keep_the_shown_one() throws IOException {
        try (VideoStream stream = VideoStream.open(VideoTestResources.readColorClip(), true)) {
            assertColor(stream.readFrameAt(0), FULL, 0, 0, FULL);
            Assertions.assertNull(stream.readFrameAt(BETWEEN_FIRST_FRAMES));
            Assertions.assertFalse(stream.isFinished());

            assertColor(stream.readFrameAt(PAST_LAST_FRAME), 0, 0, FULL, VideoTestResources.TRANSLUCENT_ALPHA);
            Assertions.assertTrue(stream.isFinished());
        }
    }

    @Test
    void read_audio_should_return_mono_16_bit_samples_up_to_the_time() throws IOException {
        try (VideoStream stream = VideoStream.open(VideoTestResources.readColorClip(), true)) {
            byte[] samples = stream.readAudio(AUDIO_END);

            Assertions.assertTrue(samples.length > 0);
            Assertions.assertEquals(0, samples.length % Short.BYTES);
            Assertions.assertEquals(0, stream.readAudio(AUDIO_END).length);
        }
    }

    @Test
    void streams_open_decode_and_close_repeatedly() throws IOException {
        byte[] clip = VideoTestResources.readColorClip();

        for (int round = 0; round < ROUNDS; round++) {
            try (VideoStream stream = VideoStream.open(clip, false)) {
                assertColor(stream.readNextLoopedFrame(), FULL, 0, 0, FULL);
            }
        }
    }

    @Test
    void streams_decode_while_open_at_once() throws IOException {
        byte[] clip = VideoTestResources.readColorClip();
        List<VideoStream> streams = new ArrayList<>();

        try {
            for (int index = 0; index < OPEN_AT_ONCE; index++) {
                VideoStream stream = VideoStream.open(clip, false);

                streams.add(stream);

                assertColor(stream.readNextFrame(), FULL, 0, 0, FULL);
            }

            for (VideoStream stream : streams) {
                assertColor(stream.readNextFrame(), 0, FULL, 0, FULL);
            }
        } finally {
            for (VideoStream stream : streams) {
                stream.close();
            }
        }
    }
}
