package org.fogofwarlabs.sc4j.engine.client.video;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A whole decoded clip: its RGBA frames and the display time of each frame.
 */
public final class VideoClip {
    /**
     * Reads every remaining frame of a stream ({@link VideoStream#readNextFrame()}). Positive gaps between frame presentation times give the
     * durations; the stream frame period covers missing times and the last frame.
     *
     * @param stream the open stream; it is not closed
     * @return the created {@link VideoClip} instance
     */
    public static @NotNull VideoClip read(@NotNull VideoStream stream) {
        List<byte[]> frames = new ArrayList<>();
        List<Long> starts = new ArrayList<>();
        byte[] frame;

        while ((frame = stream.readNextFrame()) != null) {
            frames.add(frame);
            starts.add(stream.getLastFrameMilliseconds());
        }

        long fallback = stream.getFrameMilliseconds();
        long[] durations = new long[frames.size()];

        for (int index = 0; index < durations.length; index++) {
            long start = starts.get(index);
            long next = index + 1 < starts.size() ? starts.get(index + 1) : -1;

            durations[index] = start >= 0 && next > start ? next - start : fallback;
        }

        return new VideoClip(stream.getWidth(), stream.getHeight(), frames.toArray(new byte[0][]), durations);
    }


    private final int width;
    private final int height;

    private final byte @NotNull [] @NotNull [] frames;
    private final long @NotNull [] durations;

    /**
     * Creates a {@link VideoClip} instance.
     *
     * @param width     the frame width
     * @param height    the frame height
     * @param frames    the RGBA frames
     * @param durations the display time of each frame in milliseconds
     */
    public VideoClip(int width, int height, byte @NotNull [] @NotNull [] frames, long @NotNull [] durations) {
        this.width  = width;
        this.height = height;

        this.frames    = frames;
        this.durations = durations;
    }

    /**
     * @return the frame width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the frame height
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the number of frames
     */
    public int getFrameCount() {
        return this.frames.length;
    }

    /**
     * @param index the frame index
     * @return the RGBA pixels of a frame
     */
    public byte @NotNull [] getFrame(int index) {
        return this.frames[index];
    }

    /**
     * @param index the frame index
     * @return the display time of a frame in milliseconds
     */
    public long getDuration(int index) {
        return this.durations[index];
    }

    /**
     * @return the total clip time in milliseconds
     */
    public long getTotalDuration() {
        long total = 0;

        for (long duration : this.durations) {
            total += duration;
        }

        return total;
    }
}
