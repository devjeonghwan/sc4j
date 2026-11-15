package org.fogofwarlabs.sc4j.engine.client.audio;

import org.fogofwarlabs.sc4j.engine.client.audio.openal.OpenAlAudioDevice;
import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.jetbrains.annotations.NotNull;

/**
 * Plays sound: the output backend of the engine audio. A device uploads {@link SoundClip}s into {@link AudioBuffer}s and creates the
 * {@link AudioVoice}s that play them; which voice plays what at which gain is decided above it.
 * <p>
 * A device and its objects are used from one thread, the client thread. Operations report device failures as {@link IllegalStateException}s;
 * releasing never throws.
 * <p>
 * The engine default plays through OpenAL ({@link #openDefault}).
 */
public interface AudioDevice extends AutoCloseable {
    /**
     * Creates an {@link AudioDevice} instance of the engine default (OpenAL) on the system's default output. HRTF is off: voices are mixed
     * in plain stereo.
     *
     * @return the created {@link AudioDevice} instance, closed by the caller
     * @throws IllegalStateException if the device or its context cannot be opened
     */
    static @NotNull AudioDevice openDefault() {
        return OpenAlAudioDevice.open();
    }

    /**
     * @return the time base of playback in nanoseconds, monotonic: voices finish their buffers on it
     */
    long getTime();


    /**
     * Uploads a clip into a new buffer.
     *
     * @param clip the samples
     * @return the created buffer, which the caller releases
     * @throws IllegalArgumentException if the device cannot play the clip's channel count
     * @throws IllegalStateException    if the device is closed or the upload fails
     */
    @NotNull AudioBuffer createBuffer(@NotNull SoundClip clip);

    /**
     * Creates a stopped voice without a buffer, at gain {@code 1} and centred.
     *
     * @return the created voice, which the caller releases
     * @throws IllegalStateException if the device is closed or cannot create a voice
     */
    @NotNull AudioVoice createVoice();


    /**
     * Closes the device. Buffers and voices that were not released are released with it and must not be used again.
     */
    @Override
    void close();
}
