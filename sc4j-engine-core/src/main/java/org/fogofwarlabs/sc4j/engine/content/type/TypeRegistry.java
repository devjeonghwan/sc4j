package org.fogofwarlabs.sc4j.engine.content.type;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the identities of one content kind in registration order and assigns their indices.
 * <p>
 * Determinism contract: the index of an identity is its registration position, starting at {@code 0}, so two machines
 * that register the same identities in the same order get the same indices. {@link #freeze()} ends the registration;
 * the identities, their order and their indices never change afterwards. Lookups never depend on hash order.
 *
 * @param <T> the identity type
 */
public final class TypeRegistry<T extends ContentType> {
    private final @NotNull String kindName;

    private final @NotNull List<@NotNull T> types;
    private boolean frozen;

    /**
     * Creates an empty, open {@link TypeRegistry} instance.
     *
     * @param kindName the name of the content kind, for diagnostics
     */
    public TypeRegistry(@NotNull String kindName) {
        this.kindName = kindName;

        this.types  = new ArrayList<>();
        this.frozen = false;
    }

    /**
     * @return the name of the content kind, for diagnostics
     */
    public @NotNull String getKindName() {
        return this.kindName;
    }

    /**
     * @return the number of registered identities
     */
    public int getCount() {
        return this.types.size();
    }

    /**
     * @return the registered identities in registration order, unmodifiable
     */
    public @NotNull List<@NotNull T> getTypes() {
        return Collections.unmodifiableList(this.types);
    }

    /**
     * @return {@code true} if the registration is ended, {@code false} otherwise
     */
    public boolean isFrozen() {
        return this.frozen;
    }


    /**
     * Registers an identity at the next index.
     *
     * @param type the identity
     * @return the registered identity, for constant declarations
     * @throws IllegalArgumentException if the identity is already registered here
     * @throws IllegalStateException    if the registry is frozen or the identity has another index
     */
    public @NotNull T register(@NotNull T type) {
        if (this.frozen) {
            throw Exceptions.newIllegalStateException("The {0} registry is frozen; {1} cannot be registered.", this.kindName, type.getDebugName());
        }

        if (this.contains(type)) {
            throw Exceptions.newIllegalArgumentException("The type {0} is already registered as a {1}.", type, this.kindName);
        }

        type.assignIndex(this.types.size());
        this.types.add(type);

        return type;
    }

    /**
     * Ends the registration; later registrations fail. Freezing again has no effect.
     */
    public void freeze() {
        this.frozen = true;
    }

    /**
     * Returns the identity at an index.
     *
     * @param index the registration-order index
     * @return the identity
     * @throws IndexOutOfBoundsException if no identity has the index
     */
    public @NotNull T get(int index) {
        if (index < 0 || index >= this.types.size()) {
            throw Exceptions.newIndexOutOfBoundsException("No {0} has the index {1}; {2} are registered.", this.kindName, index, this.types.size());
        }

        return this.types.get(index);
    }

    /**
     * Checks whether an identity is registered here.
     *
     * @param type the identity
     * @return {@code true} if the identity is registered in this registry, {@code false} otherwise
     */
    public boolean contains(@NotNull ContentType type) {
        int index = type.getIndex();

        return index >= 0 && index < this.types.size() && this.types.get(index) == type;
    }

    /**
     * Returns the index of an optional identity that must be registered here, for resolving a type reference of a
     * definition.
     *
     * @param type the identity, or {@code null} for none
     * @return the index, or {@link ContentType#NO_INDEX} if the identity is {@code null}
     * @throws IllegalArgumentException if the identity is not registered here
     */
    public int requireIndex(@Nullable T type) {
        if (type == null) {
            return ContentType.NO_INDEX;
        }

        if (!this.contains(type)) {
            throw Exceptions.newIllegalArgumentException("The type {0} is not registered as a {1}.", type, this.kindName);
        }

        return type.getIndex();
    }
}
