package org.fogofwarlabs.sc4j.engine.content.definition;

import org.fogofwarlabs.sc4j.engine.content.type.SoundType;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the initial values of one {@link SoundType}: what each game copies into its sound type table when it is
 * created. A definition is immutable; type references are identities that must be registered in the same content.
 */
public final class SoundDefinition {
    /**
     * Builds {@link SoundDefinition} instances. Every value starts at {@code 0}, {@code false}, {@code null} or no cost
     * unless its setter says otherwise.
     */
    public static final class Builder {
        private int priority;
        private int volume;

        /**
         * Creates a {@link Builder} instance with the default values.
         */
        public Builder() {
            this.priority = 0;
            this.volume   = 0;
        }

        /**
         * Sets the priority; a sound of higher priority replaces one of lower priority.
         *
         * @param priority the value
         * @return this instance for chaining
         */
        public @NotNull Builder setPriority(int priority) {
            this.priority = priority;

            return this;
        }

        /**
         * Sets the volume in percent.
         *
         * @param volume the value
         * @return this instance for chaining
         */
        public @NotNull Builder setVolume(int volume) {
            this.volume = volume;

            return this;
        }


        /**
         * Builds a {@link SoundDefinition} instance from the current values.
         *
         * @return the built {@link SoundDefinition} instance
         */
        public @NotNull SoundDefinition build() {
            return new SoundDefinition(this);
        }
    }

    private final int priority;
    private final int volume;

    private SoundDefinition(@NotNull Builder builder) {
        this.priority = builder.priority;
        this.volume   = builder.volume;
    }

    /**
     * @return the priority; a sound of higher priority replaces one of lower priority
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * @return the volume in percent
     */
    public int getVolume() {
        return this.volume;
    }
}
