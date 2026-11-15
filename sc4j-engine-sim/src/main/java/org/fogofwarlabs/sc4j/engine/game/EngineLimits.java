package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.entity.EntityKind;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the pool sizes of a game, which the mod decides: the entity pools of every game (units, sprites, images,
 * projectiles) and the capacities the order and path systems size their own storage by.
 */
public final class EngineLimits {
    /**
     * The limits of a definition that sets none: every pool is empty.
     */
    public static final @NotNull EngineLimits EMPTY = new Builder().build();

    /**
     * Builds {@link EngineLimits} instances; every limit starts at {@code 0}.
     */
    public static final class Builder {
        private int units;
        private int sprites;
        private int images;
        private int orders;
        private int paths;
        private int projectiles;

        /**
         * Creates a {@link Builder} instance with every limit at {@code 0}.
         */
        public Builder() {
            this.units       = 0;
            this.sprites     = 0;
            this.images      = 0;
            this.orders      = 0;
            this.paths       = 0;
            this.projectiles = 0;
        }

        /**
         * Sets the capacity of the unit pool.
         *
         * @param units the capacity, {@code 0} up to {@link EntityPool#MAXIMUM_CAPACITY}
         * @return this instance for chaining
         */
        public @NotNull Builder setUnits(int units) {
            this.units = units;

            return this;
        }

        /**
         * Sets the capacity of the sprite pool.
         *
         * @param sprites the capacity, {@code 0} up to {@link EntityPool#MAXIMUM_CAPACITY}
         * @return this instance for chaining
         */
        public @NotNull Builder setSprites(int sprites) {
            this.sprites = sprites;

            return this;
        }

        /**
         * Sets the capacity of the image pool.
         *
         * @param images the capacity, {@code 0} up to {@link EntityPool#MAXIMUM_CAPACITY}
         * @return this instance for chaining
         */
        public @NotNull Builder setImages(int images) {
            this.images = images;

            return this;
        }

        /**
         * Sets the capacity of the order storage.
         *
         * @param orders the capacity, at least {@code 0}
         * @return this instance for chaining
         */
        public @NotNull Builder setOrders(int orders) {
            this.orders = orders;

            return this;
        }

        /**
         * Sets the capacity of the path storage.
         *
         * @param paths the capacity, at least {@code 0}
         * @return this instance for chaining
         */
        public @NotNull Builder setPaths(int paths) {
            this.paths = paths;

            return this;
        }

        /**
         * Sets the capacity of the projectile pool.
         *
         * @param projectiles the capacity, {@code 0} up to {@link EntityPool#MAXIMUM_CAPACITY}
         * @return this instance for chaining
         */
        public @NotNull Builder setProjectiles(int projectiles) {
            this.projectiles = projectiles;

            return this;
        }


        /**
         * Builds an {@link EngineLimits} instance.
         *
         * @return the built {@link EngineLimits} instance
         * @throws IllegalArgumentException if a pool capacity is out of range or a capacity is negative
         */
        public @NotNull EngineLimits build() {
            checkPool("units", this.units);
            checkPool("sprites", this.sprites);
            checkPool("images", this.images);
            checkPool("projectiles", this.projectiles);

            if (this.orders < 0 || this.paths < 0) {
                throw Exceptions.newIllegalArgumentException("Negative limit: orders {0}, paths {1}", this.orders, this.paths);
            }

            return new EngineLimits(this.units, this.sprites, this.images, this.orders, this.paths, this.projectiles);
        }
    }

    private static void checkPool(@NotNull String name, int capacity) {
        if (capacity < 0 || capacity > EntityPool.MAXIMUM_CAPACITY) {
            throw Exceptions.newIllegalArgumentException("The {0} limit must be 0..{1}: {2}", name, EntityPool.MAXIMUM_CAPACITY, capacity);
        }
    }

    private final int units;
    private final int sprites;
    private final int images;
    private final int orders;
    private final int paths;
    private final int projectiles;

    private EngineLimits(int units, int sprites, int images, int orders, int paths, int projectiles) {
        this.units       = units;
        this.sprites     = sprites;
        this.images      = images;
        this.orders      = orders;
        this.paths       = paths;
        this.projectiles = projectiles;
    }

    /**
     * @return the capacity of the unit pool
     */
    public int getUnits() {
        return this.units;
    }

    /**
     * @return the capacity of the sprite pool
     */
    public int getSprites() {
        return this.sprites;
    }

    /**
     * @return the capacity of the image pool
     */
    public int getImages() {
        return this.images;
    }

    /**
     * @return the capacity of the order storage
     */
    public int getOrders() {
        return this.orders;
    }

    /**
     * @return the capacity of the path storage
     */
    public int getPaths() {
        return this.paths;
    }

    /**
     * @return the capacity of the projectile pool
     */
    public int getProjectiles() {
        return this.projectiles;
    }

    /**
     * Returns the capacity of the pool of an entity kind.
     *
     * @param kind the entity kind
     * @return the capacity of its pool
     */
    public int getCapacity(@NotNull EntityKind kind) {
        return switch (kind) {
            case UNIT -> this.units;
            case SPRITE -> this.sprites;
            case IMAGE -> this.images;
            case PROJECTILE -> this.projectiles;
        };
    }
}
