package org.fogofwarlabs.sc4j.engine.content.type;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one sound type: a sound effect the simulation asks to play. Its values are in the per-game sound type table.
 */
public final class SoundType extends ContentType {
    /**
     * Creates a {@link SoundType} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public SoundType(@NotNull String debugName) {
        super(debugName);
    }
}
