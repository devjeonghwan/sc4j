package org.fogofwarlabs.sc4j.engine.content.combat;

import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies one kind of weapon damage, such as normal or explosive damage. The mod registers its kinds in the content;
 * a weapon type names its kind, and the combat system of the mod decides what each kind does against each target.
 */
public final class DamageKind extends ContentType {
    /**
     * Creates a {@link DamageKind} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public DamageKind(@NotNull String debugName) {
        super(debugName);
    }
}
