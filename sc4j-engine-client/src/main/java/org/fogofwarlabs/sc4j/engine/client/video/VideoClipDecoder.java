package org.fogofwarlabs.sc4j.engine.client.video;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Decodes a whole clip into a {@link VideoClip}, for short clips that play from memory (portraits, animated icons). A client takes the engine
 * default ({@link #getDefault}); tests give their own.
 */
@FunctionalInterface
public interface VideoClipDecoder {
    /**
     * Returns the engine default decoder: it opens the clip with {@link VideoStream#open} without audio and reads it with
     * {@link VideoClip#read}.
     *
     * @return the default decoder
     */
    static @NotNull VideoClipDecoder getDefault() {
        return clip -> {
            try (VideoStream stream = VideoStream.open(clip, false)) {
                return VideoClip.read(stream);
            }
        };
    }

    /**
     * Decodes a whole clip.
     *
     * @param clip the clip file contents
     * @return the decoded clip
     * @throws IOException if the clip cannot be decoded
     */
    @NotNull VideoClip decode(byte @NotNull [] clip) throws IOException;
}
