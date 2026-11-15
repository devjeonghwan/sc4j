package org.fogofwarlabs.sc4j.engine.animation;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Names a position of an {@link AnimationProgram}, such as the start of an animation or of a shared block. Labels are
 * resolved when the program is created; instructions hold positions, and the labels remain for tools and diagnostics.
 */
public final class AnimationLabel {
    private final @NotNull String name;
    private final int position;

    /**
     * Creates an {@link AnimationLabel} instance.
     *
     * @param name     the label name, not empty
     * @param position the position it names, at least {@code 0}
     * @throws IllegalArgumentException if the name is empty or the position is negative
     */
    public AnimationLabel(@NotNull String name, int position) {
        if (name.isEmpty() || position < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid animation label {0} at position {1}", name, position);
        }

        this.name     = name;
        this.position = position;
    }

    /**
     * @return the label name
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the position the label names
     */
    public int getPosition() {
        return this.position;
    }
}
