package org.fogofwarlabs.sc4j.engine.client.audio;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Plays one buffer, once or looping, or a queue of buffers (a stream), at a gain and a stereo position. A voice plays until its samples end or
 * it is stopped; starting a voice restarts it from the first sample.
 * <p>
 * Every operation except {@link #release()} throws an {@link IllegalStateException} if the device fails.
 */
public interface AudioVoice {
    /**
     * @return {@code true} if the voice is playing, {@code false} if it is stopped or its samples ended
     */
    boolean isPlaying();

    /**
     * Sets the buffer the voice plays; the voice must be stopped.
     *
     * @param buffer the buffer, or {@code null} to detach the current one
     */
    void setBuffer(@Nullable AudioBuffer buffer);

    /**
     * Sets whether the voice repeats its buffer.
     *
     * @param looping whether the buffer repeats
     */
    void setLooping(boolean looping);

    /**
     * Sets the gain the voice plays at.
     *
     * @param gain the linear gain, {@code 1} unattenuated
     */
    void setGain(float gain);

    /**
     * Sets the stereo position of a mono buffer; stereo buffers keep their own channels.
     *
     * @param pan the position from {@code -1} (left) through {@code 0} (centre) to {@code 1} (right)
     */
    void setPan(float pan);


    /**
     * Starts playing the buffer or the queue from its first sample.
     */
    void start();

    /**
     * Stops playing; every queued buffer counts as processed.
     */
    void stop();


    /**
     * Appends a buffer to the voice's queue; a queue and a single buffer ({@link #setBuffer}) are not mixed.
     *
     * @param buffer the buffer, of the same format as the buffers already queued
     */
    void queueBuffer(@NotNull AudioBuffer buffer);

    /**
     * @return the number of buffers in the queue, processed ones included
     */
    int getQueuedBufferCount();

    /**
     * @return the number of queued buffers that finished playing
     */
    int getProcessedBufferCount();

    /**
     * Removes the oldest processed buffer from the queue.
     *
     * @return the buffer, which the caller releases or queues again
     * @throws IllegalStateException if no queued buffer is processed
     */
    @NotNull AudioBuffer unqueueBuffer();


    /**
     * Releases the voice; it must be stopped. A released voice must not be used again. Never throws.
     */
    void release();
}
