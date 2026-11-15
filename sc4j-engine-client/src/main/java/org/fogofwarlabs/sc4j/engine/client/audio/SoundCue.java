package org.fogofwarlabs.sc4j.engine.client.audio;

import org.jetbrains.annotations.NotNull;

/**
 * Describes a sound for the channel rules of {@link SoundChannels}: its priority, whether a sound of higher priority may replace it, and which
 * playing sounds keep it from starting. A mod implements its own rules (for example one voice line per unit type) in its cues.
 */
public interface SoundCue {
    /**
     * Creates a {@link SoundCue} instance that any sound of higher priority may replace and that no playing sound blocks.
     *
     * @param priority the priority, higher is more important
     * @return the created {@link SoundCue} instance
     */
    static @NotNull SoundCue of(int priority) {
        return new PrioritySoundCue(priority);
    }

    /**
     * @return the priority, higher is more important
     */
    int getPriority();

    /**
     * @return {@code true} if a sound of higher priority may replace this one while it plays, {@code false} otherwise
     */
    boolean isPreemptible();

    /**
     * Tells whether a playing sound keeps this sound from starting.
     *
     * @param playing the cue of a playing sound
     * @return {@code true} if this sound must not start while that one plays, {@code false} otherwise
     */
    boolean conflictsWith(@NotNull SoundCue playing);
}
