package org.fogofwarlabs.sc4j.engine.animation;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies one named animation of the animation sets, such as idle, walk or death. A library lists its kinds in index
 * order; kind {@code n} has index {@code n}. Kinds compare by identity: a mod declares its kinds once, as constants, or
 * takes them from the library it loaded.
 */
public final class AnimationKind {
    /**
     * Creates an {@link AnimationKind} instance.
     *
     * @param index the index of the kind in its library, at least {@code 0}
     * @param name  the kind name
     * @return the created {@link AnimationKind} instance
     * @throws IllegalArgumentException if the index is negative or the name is empty
     */
    public static @NotNull AnimationKind of(int index, @NotNull String name) {
        if (index < 0 || name.isEmpty()) {
            throw Exceptions.newIllegalArgumentException("Invalid animation kind {0} at index {1}", name, index);
        }

        return new AnimationKind(index, name);
    }


    private final int index;
    private final @NotNull String name;

    private AnimationKind(int index, @NotNull String name) {
        this.index = index;
        this.name  = name;
    }

    /**
     * @return the index of the kind in its library
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * @return the kind name
     */
    public @NotNull String getName() {
        return this.name;
    }


    /**
     * @return the kind name and index, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return this.name + " (" + this.index + ")";
    }
}
