package org.fogofwarlabs.sc4j.engine.client.audio;

import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.jetbrains.annotations.NotNull;

/**
 * Plays sound that arrives while it plays, such as the audio track of a video: queued clips play one after the other on one voice of a bus,
 * and the clips that finished are released when the next one is queued.
 * <p>
 * Created by {@link AudioMixer#createStream}.
 */
public final class AudioStream implements AudioBus.Member {
    private static final float FULL = 1.0f;

    private final @NotNull AudioMixer mixer;
    private final @NotNull AudioBus bus;
    private final @NotNull AudioVoice voice;

    private float gain;
    private boolean closed;

    /**
     * Creates an {@link AudioStream} instance at gain {@code 1}.
     *
     * @param mixer the mixer
     * @param bus   the bus of the stream
     * @throws IllegalStateException if the voice cannot be created
     */
    AudioStream(@NotNull AudioMixer mixer, @NotNull AudioBus bus) {
        this.mixer = mixer;
        this.bus   = bus;
        this.voice = mixer.getDevice().createVoice();

        this.gain   = FULL;
        this.closed = false;
    }

    /**
     * @return the number of queued clips that have not finished playing, {@code 0} after {@link #close()}
     * @throws IllegalStateException if the device fails
     */
    public int getPendingClipCount() {
        return this.closed ? 0 : this.voice.getQueuedBufferCount() - this.voice.getProcessedBufferCount();
    }


    /**
     * Releases the finished clips, queues a clip at a gain and starts the stream if it does not play; a closed stream ignores the clip.
     *
     * @param clip the samples, of the same format as the clips queued before
     * @param gain the stream's own gain from now on
     * @throws IllegalStateException if a device operation fails
     */
    public void queue(@NotNull SoundClip clip, float gain) {
        if (this.closed) {
            return;
        }

        this.releaseProcessed();

        AudioBuffer buffer = this.mixer.getDevice().createBuffer(clip);

        this.gain = gain;

        try {
            this.voice.queueBuffer(buffer);
        } catch (RuntimeException exception) {
            buffer.release();

            throw exception;
        }

        this.voice.setGain(this.bus.mix(this.gain));

        if (!this.voice.isPlaying()) {
            this.voice.start();
        }
    }

    /**
     * Applies the current bus and master gains to the member's voices.
     */
    @Override
    public void refreshGain() {
        if (!this.closed) {
            this.voice.setGain(this.bus.mix(this.gain));
        }
    }

    private void releaseProcessed() {
        int processed = this.voice.getProcessedBufferCount();

        for (int index = 0; index < processed; index++) {
            this.voice.unqueueBuffer().release();
        }
    }


    /**
     * Stops the stream and releases its voice and clips. Never throws.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        this.mixer.removeMember(this.bus, this);

        try {
            this.voice.stop();
            this.releaseProcessed();
        } catch (RuntimeException exception) {
            // The voice is released below; a failing device frees its buffers when it closes.
        }

        this.voice.release();
    }
}
