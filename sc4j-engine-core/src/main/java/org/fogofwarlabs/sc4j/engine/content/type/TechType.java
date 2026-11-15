package org.fogofwarlabs.sc4j.engine.content.type;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one tech type: a one-time research, usually an ability. Its values are in the per-game tech type table.
 */
public final class TechType extends ContentType {
    /**
     * Creates a {@link TechType} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public TechType(@NotNull String debugName) {
        super(debugName);
    }
}
