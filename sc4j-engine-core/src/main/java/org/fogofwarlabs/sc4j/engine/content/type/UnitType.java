package org.fogofwarlabs.sc4j.engine.content.type;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one unit type: a kind of unit or building. Its values are in the per-game unit type table.
 */
public final class UnitType extends ContentType {
    /**
     * Creates a {@link UnitType} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public UnitType(@NotNull String debugName) {
        super(debugName);
    }
}
