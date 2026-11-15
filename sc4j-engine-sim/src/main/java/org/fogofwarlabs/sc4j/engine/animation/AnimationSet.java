package org.fogofwarlabs.sc4j.engine.animation;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Holds the animations of one image type: for each {@link AnimationKind} it has, the program and the position the
 * animation starts at. A set has the index of its position in its library.
 * <p>
 * The lookups by kind index are array reads, for the frame.
 */
public final class AnimationSet {
    /**
     * The start position of a kind the set does not have.
     */
    public static final int NO_START_POSITION = -1;

    /**
     * Builds {@link AnimationSet} instances.
     */
    public static final class Builder {
        private final int index;
        private final @NotNull String name;

        private @Nullable AnimationKind @NotNull [] kinds;
        private @Nullable AnimationProgram @NotNull [] programs;
        private int @NotNull [] startPositions;

        /**
         * Creates a {@link Builder} instance of a set without animations.
         *
         * @param index the index of the set in its library, at least {@code 0}
         * @param name  the set name, for diagnostics
         * @throws IllegalArgumentException if the index is negative
         */
        public Builder(int index, @NotNull String name) {
            if (index < 0) {
                throw Exceptions.newIllegalArgumentException("Invalid animation set index {0} of {1}", index, name);
            }

            this.index = index;
            this.name  = name;

            this.kinds          = new AnimationKind[0];
            this.programs       = new AnimationProgram[0];
            this.startPositions = new int[0];
        }

        /**
         * Sets the animation of a kind.
         *
         * @param kind          the kind
         * @param program       the program of the animation
         * @param startPosition the position the animation starts at
         * @return this instance for chaining
         * @throws IllegalArgumentException if the start position lies outside the program or the kind is already set
         */
        public @NotNull Builder setAnimation(@NotNull AnimationKind kind, @NotNull AnimationProgram program, int startPosition) {
            int kindIndex = kind.getIndex();

            if (!program.contains(startPosition)) {
                throw Exceptions.newIllegalArgumentException("The animation {0} of set {1} starts outside program {2}: {3}",
                                                             kind, this.name, program, startPosition);
            }

            if (kindIndex >= this.kinds.length) {
                int length = kindIndex + 1;
                int oldLength = this.startPositions.length;

                this.kinds          = Arrays.copyOf(this.kinds, length);
                this.programs       = Arrays.copyOf(this.programs, length);
                this.startPositions = Arrays.copyOf(this.startPositions, length);

                Arrays.fill(this.startPositions, oldLength, length, NO_START_POSITION);
            }

            if (this.kinds[kindIndex] != null) {
                throw Exceptions.newIllegalArgumentException("The animation {0} of set {1} is set twice.", kind, this.name);
            }

            this.kinds[kindIndex]          = kind;
            this.programs[kindIndex]       = program;
            this.startPositions[kindIndex] = startPosition;

            return this;
        }


        /**
         * Builds an {@link AnimationSet} instance.
         *
         * @return the built {@link AnimationSet} instance
         */
        public @NotNull AnimationSet build() {
            return new AnimationSet(this.index, this.name, this.kinds.clone(), this.programs.clone(), this.startPositions.clone());
        }
    }

    private final int index;
    private final @NotNull String name;

    private final @Nullable AnimationKind @NotNull [] kinds;
    private final @Nullable AnimationProgram @NotNull [] programs;
    private final int @NotNull [] startPositions;

    private AnimationSet(int index, @NotNull String name,
                         @Nullable AnimationKind @NotNull [] kinds, @Nullable AnimationProgram @NotNull [] programs, int @NotNull [] startPositions) {
        this.index = index;
        this.name  = name;

        this.kinds          = kinds;
        this.programs       = programs;
        this.startPositions = startPositions;
    }

    /**
     * @return the index of the set in its library
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * @return the set name, for diagnostics
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return one more than the highest kind index the set has an animation of, {@code 0} without animations
     */
    public int getKindLimit() {
        return this.kinds.length;
    }

    /**
     * Returns the kind of a kind index if the set has its animation.
     *
     * @param kindIndex the kind index
     * @return the kind, or {@code null} if the set has no animation of the index
     */
    public @Nullable AnimationKind getKind(int kindIndex) {
        return kindIndex >= 0 && kindIndex < this.kinds.length ? this.kinds[kindIndex] : null;
    }

    /**
     * Checks whether the set has the animation of a kind.
     *
     * @param kind the kind
     * @return {@code true} if the set has the animation, {@code false} otherwise
     */
    public boolean contains(@NotNull AnimationKind kind) {
        return this.getKind(kind.getIndex()) == kind;
    }

    /**
     * Returns the program of an animation.
     *
     * @param kind the kind
     * @return the program, or {@code null} if the set has no animation of the kind
     */
    public @Nullable AnimationProgram getProgram(@NotNull AnimationKind kind) {
        return this.contains(kind) ? this.programs[kind.getIndex()] : null;
    }

    /**
     * Returns the program of an animation by kind index.
     *
     * @param kindIndex the kind index
     * @return the program, or {@code null} if the set has no animation of the index
     */
    public @Nullable AnimationProgram getProgram(int kindIndex) {
        return kindIndex >= 0 && kindIndex < this.programs.length ? this.programs[kindIndex] : null;
    }

    /**
     * Returns the start position of an animation.
     *
     * @param kind the kind
     * @return the start position, or {@link #NO_START_POSITION} if the set has no animation of the kind
     */
    public int getStartPosition(@NotNull AnimationKind kind) {
        return this.contains(kind) ? this.startPositions[kind.getIndex()] : NO_START_POSITION;
    }

    /**
     * Returns the start position of an animation by kind index.
     *
     * @param kindIndex the kind index
     * @return the start position, or {@link #NO_START_POSITION} if the set has no animation of the index
     */
    public int getStartPosition(int kindIndex) {
        return kindIndex >= 0 && kindIndex < this.startPositions.length ? this.startPositions[kindIndex] : NO_START_POSITION;
    }


    /**
     * @return the set name and index, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return this.name + " (" + this.index + ")";
    }
}
