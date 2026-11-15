package org.fogofwarlabs.sc4j.engine.client.audio;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Plays sound without output: a voice plays for the length of its samples on a supplied clock and then ends, exactly as an output device would
 * report it. For headless clients, servers and tests; nothing is audible.
 * <p>
 * A buffer plays for {@code frames * 1e9 / sampleRate} nanoseconds; a looping voice plays until it is stopped. A queue plays its buffers one
 * after the other from the time the voice started; a buffer queued after the queue ended does not restart it.
 */
public final class SilentAudioDevice implements AudioDevice {
    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;

    /**
     * Samples held without output.
     */
    private static final class SilentBuffer implements AudioBuffer {
        private final int sampleRate;
        private final int channelCount;
        private final int frameCount;

        /**
         * Creates a {@link SilentBuffer} instance.
         *
         * @param clip the samples
         */
        private SilentBuffer(@NotNull SoundClip clip) {
            this.sampleRate   = clip.getSampleRate();
            this.channelCount = clip.getChannelCount();
            this.frameCount   = clip.getFrameCount();
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
         */
        @Override
        public void release() {
            // Nothing is held outside the Java heap.
        }
    }

    /**
     * A voice timed by the device clock.
     */
    private final class SilentVoice implements AudioVoice {
        private @Nullable SilentBuffer buffer;
        private final @NotNull List<SilentBuffer> queue;
        private boolean looping;

        /**
         * Whether the voice was started and not yet stopped or ended.
         */
        private boolean playing;
        /**
         * The device time of the last start.
         */
        private long startTime;
        /**
         * The number of queued buffers that finished playing.
         */
        private int processed;

        /**
         * Creates a {@link SilentVoice} instance.
         */
        private SilentVoice() {
            this.buffer  = null;
            this.queue   = new ArrayList<>();
            this.looping = false;

            this.playing   = false;
            this.startTime = 0L;
            this.processed = 0;
        }

        /**
         * @return {@code true} if the voice is playing, {@code false} if it is stopped or its samples ended
         */
        @Override
        public boolean isPlaying() {
            this.update();

            return this.playing;
        }

        /**
         * Sets the buffer the voice plays; the voice must be stopped.
         *
         * @param buffer the buffer, or {@code null} to detach the current one
         */
        @Override
        public void setBuffer(@Nullable AudioBuffer buffer) {
            SilentAudioDevice.this.requireOpen();

            this.buffer = buffer == null ? null : requireBuffer(buffer);
            this.queue.clear();
            this.processed = 0;
        }

        /**
         * Sets whether the voice repeats its buffer.
         *
         * @param looping whether the buffer repeats
         */
        @Override
        public void setLooping(boolean looping) {
            SilentAudioDevice.this.requireOpen();

            this.looping = looping;
        }

        /**
         * Sets the gain the voice plays at.
         *
         * @param gain the linear gain, {@code 1} unattenuated
         */
        @Override
        public void setGain(float gain) {
            SilentAudioDevice.this.requireOpen();
        }

        /**
         * Sets the stereo position of a mono buffer; stereo buffers keep their own channels.
         *
         * @param pan the position from {@code -1} (left) through {@code 0} (centre) to {@code 1} (right)
         */
        @Override
        public void setPan(float pan) {
            SilentAudioDevice.this.requireOpen();
        }


        /**
         * Starts playing the buffer or the queue from its first sample.
         */
        @Override
        public void start() {
            SilentAudioDevice.this.requireOpen();

            this.playing   = this.buffer != null || !this.queue.isEmpty();
            this.startTime = SilentAudioDevice.this.getTime();
            this.processed = 0;
        }

        /**
         * Stops playing; every queued buffer counts as processed.
         */
        @Override
        public void stop() {
            SilentAudioDevice.this.requireOpen();

            this.playing   = false;
            this.processed = this.queue.size();
        }


        /**
         * Appends a buffer to the voice's queue; a queue and a single buffer ({@link #setBuffer}) are not mixed.
         *
         * @param buffer the buffer, of the same format as the buffers already queued
         */
        @Override
        public void queueBuffer(@NotNull AudioBuffer buffer) {
            SilentAudioDevice.this.requireOpen();
            this.update();

            this.buffer = null;
            this.queue.add(requireBuffer(buffer));
        }

        /**
         * @return the number of buffers in the queue, processed ones included
         */
        @Override
        public int getQueuedBufferCount() {
            return this.queue.size();
        }

        /**
         * @return the number of queued buffers that finished playing
         */
        @Override
        public int getProcessedBufferCount() {
            this.update();

            return this.processed;
        }

        /**
         * Removes the oldest processed buffer from the queue.
         *
         * @return the buffer, which the caller releases or queues again
         * @throws IllegalStateException if no queued buffer is processed
         */
        @Override
        public @NotNull AudioBuffer unqueueBuffer() {
            this.update();

            if (this.processed == 0) {
                throw Exceptions.newIllegalStateException("No queued buffer of the voice is processed.");
            }

            this.processed--;
            this.startTime += getDuration(this.queue.getFirst());

            return this.queue.removeFirst();
        }


        /**
         * Releases the voice; it must be stopped. A released voice must not be used again. Never throws.
         */
        @Override
        public void release() {
            this.playing = false;
            this.buffer  = null;
            this.queue.clear();
        }

        /**
         * Ends a single buffer that played its length, and counts the queued buffers that played; a queue that played every buffer ends.
         */
        private void update() {
            if (!this.playing) {
                return;
            }

            long elapsed = SilentAudioDevice.this.getTime() - this.startTime;

            if (this.buffer != null) {
                this.playing = this.looping || elapsed < getDuration(this.buffer);

                return;
            }

            int played = 0;

            for (SilentBuffer queued : this.queue) {
                long duration = getDuration(queued);

                if (elapsed < duration) {
                    break;
                }

                elapsed -= duration;
                played++;
            }

            this.processed = played;
            this.playing   = played < this.queue.size();
        }
    }

    /**
     * Returns the time a buffer plays.
     *
     * @param buffer the buffer
     * @return its length in nanoseconds
     */
    private static long getDuration(@NotNull SilentBuffer buffer) {
        return buffer.frameCount * NANOSECONDS_PER_SECOND / buffer.sampleRate;
    }

    /**
     * Returns a buffer of a silent device.
     *
     * @param buffer the buffer
     * @return the silent buffer
     * @throws IllegalArgumentException if the buffer belongs to another device type
     */
    private static @NotNull SilentBuffer requireBuffer(@NotNull AudioBuffer buffer) {
        if (!(buffer instanceof SilentBuffer silentBuffer)) {
            throw Exceptions.newIllegalArgumentException("The buffer does not belong to a silent audio device: {0}", buffer);
        }

        return silentBuffer;
    }


    private final @NotNull LongSupplier clock;

    private boolean closed;

    /**
     * Creates a {@link SilentAudioDevice} instance.
     *
     * @param clock the time base in nanoseconds, monotonic, for example {@link System#nanoTime()} or a simulated clock
     */
    public SilentAudioDevice(@NotNull LongSupplier clock) {
        this.clock = clock;

        this.closed = false;
    }

    /**
     * @return the time base of playback in nanoseconds, monotonic: voices finish their buffers on it
     */
    @Override
    public long getTime() {
        return this.clock.getAsLong();
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
        this.requireOpen();

        return new SilentBuffer(clip);
    }

    /**
     * Creates a stopped voice without a buffer, at gain {@code 1} and centred.
     *
     * @return the created voice, which the caller releases
     * @throws IllegalStateException if the device is closed or cannot create a voice
     */
    @Override
    public @NotNull AudioVoice createVoice() {
        this.requireOpen();

        return new SilentVoice();
    }

    private void requireOpen() {
        if (this.closed) {
            throw Exceptions.newIllegalStateException("The silent audio device is closed.");
        }
    }


    /**
     * Closes the device. Buffers and voices that were not released are released with it and must not be used again.
     */
    @Override
    public void close() {
        this.closed = true;
    }
}
