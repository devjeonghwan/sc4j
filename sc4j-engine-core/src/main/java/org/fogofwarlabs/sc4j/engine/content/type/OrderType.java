package org.fogofwarlabs.sc4j.engine.content.type;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one order type: a kind of unit behavior. Its values are in the per-game order type table.
 */
public final class OrderType extends ContentType {
    /**
     * Creates an {@link OrderType} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public OrderType(@NotNull String debugName) {
        super(debugName);
    }
}
