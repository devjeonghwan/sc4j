package org.fogofwarlabs.sc4j.engine.content.economy;

import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies one kind of resource that players gather and spend, such as gold or wood. The mod registers its kinds
 * in the content; costs in the type tables have one value per kind, in registration order, and the amounts each player
 * holds belong to the economy system.
 */
public final class ResourceKind extends ContentType {
    /**
     * Creates a {@link ResourceKind} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public ResourceKind(@NotNull String debugName) {
        super(debugName);
    }
}
