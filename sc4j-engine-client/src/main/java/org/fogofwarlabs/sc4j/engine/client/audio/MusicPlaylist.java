package org.fogofwarlabs.sc4j.engine.client.audio;

import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Plays a client's music: a list of tracks of a {@link ResourceProvider} in turn on a {@link MusicPlayer} of its own bus under the master bus,
 * the list repeating from its first track after its last; a list of one track loops it. A new list fades in from its first track; asking for
 * the list that plays keeps it playing, so scenes that share a list (the menus) do not restart it. Each track is read and decoded when it
 * starts and released when the next one starts or the music stops. A track that cannot be read or played is logged and skipped; when no track
 * of the list plays, the music stops.
 * <p>
 * {@link #update} on the interface clock advances the fade-in and starts the next track when one ends.
 */
public final class MusicPlaylist implements AutoCloseable {
    private static final @NotNull Logger LOGGER = Logger.of(MusicPlaylist.class);
    private static final float FULL_VOLUME = 1.0f;

    private final @NotNull AudioMixer mixer;
    private final @NotNull AudioBus bus;
    private final @NotNull MusicPlayer player;
    private final @NotNull List<@NotNull ResourceKey> tracks;

    private @Nullable ResourceProvider resources;
    private int trackIndex;

    /**
     * Creates a {@link MusicPlaylist} instance at full volume that plays nothing until {@link #play}.
     *
     * @param mixer the client's audio mixer
     * @throws IllegalStateException if the mixer is closed
     */
    public MusicPlaylist(@NotNull AudioMixer mixer) {
        AudioBus musicBus = mixer.createBus();

        this.mixer  = mixer;
        this.bus    = musicBus;
        this.player = new MusicPlayer(mixer.createMusicChannel(musicBus), musicBus);
        this.tracks = new ArrayList<>();

        this.resources  = null;
        this.trackIndex = -1;
    }

    /**
     * @return the bus of the music, under the master bus
     */
    public @NotNull AudioBus getBus() {
        return this.bus;
    }

    /**
     * @return the player of the music
     */
    public @NotNull MusicPlayer getPlayer() {
        return this.player;
    }

    /**
     * @return the music volume, from {@code 0} to {@code 1}
     */
    public float getVolume() {
        return this.player.getVolume();
    }

    /**
     * Sets the music volume; {@code 0} silences the music, which keeps its place in the list.
     *
     * @param volume the music volume, clamped to {@code 0} to {@code 1}
     */
    public void setVolume(float volume) {
        this.player.setVolume(Math.clamp(volume, 0.0f, FULL_VOLUME), volume > 0);
    }

    /**
     * @return the pack id of the track that plays, or {@code null} if the music is stopped
     */
    public @Nullable ResourceKey getTrack() {
        return this.trackIndex >= 0 ? this.tracks.get(this.trackIndex) : null;
    }

    /**
     * @return the tracks of the list that plays, empty if the music is stopped
     */
    public @NotNull List<@NotNull ResourceKey> getTracks() {
        return List.copyOf(this.tracks);
    }


    /**
     * Plays a list of tracks from its first track with a fade-in, unless the same list of the same provider plays already.
     *
     * @param resources the provider of the tracks, read with {@link ResourceProvider#loadSound}
     * @param tracks    the pack ids of the tracks in their order; empty stops the music
     */
    public void play(@NotNull ResourceProvider resources, @NotNull List<@NotNull ResourceKey> tracks) {
        if (resources == this.resources && tracks.equals(this.tracks) && this.trackIndex >= 0) {
            return;
        }

        this.stop();

        if (tracks.isEmpty()) {
            return;
        }

        this.resources = resources;
        this.tracks.addAll(tracks);

        this.player.startFadeIn();
        this.startTrack(0);
    }

    /**
     * Stops the music and forgets its list.
     */
    public void stop() {
        this.player.stop();
        this.tracks.clear();

        this.resources  = null;
        this.trackIndex = -1;
    }

    /**
     * Advances the fade-in and starts the next track of the list when the current one ended.
     *
     * @param nowMilliseconds the interface clock in milliseconds
     */
    public void update(long nowMilliseconds) {
        this.player.update(nowMilliseconds);

        if (this.trackIndex >= 0 && this.player.hasChannel() && !this.player.isPlaying()) {
            this.startTrack((this.trackIndex + 1) % this.tracks.size());
        }
    }

    /**
     * Starts a track of the list, or the first one after it that plays; the music stops if none of the list plays.
     */
    private void startTrack(int firstIndex) {
        ResourceProvider provider = this.resources;
        boolean loop = this.tracks.size() == 1;

        for (int step = 0; step < this.tracks.size() && provider != null; step++) {
            int index = (firstIndex + step) % this.tracks.size();
            ResourceKey track = this.tracks.get(index);

            try {
                this.player.play(this.mixer.createBuffer(provider.loadSound(track)), loop);
                this.trackIndex = index;

                return;
            } catch (IOException | RuntimeException exception) {
                LOGGER.warning("MUSIC TRACK SKIPPED: {0} ({1})", track, exception.getMessage());
            }
        }

        this.stop();
    }


    /**
     * Disposes resources: stops the music and closes its player and channel; the bus and the mixer stay open.
     */
    @Override
    public void close() {
        IgnoreException.execute(
                this.player::close
        );
    }
}
