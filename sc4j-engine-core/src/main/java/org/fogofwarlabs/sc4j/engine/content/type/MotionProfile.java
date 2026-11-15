package org.fogofwarlabs.sc4j.engine.content.type;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one motion profile: how a moving object accelerates, turns and is drawn. Its values are in the per-game motion profile table.
 */
public final class MotionProfile extends ContentType {
    /**
     * Creates a {@link MotionProfile} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public MotionProfile(@NotNull String debugName) {
        super(debugName);
    }
}
