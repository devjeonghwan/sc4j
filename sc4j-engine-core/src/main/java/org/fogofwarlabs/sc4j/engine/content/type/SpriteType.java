package org.fogofwarlabs.sc4j.engine.content.type;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one sprite type: a drawn object made of images. Its values are in the per-game sprite type table.
 */
public final class SpriteType extends ContentType {
    /**
     * Creates a {@link SpriteType} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public SpriteType(@NotNull String debugName) {
        super(debugName);
    }
}
