package org.fogofwarlabs.sc4j.engine.content.type;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the identity of one content type: a unit type, a weapon type, a faction, a resource kind or any other kind a
 * mod registers in a {@link TypeRegistry}. A mod declares each identity once, as a constant, and refers to it in source;
 * identities compare by reference and hold no values, which live in the per-game tables.
 * <p>
 * The index is assigned by the registry in registration order and is frozen afterwards. Registering the same identity
 * again at the same position, as a new session of the same mod in the same class loader does, keeps the index;
 * registering it at another position fails. The index is {@link #NO_INDEX} until the identity is registered.
 */
public abstract class ContentType {
    /**
     * The index of an identity that is not registered, and the stored value of a type reference that names no type.
     */
    public static final int NO_INDEX = -1;

    /**
     * Returns the index of an optional identity.
     *
     * @param type the identity, or {@code null} for none
     * @return the index of the identity, or {@link #NO_INDEX} if it is {@code null}
     */
    public static int indexOf(@Nullable ContentType type) {
        return type != null ? type.index : NO_INDEX;
    }


    private final @NotNull String debugName;

    private int index;

    /**
     * Creates a {@link ContentType} instance that is not registered yet.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    protected ContentType(@NotNull String debugName) {
        this.debugName = debugName;

        this.index = NO_INDEX;
    }

    /**
     * @return the name, for diagnostics only
     */
    public final @NotNull String getDebugName() {
        return this.debugName;
    }

    /**
     * @return the registration-order index, or {@link #NO_INDEX} if the identity is not registered
     */
    public final int getIndex() {
        return this.index;
    }

    /**
     * @return {@code true} if a registry assigned the index, {@code false} otherwise
     */
    public final boolean isRegistered() {
        return this.index != NO_INDEX;
    }


    /**
     * Assigns the registration-order index. Called only by {@link TypeRegistry}.
     *
     * @param assignedIndex the position in the registry
     * @throws IllegalStateException if the identity already has another index
     */
    final void assignIndex(int assignedIndex) {
        if (this.index != NO_INDEX && this.index != assignedIndex) {
            throw Exceptions.newIllegalStateException("The type {0} already has the index {1}; it cannot be registered at {2}.",
                                                      this.debugName, this.index, assignedIndex);
        }

        this.index = assignedIndex;
    }

    /**
     * @return the name and the index, for diagnostics
     */
    @Override
    public final @NotNull String toString() {
        return this.debugName + " #" + this.index;
    }
}
