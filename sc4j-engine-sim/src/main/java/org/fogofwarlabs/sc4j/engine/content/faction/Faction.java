package org.fogofwarlabs.sc4j.engine.content.faction;

import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies one faction, the side a player plays. Its {@link FactionDefinition} is game-invariant: a faction has no per-game
 * table. A player slot names its faction by the identity ({@code PlayerSlot.getFaction()}).
 */
public final class Faction extends ContentType {
    /**
     * Creates a {@link Faction} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public Faction(@NotNull String debugName) {
        super(debugName);
    }
}
