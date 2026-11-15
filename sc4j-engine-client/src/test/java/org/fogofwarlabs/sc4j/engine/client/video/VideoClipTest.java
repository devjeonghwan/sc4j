package org.fogofwarlabs.sc4j.engine.client.video;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests that a whole clip takes its frame durations from the gaps between presentation times, with the frame period for missing times and the
 * last frame, and that the default decoder reads the test clip whole.
 */
final class VideoClipTest {
    private static final int WIDTH = 2;
    private static final int HEIGHT = 1;
    private static final long FRAME_PERIOD = 40;
    private static final long FIRST_GAP = 30;
    private static final long SECOND_START = 30;
    private static final long NO_TIME = -1;

    /**
     * A stream that returns one-byte frames with the given presentation times.
     */
    private static final class TimedFrames implements VideoStream {
        private final long @NotNull [] times;

        private int next;
        private long lastTime;

        private TimedFrames(long @NotNull [] times) {
            this.times = times;

            this.next     = 0;
            this.lastTime = NO_TIME;
        }

        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return HEIGHT;
        }

        @Override
        public long getFrameMilliseconds() {
            return FRAME_PERIOD;
        }

        @Override
        public long getLastFrameMilliseconds() {
            return this.lastTime;
        }

        @Override
        public int getAudioChannels() {
            return 0;
        }

        @Override
        public int getAudioSampleRate() {
            return 0;
        }

        @Override
        public boolean isFinished() {
            return this.next == this.times.length;
        }

        @Override
        public byte @NotNull [] readAudio(long untilMilliseconds) {
            return new byte[0];
        }

        @Override
        public byte @Nullable [] readFrameAt(long milliseconds) {
            return this.readNextFrame();
        }

        @Override
        public byte @Nullable [] readNextLoopedFrame() {
            return this.readNextFrame();
        }

        @Override
        public byte @Nullable [] readNextFrame() {
            if (this.next == this.times.length) {
                return null;
            }

            this.lastTime = this.times[this.next];

            return new byte[]{(byte) this.next++};
        }

        @Override
        public void close() {
        }
    }

    @Test
    void read_should_take_durations_from_presentation_gaps_and_the_frame_period() {
        VideoClip clip = VideoClip.read(new TimedFrames(new long[]{0, SECOND_START, NO_TIME, SECOND_START + FRAME_PERIOD}));

        Assertions.assertEquals(WIDTH, clip.getWidth());
        Assertions.assertEquals(HEIGHT, clip.getHeight());
        Assertions.assertEquals(4, clip.getFrameCount());
        Assertions.assertEquals(FIRST_GAP, clip.getDuration(0));
        Assertions.assertEquals(FRAME_PERIOD, clip.getDuration(1), "the next frame has no time");
        Assertions.assertEquals(FRAME_PERIOD, clip.getDuration(2), "the frame has no time");
        Assertions.assertEquals(FRAME_PERIOD, clip.getDuration(3), "the last frame");
        Assertions.assertEquals(FIRST_GAP + 3 * FRAME_PERIOD, clip.getTotalDuration());
        Assertions.assertArrayEquals(new byte[]{2}, clip.getFrame(2));
    }

    @Test
    void default_decoder_reads_every_frame_of_the_clip() throws IOException {
        VideoClip clip = VideoClipDecoder.getDefault().decode(VideoTestResources.readColorClip());

        Assertions.assertEquals(VideoTestResources.COLOR_CLIP_SIZE, clip.getWidth());
        Assertions.assertEquals(VideoTestResources.COLOR_CLIP_FRAMES, clip.getFrameCount());
        Assertions.assertEquals(VideoTestResources.COLOR_CLIP_FRAMES * VideoTestResources.COLOR_CLIP_FRAME_MILLISECONDS, clip.getTotalDuration());
    }
}
