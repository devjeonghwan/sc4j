package org.fogofwarlabs.sc4j.engine.content.definition;

import org.fogofwarlabs.sc4j.engine.animation.AnimationSet;
import org.fogofwarlabs.sc4j.engine.content.type.ImageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the initial values of one {@link ImageType}: what each game copies into its image type table when it is
 * created. A definition is immutable; type references are identities that must be registered in the same content.
 */
public final class ImageDefinition {
    /**
     * Builds {@link ImageDefinition} instances. Every value starts at {@code 0}, {@code false}, {@code null} or no cost
     * unless its setter says otherwise.
     */
    public static final class Builder {
        private @Nullable AnimationSet animationSet;
        private boolean turning;
        private boolean clickable;

        /**
         * Creates a {@link Builder} instance with the default values.
         */
        public Builder() {
            this.animationSet = null;
            this.turning      = false;
            this.clickable    = false;
        }

        /**
         * Sets the animation set of the image, from the animation library of the mod.
         *
         * @param animationSet the animation set, or {@code null} without one
         * @return this instance for chaining
         */
        public @NotNull Builder setAnimationSet(@Nullable AnimationSet animationSet) {
            this.animationSet = animationSet;

            return this;
        }

        /**
         * Sets whether the image has frames for each direction.
         *
         * @param turning the value
         * @return this instance for chaining
         */
        public @NotNull Builder setTurning(boolean turning) {
            this.turning = turning;

            return this;
        }

        /**
         * Sets whether a click on the image selects its owner.
         *
         * @param clickable the value
         * @return this instance for chaining
         */
        public @NotNull Builder setClickable(boolean clickable) {
            this.clickable = clickable;

            return this;
        }


        /**
         * Builds an {@link ImageDefinition} instance from the current values.
         *
         * @return the built {@link ImageDefinition} instance
         */
        public @NotNull ImageDefinition build() {
            return new ImageDefinition(this);
        }
    }

    private final @Nullable AnimationSet animationSet;
    private final boolean turning;
    private final boolean clickable;

    private ImageDefinition(@NotNull Builder builder) {
        this.animationSet = builder.animationSet;
        this.turning      = builder.turning;
        this.clickable    = builder.clickable;
    }

    /**
     * @return the animation set of the image, or {@code null} without one
     */
    public @Nullable AnimationSet getAnimationSet() {
        return this.animationSet;
    }

    /**
     * @return {@code true} if the image has frames for each direction, {@code false} otherwise
     */
    public boolean isTurning() {
        return this.turning;
    }

    /**
     * @return {@code true} if a click on the image selects its owner, {@code false} otherwise
     */
    public boolean isClickable() {
        return this.clickable;
    }
}
