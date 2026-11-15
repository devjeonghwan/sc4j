package org.fogofwarlabs.sc4j.engine.client.audio;

import org.jetbrains.annotations.NotNull;

/**
 * The engine default cue ({@link SoundCue#of}): a priority, always preemptible, never blocked.
 */
final class PrioritySoundCue implements SoundCue {
    private final int priority;

    /**
     * Creates a {@link PrioritySoundCue} instance.
     *
     * @param priority the priority, higher is more important
     */
    PrioritySoundCue(int priority) {
        this.priority = priority;
    }

    /**
     * @return the priority, higher is more important
     */
    @Override
    public int getPriority() {
        return this.priority;
    }

    /**
     * @return {@code true} if a sound of higher priority may replace this one while it plays, {@code false} otherwise
     */
    @Override
    public boolean isPreemptible() {
        return true;
    }

    /**
     * Tells whether a playing sound keeps this sound from starting.
     *
     * @param playing the cue of a playing sound
     * @return {@code true} if this sound must not start while that one plays, {@code false} otherwise
     */
    @Override
    public boolean conflictsWith(@NotNull SoundCue playing) {
        return false;
    }
}
