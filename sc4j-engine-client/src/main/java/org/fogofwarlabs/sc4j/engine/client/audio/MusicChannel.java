package org.fogofwarlabs.sc4j.engine.client.audio;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Plays one track at a time on a bus, once or repeating; the channel owns the track's buffer and releases it when the track is replaced or
 * stopped. The voice is created by the first {@link #play}.
 * <p>
 * Created by {@link AudioMixer#createMusicChannel}.
 */
public final class MusicChannel implements AudioBus.Member {
    private static final float FULL = 1.0f;

    private final @NotNull AudioMixer mixer;
    private final @NotNull AudioBus bus;

    private @Nullable AudioVoice voice;
    private @Nullable AudioBuffer buffer;
    private float gain;

    private boolean closed;

    /**
     * Creates a {@link MusicChannel} instance at gain {@code 1}.
     *
     * @param mixer the mixer
     * @param bus   the bus of the tracks
     */
    MusicChannel(@NotNull AudioMixer mixer, @NotNull AudioBus bus) {
        this.mixer = mixer;
        this.bus   = bus;

        this.voice  = null;
        this.buffer = null;
        this.gain   = FULL;

        this.closed = false;
    }

    /**
     * @return the channel's own gain
     */
    public float getGain() {
        return this.gain;
    }

    /**
     * Sets the channel's own gain, under its bus gain.
     *
     * @param gain the linear gain
     * @throws IllegalStateException if a device operation fails
     */
    public void setGain(float gain) {
        this.gain = gain;
        this.refreshGain();
    }

    /**
     * @return {@code true} if a track plays, {@code false} otherwise (always after {@link #close()})
     * @throws IllegalStateException if the device fails
     */
    public boolean isPlaying() {
        AudioVoice current = this.voice;

        return !this.closed && current != null && current.isPlaying();
    }


    /**
     * Replaces the current track: the channel stops and releases the previous buffer, then plays the new one at the bus-mixed gain. The channel
     * takes the buffer; it is released if it cannot play.
     *
     * @param track the track's samples
     * @param loop  whether the track repeats
     * @throws IllegalStateException if the channel is closed or a device operation fails
     */
    public void play(@NotNull AudioBuffer track, boolean loop) {
        try {
            if (this.closed) {
                throw this.mixer.newClosedException();
            }

            AudioVoice current = this.voice;

            if (current == null) {
                current    = this.mixer.getDevice().createVoice();
                this.voice = current;
            }

            this.stop();
            this.buffer = track;

            current.setBuffer(track);
            current.setLooping(loop);
            current.setGain(this.bus.mix(this.gain));
            current.start();
        } catch (RuntimeException exception) {
            if (this.buffer != track) {
                track.release();
            }

            throw exception;
        }
    }

    /**
     * Stops the track and releases its buffer.
     *
     * @throws IllegalStateException if a device operation fails
     */
    public void stop() {
        AudioVoice current = this.voice;

        if (this.closed || current == null) {
            return;
        }

        current.stop();
        current.setBuffer(null);

        if (this.buffer != null) {
            this.buffer.release();
            this.buffer = null;
        }
    }

    /**
     * Applies the current bus and master gains to the member's voices.
     */
    @Override
    public void refreshGain() {
        if (!this.closed && this.voice != null) {
            this.voice.setGain(this.bus.mix(this.gain));
        }
    }


    /**
     * Stops the track and releases the voice and the buffer. Never throws.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        this.mixer.removeMember(this.bus, this);

        AudioVoice current = this.voice;

        if (current != null) {
            try {
                current.stop();
                current.setBuffer(null);
            } catch (RuntimeException exception) {
                // The voice is released below; a failing device frees it when it closes.
            }

            current.release();
            this.voice = null;
        }

        if (this.buffer != null) {
            this.buffer.release();
            this.buffer = null;
        }
    }
}
