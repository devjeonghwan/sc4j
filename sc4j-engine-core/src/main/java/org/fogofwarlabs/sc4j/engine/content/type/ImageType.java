package org.fogofwarlabs.sc4j.engine.content.type;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one image type: a graphic with its animation set. Its values are in the per-game image type table.
 */
public final class ImageType extends ContentType {
    /**
     * Creates an {@link ImageType} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public ImageType(@NotNull String debugName) {
        super(debugName);
    }
}
