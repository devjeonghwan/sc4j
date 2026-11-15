package org.fogofwarlabs.sc4j.engine.content.type;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one upgrade type: a research with levels. Its values are in the per-game upgrade type table.
 */
public final class UpgradeType extends ContentType {
    /**
     * Creates an {@link UpgradeType} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public UpgradeType(@NotNull String debugName) {
        super(debugName);
    }
}
