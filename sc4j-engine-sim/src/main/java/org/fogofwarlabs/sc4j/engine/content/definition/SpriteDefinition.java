package org.fogofwarlabs.sc4j.engine.content.definition;

import org.fogofwarlabs.sc4j.engine.content.type.ImageType;
import org.fogofwarlabs.sc4j.engine.content.type.SpriteType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the initial values of one {@link SpriteType}: what each game copies into its sprite type table when it is
 * created. A definition is immutable; type references are identities that must be registered in the same content.
 */
public final class SpriteDefinition {
    /**
     * Builds {@link SpriteDefinition} instances. Every value starts at {@code 0}, {@code false}, {@code null} or no cost
     * unless its setter says otherwise.
     */
    public static final class Builder {
        private @Nullable ImageType image;
        private int selectionSize;

        /**
         * Creates a {@link Builder} instance with the default values.
         */
        public Builder() {
            this.image         = null;
            this.selectionSize = 0;
        }

        /**
         * Sets the primary image type.
         *
         * @param image the type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setImage(@Nullable ImageType image) {
            this.image = image;

            return this;
        }

        /**
         * Sets the size of the selection marker in pixels, {@code 0} without one.
         *
         * @param selectionSize the value
         * @return this instance for chaining
         */
        public @NotNull Builder setSelectionSize(int selectionSize) {
            this.selectionSize = selectionSize;

            return this;
        }


        /**
         * Builds a {@link SpriteDefinition} instance from the current values.
         *
         * @return the built {@link SpriteDefinition} instance
         */
        public @NotNull SpriteDefinition build() {
            return new SpriteDefinition(this);
        }
    }

    private final @Nullable ImageType image;
    private final int selectionSize;

    private SpriteDefinition(@NotNull Builder builder) {
        this.image         = builder.image;
        this.selectionSize = builder.selectionSize;
    }

    /**
     * @return the primary image type, or {@code null} for none
     */
    public @Nullable ImageType getImage() {
        return this.image;
    }

    /**
     * @return the size of the selection marker in pixels, {@code 0} without one
     */
    public int getSelectionSize() {
        return this.selectionSize;
    }
}
