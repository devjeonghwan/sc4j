package org.fogofwarlabs.sc4j.engine.client.video;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads the test clips of the video tests: {@code video/colors.webm}, three {@code 16x16} VP8 frames at {@code 10} frames per second (red, green,
 * then blue with alpha {@code 128}) with a mono {@code 48} kHz Opus track of {@code 0.3} seconds, and {@code video/colors-20x16.webm}, three
 * opaque {@code 20x16} VP8 frames (red, green, then blue) without audio, whose width is not a multiple of eight.
 */
final class VideoTestResources {
    static final @NotNull String COLOR_CLIP = "/video/colors.webm";
    static final int COLOR_CLIP_SIZE = 16;
    static final int COLOR_CLIP_FRAMES = 3;
    static final long COLOR_CLIP_FRAME_MILLISECONDS = 100;
    static final int COLOR_CLIP_SAMPLE_RATE = 48_000;
    static final int TRANSLUCENT_ALPHA = 128;

    static final @NotNull String WIDE_COLOR_CLIP = "/video/colors-20x16.webm";
    static final int WIDE_COLOR_CLIP_WIDTH = 20;
    static final int WIDE_COLOR_CLIP_HEIGHT = 16;

    /**
     * Reads the {@code 16x16} test clip.
     *
     * @return the clip bytes
     * @throws IOException if the resource cannot be read
     */
    static byte @NotNull [] readColorClip() throws IOException {
        return read(COLOR_CLIP);
    }

    /**
     * Reads the {@code 20x16} test clip.
     *
     * @return the clip bytes
     * @throws IOException if the resource cannot be read
     */
    static byte @NotNull [] readWideColorClip() throws IOException {
        return read(WIDE_COLOR_CLIP);
    }

    private static byte @NotNull [] read(@NotNull String resource) throws IOException {
        try (InputStream input = VideoTestResources.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw Exceptions.newIOException("Missing test resource {0}", resource);
            }

            return input.readAllBytes();
        }
    }

    private VideoTestResources() {
    }
}
