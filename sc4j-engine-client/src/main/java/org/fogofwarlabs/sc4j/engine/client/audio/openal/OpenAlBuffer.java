package org.fogofwarlabs.sc4j.engine.client.audio.openal;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.audio.AudioBuffer;
import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.openal.AL10.*;

/**
 * Holds the samples of a clip in an OpenAL buffer.
 */
final class OpenAlBuffer implements AudioBuffer {
    private static final int MONO_CHANNELS = 1;
    private static final int STEREO_CHANNELS = 2;

    /**
     * Creates an {@link OpenAlBuffer} instance holding an uploaded mono or stereo clip; the OpenAL buffer is deleted if the upload fails.
     *
     * @param device the device, whose context is current
     * @param clip   the samples
     * @return the created {@link OpenAlBuffer} instance
     * @throws IllegalArgumentException if the clip has more than two channels
     * @throws IllegalStateException    if an OpenAL operation fails
     */
    static @NotNull OpenAlBuffer upload(@NotNull OpenAlAudioDevice device, @NotNull SoundClip clip) {
        int channelCount = clip.getChannelCount();

        if (channelCount != MONO_CHANNELS && channelCount != STEREO_CHANNELS) {
            throw Exceptions.newIllegalArgumentException("The OpenAL device plays mono and stereo sound, not {0} channels.", channelCount);
        }

        short[] samples = new short[clip.getFrameCount() * channelCount];

        clip.copySamples(0, samples, 0, samples.length);

        int name = alGenBuffers();
        boolean uploaded = false;

        try {
            alBufferData(name, channelCount == MONO_CHANNELS ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, samples, clip.getSampleRate());
            device.checkError();
            uploaded = true;

            return new OpenAlBuffer(device, name, clip.getSampleRate(), channelCount, clip.getFrameCount());
        } finally {
            if (!uploaded) {
                alDeleteBuffers(name);
            }
        }
    }


    private final @NotNull OpenAlAudioDevice device;
    private final int name;

    private final int sampleRate;
    private final int channelCount;
    private final int frameCount;

    /**
     * Creates an {@link OpenAlBuffer} instance.
     *
     * @param device       the device
     * @param name         the OpenAL buffer name
     * @param sampleRate   the number of sample frames per second
     * @param channelCount the number of channels
     * @param frameCount   the number of sample frames
     */
    private OpenAlBuffer(@NotNull OpenAlAudioDevice device, int name, int sampleRate, int channelCount, int frameCount) {
        this.device = device;
        this.name   = name;

        this.sampleRate   = sampleRate;
        this.channelCount = channelCount;
        this.frameCount   = frameCount;
    }

    /**
     * @return the device the buffer belongs to
     */
    @NotNull OpenAlAudioDevice getDevice() {
        return this.device;
    }

    /**
     * @return the OpenAL buffer name
     */
    int getName() {
        return this.name;
    }

    /**
     * @return the number of sample frames per second
     */
    @Override
    public int getSampleRate() {
        return this.sampleRate;
    }

    /**
     * @return the number of channels
     */
    @Override
    public int getChannelCount() {
        return this.channelCount;
    }

    /**
     * @return the number of sample frames
     */
    @Override
    public int getFrameCount() {
        return this.frameCount;
    }


    /**
     * Releases the samples. A released buffer must not be used again; a voice must not hold it when it is released. Never throws.
     * <p>
     * An OpenAL error of the deletion is discarded; the device frees what remains when it closes.
     */
    @Override
    public void release() {
        if (this.device.makeCurrentForRelease()) {
            alDeleteBuffers(this.name);
        }
    }
}
