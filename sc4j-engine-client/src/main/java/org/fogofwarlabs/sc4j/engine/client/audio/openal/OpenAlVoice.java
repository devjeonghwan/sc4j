package org.fogofwarlabs.sc4j.engine.client.audio.openal;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.audio.AudioBuffer;
import org.fogofwarlabs.sc4j.engine.client.audio.AudioVoice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.lwjgl.openal.AL10.*;

/**
 * Plays buffers on an OpenAL source. A pan places the source on a unit circle in front of the listener, relative to it; the source keeps
 * OpenAL's default position until the first pan other than the centre.
 */
final class OpenAlVoice implements AudioVoice {
    private static final float CENTRE = 0.0f;
    private static final float UNIT_DISTANCE = 1.0f;
    private static final float LISTENER_HEIGHT = 0.0f;

    /**
     * Creates an {@link OpenAlVoice} instance on a new source.
     *
     * @param device the device, whose context is current
     * @return the created {@link OpenAlVoice} instance
     * @throws IllegalStateException if an OpenAL operation fails
     */
    static @NotNull OpenAlVoice create(@NotNull OpenAlAudioDevice device) {
        int name = alGenSources();

        device.checkError();

        return new OpenAlVoice(device, name);
    }


    private final @NotNull OpenAlAudioDevice device;
    private final int name;

    /**
     * The queued buffers in queue order; OpenAL processes and unqueues them in that order.
     */
    private final @NotNull Deque<OpenAlBuffer> queue;
    private float pan;

    /**
     * Creates an {@link OpenAlVoice} instance.
     *
     * @param device the device
     * @param name   the OpenAL source name
     */
    private OpenAlVoice(@NotNull OpenAlAudioDevice device, int name) {
        this.device = device;
        this.name   = name;

        this.queue = new ArrayDeque<>();
        this.pan   = CENTRE;
    }

    /**
     * @return {@code true} if the voice is playing, {@code false} if it is stopped or its samples ended
     */
    @Override
    public boolean isPlaying() {
        this.device.makeCurrent();

        return alGetSourcei(this.name, AL_SOURCE_STATE) == AL_PLAYING;
    }

    /**
     * Sets the buffer the voice plays; the voice must be stopped.
     *
     * @param buffer the buffer, or {@code null} to detach the current one
     */
    @Override
    public void setBuffer(@Nullable AudioBuffer buffer) {
        this.device.makeCurrent();
        alSourcei(this.name, AL_BUFFER, buffer == null ? AL_NONE : this.requireBuffer(buffer).getName());
        this.device.checkError();

        this.queue.clear();
    }

    /**
     * Sets whether the voice repeats its buffer.
     *
     * @param looping whether the buffer repeats
     */
    @Override
    public void setLooping(boolean looping) {
        this.device.makeCurrent();
        alSourcei(this.name, AL_LOOPING, looping ? AL_TRUE : AL_FALSE);
        this.device.checkError();
    }

    /**
     * Sets the gain the voice plays at.
     *
     * @param gain the linear gain, {@code 1} unattenuated
     */
    @Override
    public void setGain(float gain) {
        this.device.makeCurrent();
        alSourcef(this.name, AL_GAIN, gain);
        this.device.checkError();
    }

    /**
     * Sets the stereo position of a mono buffer; stereo buffers keep their own channels.
     *
     * @param pan the position from {@code -1} (left) through {@code 0} (centre) to {@code 1} (right)
     */
    @Override
    public void setPan(float pan) {
        float position = Math.clamp(pan, -UNIT_DISTANCE, UNIT_DISTANCE);

        if (position == this.pan) {
            return;
        }

        this.device.makeCurrent();
        alSourcei(this.name, AL_SOURCE_RELATIVE, AL_TRUE);
        alSource3f(this.name, AL_POSITION, position, LISTENER_HEIGHT, (float) -Math.sqrt(UNIT_DISTANCE - position * position));
        this.device.checkError();

        this.pan = position;
    }


    /**
     * Starts playing the buffer or the queue from its first sample.
     */
    @Override
    public void start() {
        this.device.makeCurrent();
        alSourcePlay(this.name);
        this.device.checkError();
    }

    /**
     * Stops playing; every queued buffer counts as processed.
     */
    @Override
    public void stop() {
        this.device.makeCurrent();
        alSourceStop(this.name);
        this.device.checkError();
    }


    /**
     * Appends a buffer to the voice's queue; a queue and a single buffer ({@link #setBuffer}) are not mixed.
     *
     * @param buffer the buffer, of the same format as the buffers already queued
     */
    @Override
    public void queueBuffer(@NotNull AudioBuffer buffer) {
        OpenAlBuffer queued = this.requireBuffer(buffer);

        this.device.makeCurrent();
        alSourceQueueBuffers(this.name, queued.getName());
        this.device.checkError();

        this.queue.addLast(queued);
    }

    /**
     * @return the number of buffers in the queue, processed ones included
     */
    @Override
    public int getQueuedBufferCount() {
        this.device.makeCurrent();

        return alGetSourcei(this.name, AL_BUFFERS_QUEUED);
    }

    /**
     * @return the number of queued buffers that finished playing
     */
    @Override
    public int getProcessedBufferCount() {
        this.device.makeCurrent();

        return alGetSourcei(this.name, AL_BUFFERS_PROCESSED);
    }

    /**
     * Removes the oldest processed buffer from the queue.
     *
     * @return the buffer, which the caller releases or queues again
     * @throws IllegalStateException if no queued buffer is processed
     */
    @Override
    public @NotNull AudioBuffer unqueueBuffer() {
        this.device.makeCurrent();
        alSourceUnqueueBuffers(this.name);
        this.device.checkError();

        OpenAlBuffer buffer = this.queue.pollFirst();

        if (buffer == null) {
            throw Exceptions.newIllegalStateException("The voice unqueued a buffer it did not queue.");
        }

        return buffer;
    }

    private @NotNull OpenAlBuffer requireBuffer(@NotNull AudioBuffer buffer) {
        if (!(buffer instanceof OpenAlBuffer openAlBuffer) || openAlBuffer.getDevice() != this.device) {
            throw Exceptions.newIllegalArgumentException("The buffer does not belong to this OpenAL device: {0}", buffer);
        }

        return openAlBuffer;
    }


    /**
     * Releases the voice; it must be stopped. A released voice must not be used again. Never throws.
     * <p>
     * An OpenAL error of the deletion is discarded; the device frees what remains when it closes.
     */
    @Override
    public void release() {
        if (this.device.makeCurrentForRelease()) {
            alDeleteSources(this.name);
        }

        this.queue.clear();
    }
}
