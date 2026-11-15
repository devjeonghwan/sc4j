package org.fogofwarlabs.sc4j.engine.client.audio;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixes the client's sound on one {@link AudioDevice}: a master bus with the buses under it ({@link AudioBus}), sound channels with their channel
 * rules ({@link SoundChannels}), music channels ({@link MusicChannel}) and streams ({@link AudioStream}). Which sounds a game plays, at which
 * priority and volume, is the mod's; the mixer decides channels and gains.
 * <p>
 * Used from the client thread. Closing the mixer closes everything it created, then the device; closed parts play nothing and ignore calls.
 */
public final class AudioMixer implements AutoCloseable {
    private final @NotNull AudioDevice device;
    private final @NotNull AudioBus masterBus;
    private final @NotNull List<AudioBus.Member> members;

    private boolean closed;

    /**
     * Creates an {@link AudioMixer} instance on a device, which the mixer closes when it closes.
     *
     * @param device the device
     */
    public AudioMixer(@NotNull AudioDevice device) {
        this.device    = device;
        this.masterBus = new AudioBus(null);
        this.members   = new ArrayList<>();

        this.closed = false;
    }

    /**
     * @return the device the mixer plays on
     */
    public @NotNull AudioDevice getDevice() {
        return this.device;
    }

    /**
     * @return the device's time base in nanoseconds ({@link AudioDevice#getTime()})
     */
    public long getTime() {
        return this.device.getTime();
    }

    /**
     * @return the master bus, under which every other bus plays
     */
    public @NotNull AudioBus getMasterBus() {
        return this.masterBus;
    }

    /**
     * @return {@code true} if the mixer is closed, {@code false} otherwise
     */
    public boolean isClosed() {
        return this.closed;
    }


    /**
     * Creates a bus under the master bus, at gain {@code 1}.
     *
     * @return the created bus
     */
    public @NotNull AudioBus createBus() {
        return new AudioBus(this.masterBus);
    }

    /**
     * Uploads a clip into a buffer of the device.
     *
     * @param clip the samples
     * @return the created buffer, which the caller releases or hands to a part that owns it
     * @throws IllegalArgumentException if the device cannot play the clip's channel count
     * @throws IllegalStateException    if the mixer is closed or the upload fails
     */
    public @NotNull AudioBuffer createBuffer(@NotNull SoundClip clip) {
        this.requireOpen();

        return this.device.createBuffer(clip);
    }

    /**
     * Creates sound channels on a bus.
     *
     * @param bus    the bus of the sounds
     * @param count  the number of channels
     * @param policy the rule that chooses the channel of each sound
     * @return the created channels
     * @throws IllegalStateException if the mixer is closed or the voices cannot be created
     */
    public @NotNull SoundChannels createChannels(@NotNull AudioBus bus, int count, @NotNull ChannelPolicy policy) {
        this.requireOpen();

        SoundChannels channels = new SoundChannels(this, bus, count, policy);

        this.addMember(bus, channels);

        return channels;
    }

    /**
     * Creates a music channel on a bus.
     *
     * @param bus the bus of the tracks
     * @return the created channel
     * @throws IllegalStateException if the mixer is closed
     */
    public @NotNull MusicChannel createMusicChannel(@NotNull AudioBus bus) {
        this.requireOpen();

        MusicChannel channel = new MusicChannel(this, bus);

        this.addMember(bus, channel);

        return channel;
    }

    /**
     * Creates a stream on a bus.
     *
     * @param bus the bus of the stream
     * @return the created stream
     * @throws IllegalStateException if the mixer is closed or the voice cannot be created
     */
    public @NotNull AudioStream createStream(@NotNull AudioBus bus) {
        this.requireOpen();

        AudioStream stream = new AudioStream(this, bus);

        this.addMember(bus, stream);

        return stream;
    }

    private void addMember(@NotNull AudioBus bus, @NotNull AudioBus.Member member) {
        bus.addMember(member);
        this.members.add(member);
    }

    /**
     * Forgets a part that closed itself.
     *
     * @param bus    the part's bus
     * @param member the part
     */
    void removeMember(@NotNull AudioBus bus, @NotNull AudioBus.Member member) {
        bus.removeMember(member);
        this.members.remove(member);
    }

    /**
     * Creates an {@link IllegalStateException} instance for an operation on a closed mixer.
     *
     * @return the created {@link IllegalStateException} instance
     */
    @NotNull IllegalStateException newClosedException() {
        return Exceptions.newIllegalStateException("The audio mixer is closed.");
    }

    private void requireOpen() {
        if (this.closed) {
            throw this.newClosedException();
        }
    }


    /**
     * Closes every part the mixer created, newest first, then the device. Never throws.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        List<AudioBus.Member> open = new ArrayList<>(this.members);

        for (int index = open.size() - 1; index >= 0; index--) {
            open.get(index).close();
        }

        this.closed = true;
        this.members.clear();

        IgnoreException.execute(
                this.device::close
        );
    }
}
