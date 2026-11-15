package org.fogofwarlabs.sc4j.engine.client.audio.openal;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.audio.AudioBuffer;
import org.fogofwarlabs.sc4j.engine.client.audio.AudioDevice;
import org.fogofwarlabs.sc4j.engine.client.audio.AudioVoice;
import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

import java.nio.ByteBuffer;

import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.AL10.alGetError;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.SOFTHRTF.ALC_HRTF_SOFT;
import static org.lwjgl.openal.SOFTLoopback.*;

/**
 * Plays sound through OpenAL: the engine default {@link AudioDevice}. It opens the system's default output, or a loopback device that renders
 * into memory on {@link #render} and plays nothing audible. HRTF is off: voices are mixed in plain stereo.
 * <p>
 * Every operation makes the device's context current if another context is, and reports an OpenAL error as an {@link IllegalStateException}.
 */
public final class OpenAlAudioDevice implements AudioDevice {
    /**
     * The terminator of an OpenAL context attribute list.
     */
    private static final int ATTRIBUTE_LIST_TERMINATOR = 0;

    /**
     * The number of interleaved channels of a loopback render.
     */
    private static final int LOOPBACK_CHANNELS = 2;

    /**
     * The loopback rate of a device with system output.
     */
    private static final int NO_LOOPBACK = 0;

    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;

    /**
     * Creates an {@link OpenAlAudioDevice} instance on the system's default output.
     *
     * @return the created {@link OpenAlAudioDevice} instance
     * @throws IllegalStateException if the device or its context cannot be opened
     */
    public static @NotNull OpenAlAudioDevice open() {
        long device = alcOpenDevice((ByteBuffer) null);

        if (device == 0) {
            throw Exceptions.newIllegalStateException("Cannot open the default OpenAL sound device.");
        }

        return createContext(device, new int[]{ALC_HRTF_SOFT, ALC_FALSE, ATTRIBUTE_LIST_TERMINATOR}, NO_LOOPBACK);
    }

    /**
     * Creates an {@link OpenAlAudioDevice} instance on a loopback device: it plays nothing audible and mixes its voices into memory when
     * {@link #render} is called; its time advances with the rendered samples.
     *
     * @param sampleRate the rate of the rendered stereo samples, in sample frames per second
     * @return the created {@link OpenAlAudioDevice} instance
     * @throws IllegalArgumentException if the rate is not positive
     * @throws IllegalStateException    if the device or its context cannot be opened
     */
    public static @NotNull OpenAlAudioDevice openLoopback(int sampleRate) {
        if (sampleRate <= 0) {
            throw Exceptions.newIllegalArgumentException("A loopback device renders at a positive rate: {0}", sampleRate);
        }

        long device = alcLoopbackOpenDeviceSOFT((ByteBuffer) null);

        if (device == 0) {
            throw Exceptions.newIllegalStateException("Cannot open an OpenAL loopback device.");
        }

        return createContext(device,
                             new int[]{ALC_FORMAT_CHANNELS_SOFT, ALC_STEREO_SOFT, ALC_FORMAT_TYPE_SOFT, ALC_SHORT_SOFT, ALC_FREQUENCY, sampleRate,
                                       ALC_HRTF_SOFT, ALC_FALSE, ATTRIBUTE_LIST_TERMINATOR},
                             sampleRate);
    }

    /**
     * Creates the context of an opened device and makes it current; the device is closed if that fails.
     *
     * @param device       the opened device
     * @param attributes   the context attributes
     * @param loopbackRate the loopback render rate, or {@link #NO_LOOPBACK}
     * @return the created {@link OpenAlAudioDevice} instance
     * @throws IllegalStateException if the context cannot be established
     */
    private static @NotNull OpenAlAudioDevice createContext(long device, int @NotNull [] attributes, int loopbackRate) {
        long context = alcCreateContext(device, attributes);

        if (context == 0 || !alcMakeContextCurrent(context)) {
            if (context != 0) {
                alcDestroyContext(context);
            }

            alcCloseDevice(device);

            throw Exceptions.newIllegalStateException("Cannot establish the OpenAL sound context.");
        }

        try {
            AL.createCapabilities(ALC.createCapabilities(device));
        } catch (RuntimeException | Error exception) {
            alcMakeContextCurrent(0);
            alcDestroyContext(context);
            alcCloseDevice(device);

            throw exception;
        }

        return new OpenAlAudioDevice(device, context, loopbackRate);
    }


    private final long device;
    private final long context;
    private final int loopbackRate;

    /**
     * The sample frames rendered by a loopback device.
     */
    private long renderedFrames;
    private boolean closed;

    /**
     * Creates an {@link OpenAlAudioDevice} instance.
     *
     * @param device       the opened device
     * @param context      its current context
     * @param loopbackRate the loopback render rate, or {@link #NO_LOOPBACK}
     */
    private OpenAlAudioDevice(long device, long context, int loopbackRate) {
        this.device       = device;
        this.context      = context;
        this.loopbackRate = loopbackRate;

        this.renderedFrames = 0L;
        this.closed         = false;
    }

    /**
     * @return {@code true} if this is a loopback device, {@code false} otherwise (it plays on the system output)
     */
    public boolean isLoopback() {
        return this.loopbackRate != NO_LOOPBACK;
    }

    /**
     * Returns the time base of playback: {@link System#nanoTime()} for the system output, the rendered sample frames for a loopback device.
     *
     * @return the time base of playback in nanoseconds, monotonic: voices finish their buffers on it
     */
    @Override
    public long getTime() {
        return this.isLoopback() ? this.renderedFrames * NANOSECONDS_PER_SECOND / this.loopbackRate : System.nanoTime();
    }


    /**
     * Uploads a clip into a new buffer.
     *
     * @param clip the samples
     * @return the created buffer, which the caller releases
     * @throws IllegalArgumentException if the device cannot play the clip's channel count
     * @throws IllegalStateException    if the device is closed or the upload fails
     */
    @Override
    public @NotNull AudioBuffer createBuffer(@NotNull SoundClip clip) {
        this.makeCurrent();

        return OpenAlBuffer.upload(this, clip);
    }

    /**
     * Creates a stopped voice without a buffer, at gain {@code 1} and centred.
     *
     * @return the created voice, which the caller releases
     * @throws IllegalStateException if the device is closed or cannot create a voice
     */
    @Override
    public @NotNull AudioVoice createVoice() {
        this.makeCurrent();

        return OpenAlVoice.create(this);
    }

    /**
     * Mixes the playing voices of a loopback device into memory and advances its time.
     *
     * @param samples receives interleaved stereo signed {@code 16}-bit samples, a whole number of sample frames
     * @throws IllegalStateException if this is not a loopback device or it is closed
     */
    public void render(short @NotNull [] samples) {
        if (!this.isLoopback()) {
            throw Exceptions.newIllegalStateException("Only a loopback device renders into memory.");
        }

        this.makeCurrent();

        int frames = samples.length / LOOPBACK_CHANNELS;

        alcRenderSamplesSOFT(this.device, samples, frames);
        this.renderedFrames += frames;
    }

    /**
     * Makes the device's context current and discards errors left by earlier calls; another device's context may be current.
     *
     * @throws IllegalStateException if the device is closed or its context cannot be made current
     */
    void makeCurrent() {
        if (this.closed) {
            throw Exceptions.newIllegalStateException("The OpenAL sound device is closed.");
        }

        if (alcGetCurrentContext() != this.context && !alcMakeContextCurrent(this.context)) {
            throw Exceptions.newIllegalStateException("Cannot make the OpenAL sound context current.");
        }

        alGetError();
    }

    /**
     * Makes the device's context current for a release and discards errors left by earlier calls; never throws.
     *
     * @return {@code true} if the context is current, {@code false} otherwise (the device is closed or the context cannot be made current)
     */
    boolean makeCurrentForRelease() {
        if (this.closed || alcGetCurrentContext() != this.context && !alcMakeContextCurrent(this.context)) {
            return false;
        }

        alGetError();

        return true;
    }

    /**
     * Reports the first OpenAL error since the last check.
     *
     * @throws IllegalStateException if an OpenAL operation failed
     */
    void checkError() {
        int error = alGetError();

        if (error != AL_NO_ERROR) {
            throw Exceptions.newIllegalStateException("OpenAL sound operation failed: {0}", error);
        }
    }


    /**
     * Closes the device. Buffers and voices that were not released are released with it and must not be used again.
     * <p>
     * No OpenAL context is current afterwards.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;

        alcMakeContextCurrent(0);
        alcDestroyContext(this.context);
        alcCloseDevice(this.device);
    }
}
