package org.fogofwarlabs.sc4j.engine.content.economy;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Holds an immutable amount per resource kind, the cost of a definition. Kinds without an amount cost {@code 0}; the
 * entries keep the order they were added in.
 */
public final class ResourceCost {
    /**
     * The cost without any resource.
     */
    public static final @NotNull ResourceCost NONE = new ResourceCost(new ResourceKind[0], new int[0]);

    /**
     * Builds {@link ResourceCost} instances.
     */
    public static final class Builder {
        private @NotNull ResourceKind @NotNull [] kinds;
        private int @NotNull [] amounts;

        /**
         * Creates a {@link Builder} instance without entries.
         */
        public Builder() {
            this.kinds   = new ResourceKind[0];
            this.amounts = new int[0];
        }

        /**
         * Adds the amount of one resource kind.
         *
         * @param kind   the resource kind
         * @param amount the amount
         * @return this instance for chaining
         * @throws IllegalArgumentException if the kind already has an amount
         */
        public @NotNull Builder add(@NotNull ResourceKind kind, int amount) {
            for (ResourceKind added : this.kinds) {
                if (added == kind) {
                    throw Exceptions.newIllegalArgumentException("The resource kind {0} already has an amount.", kind);
                }
            }

            this.kinds   = Arrays.copyOf(this.kinds, this.kinds.length + 1);
            this.amounts = Arrays.copyOf(this.amounts, this.amounts.length + 1);

            this.kinds[this.kinds.length - 1]     = kind;
            this.amounts[this.amounts.length - 1] = amount;

            return this;
        }

        /**
         * Builds a {@link ResourceCost} instance from the added entries.
         *
         * @return the built {@link ResourceCost} instance
         */
        public @NotNull ResourceCost build() {
            return new ResourceCost(this.kinds.clone(), this.amounts.clone());
        }
    }

    /**
     * Creates a {@link ResourceCost} instance of one resource kind.
     *
     * @param kind   the resource kind
     * @param amount the amount
     * @return the created {@link ResourceCost} instance
     */
    public static @NotNull ResourceCost of(@NotNull ResourceKind kind, int amount) {
        return new Builder().add(kind, amount).build();
    }

    private final @NotNull ResourceKind @NotNull [] kinds;
    private final int @NotNull [] amounts;

    private ResourceCost(@NotNull ResourceKind @NotNull [] kinds, int @NotNull [] amounts) {
        this.kinds   = kinds;
        this.amounts = amounts;
    }

    /**
     * @return the number of entries
     */
    public int getCount() {
        return this.kinds.length;
    }

    /**
     * Returns the resource kind of one entry.
     *
     * @param index the entry, in the order the entries were added
     * @return the resource kind
     */
    public @NotNull ResourceKind getKind(int index) {
        return this.kinds[index];
    }

    /**
     * Returns the amount of one entry.
     *
     * @param index the entry, in the order the entries were added
     * @return the amount
     */
    public int getAmount(int index) {
        return this.amounts[index];
    }

    /**
     * Returns the amount of a resource kind.
     *
     * @param kind the resource kind
     * @return the amount, {@code 0} if the cost has no entry of the kind
     */
    public int getAmount(@NotNull ResourceKind kind) {
        for (int index = 0; index < this.kinds.length; index++) {
            if (this.kinds[index] == kind) {
                return this.amounts[index];
            }
        }

        return 0;
    }
}
