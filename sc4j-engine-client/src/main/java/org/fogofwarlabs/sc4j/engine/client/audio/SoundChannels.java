package org.fogofwarlabs.sc4j.engine.client.audio;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Plays sounds on a fixed number of channels of one bus: a {@link ChannelPolicy} chooses the channel of each new sound, which then plays at its
 * own gain and stereo position. A channel either borrows its buffer (installed sounds the caller keeps) or owns it (one-off sounds, released when
 * the channel is reused or stopped).
 * <p>
 * Created by {@link AudioMixer#createChannels}; the voices are created with the channels.
 */
public final class SoundChannels implements AudioBus.Member {
    /**
     * Creates the voices of the channels; the created ones are released if one fails.
     *
     * @param device the device
     * @param count  the number of voices
     * @return the voices
     * @throws IllegalStateException if a voice cannot be created
     */
    private static @NotNull AudioVoice @NotNull [] createVoices(@NotNull AudioDevice device, int count) {
        AudioVoice[] voices = new AudioVoice[count];
        int created = 0;

        try {
            for (; created < count; created++) {
                voices[created] = device.createVoice();
            }

            return voices;
        } finally {
            if (created < count) {
                for (int index = 0; index < created; index++) {
                    voices[index].release();
                }
            }
        }
    }


    private final @NotNull AudioMixer mixer;
    private final @NotNull AudioBus bus;
    private final @NotNull ChannelPolicy policy;

    private final @NotNull AudioVoice @NotNull [] voices;
    private final @Nullable SoundCue @NotNull [] cues;
    private final @Nullable AudioBuffer @NotNull [] buffers;
    private final boolean @NotNull [] ownedBuffers;
    private final float @NotNull [] gains;
    private final long @NotNull [] startSequences;
    private final @Nullable PlayingSound @NotNull [] sounds;
    private long nextSequence;

    private boolean closed;

    /**
     * Creates a {@link SoundChannels} instance; the voices created before a failure are released.
     *
     * @param mixer  the mixer
     * @param bus    the bus of the sounds
     * @param count  the number of channels
     * @param policy the channel policy
     * @throws IllegalStateException if a voice cannot be created
     */
    SoundChannels(@NotNull AudioMixer mixer, @NotNull AudioBus bus, int count, @NotNull ChannelPolicy policy) {
        this.mixer  = mixer;
        this.bus    = bus;
        this.policy = policy;

        this.voices         = createVoices(mixer.getDevice(), count);
        this.cues           = new SoundCue[count];
        this.buffers        = new AudioBuffer[count];
        this.ownedBuffers   = new boolean[count];
        this.gains          = new float[count];
        this.startSequences = new long[count];
        this.sounds         = new PlayingSound[count];
        this.nextSequence   = 1L;

        this.closed = false;
    }

    /**
     * @return the bus of the sounds
     */
    public @NotNull AudioBus getBus() {
        return this.bus;
    }

    /**
     * @return the number of channels
     */
    public int getChannelCount() {
        return this.voices.length;
    }

    /**
     * @param channel the channel index
     * @return {@code true} if the channel plays a sound, {@code false} otherwise (always after {@link #close()})
     * @throws IllegalStateException if the device fails
     */
    public boolean isPlaying(int channel) {
        return !this.closed && this.voices[channel].isPlaying();
    }

    /**
     * @param sound a sound of these channels
     * @return {@code true} if the sound still plays on its channel, {@code false} otherwise (it ended, was stopped or was replaced)
     * @throws IllegalStateException if the device fails
     */
    boolean isPlaying(@NotNull PlayingSound sound) {
        return this.sounds[sound.getChannel()] == sound && this.isPlaying(sound.getChannel());
    }

    /**
     * @param channel the channel index
     * @return the cue of the channel's last sound, which may have ended, or {@code null} if the channel has played nothing since it was cleared
     */
    public @Nullable SoundCue getCue(int channel) {
        return this.cues[channel];
    }

    /**
     * @param channel the channel index
     * @return the start order of the channel's last sound (later sounds have larger values), {@code 0} if it played none
     */
    public long getStartSequence(int channel) {
        return this.startSequences[channel];
    }


    /**
     * Plays a sound on the channel the policy chooses: the channel stops, releases a buffer it owns, takes the new buffer at the bus-mixed gain and
     * position, and starts. An owned buffer that does not play is released.
     *
     * @param cue    the channel rules of the sound
     * @param buffer the samples
     * @param owned  whether the channels release the buffer when the channel is reused or stopped
     * @param gain   the sound's own gain
     * @param pan    the stereo position from {@code -1} (left) to {@code 1} (right)
     * @return the playing sound, or {@code null} if the policy chose no channel or the channels are closed
     * @throws IllegalStateException if a device operation fails
     */
    public @Nullable PlayingSound play(@NotNull SoundCue cue, @NotNull AudioBuffer buffer, boolean owned, float gain, float pan) {
        int channel = this.closed ? ChannelPolicy.NO_CHANNEL : this.policy.selectChannel(this, cue);

        if (channel == ChannelPolicy.NO_CHANNEL) {
            if (owned) {
                buffer.release();
            }

            return null;
        }

        AudioVoice voice = this.voices[channel];

        this.gains[channel] = gain;
        voice.stop();
        this.releaseOwnedBuffer(channel);

        this.buffers[channel]      = buffer;
        this.ownedBuffers[channel] = owned;

        voice.setBuffer(buffer);
        voice.setGain(this.bus.mix(gain));
        voice.setPan(pan);
        voice.start();

        PlayingSound sound = new PlayingSound(this, channel);

        this.cues[channel]           = cue;
        this.startSequences[channel] = this.nextSequence++;
        this.sounds[channel]         = sound;

        return sound;
    }

    /**
     * Stops every channel; owned buffers are released, borrowed ones stay attached.
     *
     * @throws IllegalStateException if a device operation fails
     */
    public void stopAll() {
        if (this.closed) {
            return;
        }

        for (int channel = 0; channel < this.voices.length; channel++) {
            this.voices[channel].stop();
            this.releaseOwnedBuffer(channel);
            this.sounds[channel] = null;
        }
    }

    /**
     * Stops every channel, detaches every buffer and forgets the cues, for example before the caller releases its borrowed buffers.
     *
     * @throws IllegalStateException if a device operation fails
     */
    public void clear() {
        if (this.closed) {
            return;
        }

        for (int channel = 0; channel < this.voices.length; channel++) {
            this.voices[channel].stop();
            this.releaseOwnedBuffer(channel);
            this.voices[channel].setBuffer(null);
            this.buffers[channel] = null;
            this.cues[channel]    = null;
            this.sounds[channel]  = null;
        }
    }

    /**
     * Applies the current bus and master gains to the member's voices.
     */
    @Override
    public void refreshGain() {
        if (this.closed) {
            return;
        }

        for (int channel = 0; channel < this.voices.length; channel++) {
            this.voices[channel].setGain(this.bus.mix(this.gains[channel]));
        }
    }

    /**
     * Stops a sound if it still holds its channel; an owned buffer is released.
     *
     * @param sound a sound of these channels
     */
    void stop(@NotNull PlayingSound sound) {
        int channel = sound.getChannel();

        if (this.closed || this.sounds[channel] != sound) {
            return;
        }

        this.voices[channel].stop();
        this.releaseOwnedBuffer(channel);
        this.sounds[channel] = null;
    }

    private void releaseOwnedBuffer(int channel) {
        AudioBuffer buffer = this.buffers[channel];

        if (buffer == null || !this.ownedBuffers[channel]) {
            return;
        }

        this.voices[channel].setBuffer(null);
        buffer.release();
        this.buffers[channel] = null;
    }


    /**
     * Stops and releases the voices and the owned buffers; the channels play nothing afterwards. Never throws.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        this.mixer.removeMember(this.bus, this);

        for (int channel = 0; channel < this.voices.length; channel++) {
            AudioVoice voice = this.voices[channel];
            AudioBuffer buffer = this.buffers[channel];

            try {
                voice.stop();
                voice.setBuffer(null);
            } catch (RuntimeException exception) {
                // The voice is released below; a failing device frees it when it closes.
            }

            voice.release();

            if (buffer != null && this.ownedBuffers[channel]) {
                buffer.release();
            }

            this.buffers[channel] = null;
            this.sounds[channel]  = null;
        }
    }
}
