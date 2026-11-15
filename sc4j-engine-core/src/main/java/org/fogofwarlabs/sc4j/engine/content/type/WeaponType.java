package org.fogofwarlabs.sc4j.engine.content.type;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one weapon type. Its values are in the per-game weapon type table.
 */
public final class WeaponType extends ContentType {
    /**
     * Creates a {@link WeaponType} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public WeaponType(@NotNull String debugName) {
        super(debugName);
    }
}
