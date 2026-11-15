package org.fogofwarlabs.sc4j.engine.content.definition;

import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.TechType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the initial values of one {@link OrderType}: what each game copies into its order type table when it is
 * created. A definition is immutable; type references are identities that must be registered in the same content.
 */
public final class OrderDefinition {
    /**
     * Builds {@link OrderDefinition} instances. Every value starts at {@code 0}, {@code false}, {@code null} or no cost
     * unless its setter says otherwise.
     */
    public static final class Builder {
        private @Nullable WeaponType weapon;
        private @Nullable TechType tech;
        private boolean interruptible;
        private boolean queueable;

        /**
         * Creates a {@link Builder} instance with the default values.
         */
        public Builder() {
            this.weapon        = null;
            this.tech          = null;
            this.interruptible = false;
            this.queueable     = false;
        }

        /**
         * Sets the weapon the order uses.
         *
         * @param weapon the type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setWeapon(@Nullable WeaponType weapon) {
            this.weapon = weapon;

            return this;
        }

        /**
         * Sets the tech the order uses.
         *
         * @param tech the type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setTech(@Nullable TechType tech) {
            this.tech = tech;

            return this;
        }

        /**
         * Sets whether a new order replaces the order at once.
         *
         * @param interruptible the value
         * @return this instance for chaining
         */
        public @NotNull Builder setInterruptible(boolean interruptible) {
            this.interruptible = interruptible;

            return this;
        }

        /**
         * Sets whether the order can be queued after others.
         *
         * @param queueable the value
         * @return this instance for chaining
         */
        public @NotNull Builder setQueueable(boolean queueable) {
            this.queueable = queueable;

            return this;
        }


        /**
         * Builds an {@link OrderDefinition} instance from the current values.
         *
         * @return the built {@link OrderDefinition} instance
         */
        public @NotNull OrderDefinition build() {
            return new OrderDefinition(this);
        }
    }

    private final @Nullable WeaponType weapon;
    private final @Nullable TechType tech;
    private final boolean interruptible;
    private final boolean queueable;

    private OrderDefinition(@NotNull Builder builder) {
        this.weapon        = builder.weapon;
        this.tech          = builder.tech;
        this.interruptible = builder.interruptible;
        this.queueable     = builder.queueable;
    }

    /**
     * @return the weapon the order uses, or {@code null} for none
     */
    public @Nullable WeaponType getWeapon() {
        return this.weapon;
    }

    /**
     * @return the tech the order uses, or {@code null} for none
     */
    public @Nullable TechType getTech() {
        return this.tech;
    }

    /**
     * @return {@code true} if a new order replaces the order at once, {@code false} otherwise
     */
    public boolean isInterruptible() {
        return this.interruptible;
    }

    /**
     * @return {@code true} if the order can be queued after others, {@code false} otherwise
     */
    public boolean isQueueable() {
        return this.queueable;
    }
}
