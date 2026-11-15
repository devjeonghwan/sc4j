package org.fogofwarlabs.sc4j.engine.client.audio;

import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.locks.LockSupport;

/**
 * Plays the tracks of a client on one {@link MusicChannel} and sets the gain of its music {@link AudioBus}: the volume from the settings,
 * ducking (a lower level while something else should be heard) and fades. A fade-in raises the level by a step each period, advanced by
 * {@link #update(long)} on the interface clock; a fade-out lowers it by a step each period and blocks the calling thread until it is silent.
 * The bus gain is {@code volume * (ducked ? duckedLevel : 1) * fadeLevel}, at most {@code 1}.
 * <p>
 * A player without a channel (no sound output) keeps its state and plays nothing. The mod keeps its track lists and chooses the next track:
 * {@link #isPlaying()} turns {@code false} when a track that does not loop ends.
 */
public final class MusicPlayer implements AutoCloseable {
    /**
     * The default gain factor while ducked.
     */
    public static final float DEFAULT_DUCKED_LEVEL = 0.75f;
    /**
     * The default level increase per fade-in period.
     */
    public static final float DEFAULT_FADE_IN_STEP = 0.05f;
    /**
     * The default level decrease per fade-out period.
     */
    public static final float DEFAULT_FADE_OUT_STEP = 0.1f;
    /**
     * The default fade period in milliseconds.
     */
    public static final long DEFAULT_FADE_PERIOD_MILLISECONDS = 20;

    private static final @NotNull Logger LOGGER = Logger.of(MusicPlayer.class);
    private static final float FULL_LEVEL = 1.0f;
    private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;

    private final @Nullable MusicChannel channel;
    private final @Nullable AudioBus bus;

    private final float duckedLevel;
    private final float fadeInStep;
    private final float fadeOutStep;
    private final long fadePeriodMilliseconds;

    /**
     * Whether a track was started and not stopped, including while silent.
     */
    private boolean active;
    private boolean ducked;
    /**
     * The music gain from the settings.
     */
    private float volume;
    /**
     * Whether the music is audible; a fade-in starts only then.
     */
    private boolean audible;

    /**
     * The fade level, {@code 1} when no fade is active.
     */
    private float fadeLevel;
    private boolean fading;
    /**
     * The interface time of the last fade-in step, {@link Long#MIN_VALUE} until the first {@link #update} of a fade-in.
     */
    private long fadeTime;

    /**
     * Creates a {@link MusicPlayer} instance with the default ducking and fade steps.
     *
     * @param channel the music channel, or {@code null} to play nothing
     * @param bus     the music bus whose gain the player sets, or {@code null} to set none
     */
    public MusicPlayer(@Nullable MusicChannel channel, @Nullable AudioBus bus) {
        this(channel, bus, DEFAULT_DUCKED_LEVEL, DEFAULT_FADE_IN_STEP, DEFAULT_FADE_OUT_STEP, DEFAULT_FADE_PERIOD_MILLISECONDS);
    }

    /**
     * Creates a {@link MusicPlayer} instance.
     *
     * @param channel                the music channel, or {@code null} to play nothing
     * @param bus                    the music bus whose gain the player sets, or {@code null} to set none
     * @param duckedLevel            the gain factor while ducked
     * @param fadeInStep             the level increase per fade-in period
     * @param fadeOutStep            the level decrease per fade-out period
     * @param fadePeriodMilliseconds the fade period in milliseconds
     */
    public MusicPlayer(@Nullable MusicChannel channel, @Nullable AudioBus bus,
                       float duckedLevel, float fadeInStep, float fadeOutStep, long fadePeriodMilliseconds) {
        this.channel = channel;
        this.bus     = bus;

        this.duckedLevel            = duckedLevel;
        this.fadeInStep             = fadeInStep;
        this.fadeOutStep            = fadeOutStep;
        this.fadePeriodMilliseconds = fadePeriodMilliseconds;

        this.active  = false;
        this.ducked  = false;
        this.volume  = FULL_LEVEL;
        this.audible = true;

        this.fadeLevel = FULL_LEVEL;
        this.fading    = false;
        this.fadeTime  = Long.MIN_VALUE;
    }

    /**
     * @return {@code true} if the player has a music channel, {@code false} otherwise
     */
    public boolean hasChannel() {
        return this.channel != null;
    }

    /**
     * @return {@code true} if a track was started and not stopped, including while silent, {@code false} otherwise
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * @return {@code true} if the channel is playing a track, {@code false} otherwise or if the channel fails to tell
     */
    public boolean isPlaying() {
        MusicChannel output = this.channel;

        try {
            return output != null && output.isPlaying();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * @return the music gain from the settings, from {@code 0} to {@code 1}
     */
    public float getVolume() {
        return this.volume;
    }

    /**
     * Sets the volume and audibility, ends a fade and applies the gain.
     *
     * @param volume  the music gain from the settings, clamped to {@code 0} to {@code 1}
     * @param audible whether music is audible
     */
    public void setVolume(float volume, boolean audible) {
        this.volume  = Math.clamp(volume, 0.0f, FULL_LEVEL);
        this.audible = audible;

        this.fading    = false;
        this.fadeLevel = FULL_LEVEL;
        this.applyGain();
    }

    /**
     * Sets whether music is ducked and applies the resulting gain at once.
     *
     * @param ducked whether music is ducked
     */
    public void setDucked(boolean ducked) {
        if (this.ducked == ducked) {
            return;
        }

        this.ducked = ducked;
        this.applyGain();
    }


    /**
     * Starts a fade-in: the level drops to {@code 0} and rises from the next {@link #update} on, while the music is audible.
     */
    public void startFadeIn() {
        this.fadeLevel = 0.0f;
        this.applyGain();

        this.fading   = this.audible;
        this.fadeTime = Long.MIN_VALUE;
    }

    /**
     * Plays a track on the channel without ducking.
     *
     * @param track the decoded track
     * @param loop  whether the track repeats
     * @throws RuntimeException if the channel cannot play the track
     */
    public void play(@NotNull AudioBuffer track, boolean loop) {
        MusicChannel output = this.channel;

        if (output == null) {
            return;
        }

        output.play(track, loop);
        this.active = true;
    }

    /**
     * Cancels ducking and applies the gain, as the start of a new track does before it plays.
     */
    public void clearDucking() {
        this.ducked = false;
        this.applyGain();
    }

    /**
     * Stops the channel; a failure is logged.
     */
    public void stop() {
        MusicChannel output = this.channel;

        if (output != null) {
            try {
                output.stop();
            } catch (RuntimeException exception) {
                LOGGER.warning("MUSIC STOP: " + exception.getMessage());
            }
        }

        this.active = false;
    }

    /**
     * Ends a fade-in and lowers active music by the fade-out step each period, blocking the calling thread, then stops and restores the gain.
     */
    public void fadeOut() {
        this.fading = false;

        if (this.active && this.channel != null) {
            long nextFadeTimeNanoseconds = System.nanoTime();

            for (float level = FULL_LEVEL; level > 0.0f; level -= this.fadeOutStep) {
                this.fadeLevel = level;
                this.applyGain();

                nextFadeTimeNanoseconds += this.fadePeriodMilliseconds * NANOSECONDS_PER_MILLISECOND;

                long waitNanoseconds = nextFadeTimeNanoseconds - System.nanoTime();

                if (waitNanoseconds > 0) {
                    LockSupport.parkNanos(waitNanoseconds);
                }
            }

            this.fadeLevel = 0.0f;
            this.applyGain();
        }

        this.fadeLevel = FULL_LEVEL;
        this.stop();
        this.applyGain();
    }

    /**
     * Advances the fade-in steps that are due; the first call after {@link #startFadeIn()} starts its clock.
     *
     * @param nowMilliseconds the interface clock in milliseconds
     */
    public void update(long nowMilliseconds) {
        if (!this.fading) {
            return;
        }

        if (this.fadeTime == Long.MIN_VALUE) {
            this.fadeTime = nowMilliseconds;
        }

        while (this.fading && nowMilliseconds - this.fadeTime >= this.fadePeriodMilliseconds) {
            this.fadeTime += this.fadePeriodMilliseconds;
            this.fadeLevel += this.fadeInStep;

            if (this.fadeLevel >= FULL_LEVEL) {
                this.fadeLevel = FULL_LEVEL;
                this.fading    = false;
            }
        }

        this.applyGain();
    }

    private void applyGain() {
        AudioBus output = this.bus;

        if (output == null || this.channel == null) {
            return;
        }

        float gain = this.volume * (this.ducked ? this.duckedLevel : FULL_LEVEL) * this.fadeLevel;

        output.setGain(Math.min(FULL_LEVEL, gain));
    }


    /**
     * Disposes resources: stops the music and closes the music channel; the bus and the mixer stay open.
     */
    @Override
    public void close() {
        MusicChannel output = this.channel;

        IgnoreException.execute(
                this::stop,
                () -> {
                    if (output != null) {
                        output.close();
                    }
                }
        );
    }
}
