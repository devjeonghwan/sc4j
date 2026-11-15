package org.fogofwarlabs.sc4j.engine.client.audio;

import org.jetbrains.annotations.NotNull;

/**
 * One sound started by {@link SoundChannels#play}: it plays until it ends, is stopped or another sound replaces it on its channel.
 */
public final class PlayingSound {
    private final @NotNull SoundChannels channels;
    private final int channel;

    /**
     * Creates a {@link PlayingSound} instance.
     *
     * @param channels the channels that play it
     * @param channel  its channel index
     */
    PlayingSound(@NotNull SoundChannels channels, int channel) {
        this.channels = channels;
        this.channel  = channel;
    }

    /**
     * @return the channels that play the sound
     */
    public @NotNull SoundChannels getChannels() {
        return this.channels;
    }

    /**
     * @return the channel index of the sound
     */
    public int getChannel() {
        return this.channel;
    }

    /**
     * @return {@code true} if the sound still plays, {@code false} otherwise (it ended, was stopped or was replaced)
     * @throws IllegalStateException if the device fails
     */
    public boolean isPlaying() {
        return this.channels.isPlaying(this);
    }


    /**
     * Stops the sound if it still holds its channel; otherwise nothing happens.
     *
     * @throws IllegalStateException if the device fails
     */
    public void stop() {
        this.channels.stop(this);
    }
}
