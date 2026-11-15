package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.random.DeterministicRandom;
import org.fogofwarlabs.sc4j.engine.random.RandomFactory;
import org.fogofwarlabs.sc4j.engine.random.RandomState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the inputs a game is created from: the map, the player slots, the seed or a saved random state, the options and
 * the pool sizes when they differ from the definition. Two machines that create a game from the same definition and
 * the same setup get the same game.
 */
public final class GameSetup {
    /**
     * Builds {@link GameSetup} instances.
     */
    public static final class Builder {
        private final @NotNull MapReference map;
        private final @NotNull List<@NotNull PlayerSlot> players;

        private long seed;
        private @Nullable RandomState initialRandomState;
        private @Nullable DeterministicRandom random;

        private @NotNull GameOptions options;
        private @Nullable EngineLimits limits;

        /**
         * Creates a {@link Builder} instance without players, with seed {@code 0}, the default options and the limits of
         * the definition.
         *
         * @param map the map
         */
        public Builder(@NotNull MapReference map) {
            this.map     = map;
            this.players = new ArrayList<>();

            this.seed               = 0L;
            this.initialRandomState = null;
            this.random             = null;

            this.options = GameOptions.DEFAULTS;
            this.limits  = null;
        }

        /**
         * Adds a player slot; slots are added in ascending slot index order.
         *
         * @param player the slot
         * @return this instance for chaining
         * @throws IllegalArgumentException if the slot index is not higher than the index of the previous slot
         */
        public @NotNull Builder addPlayer(@NotNull PlayerSlot player) {
            if (!this.players.isEmpty() && player.getSlotIndex() <= this.players.getLast().getSlotIndex()) {
                throw Exceptions.newIllegalArgumentException("Player slots are added in ascending order: {0} after {1}", player.getSlotIndex(),
                                                             this.players.getLast().getSlotIndex());
            }

            this.players.add(player);

            return this;
        }

        /**
         * Sets the seed the random generator is created with.
         *
         * @param seed the seed
         * @return this instance for chaining
         */
        public @NotNull Builder setSeed(long seed) {
            this.seed = seed;

            return this;
        }

        /**
         * Sets a saved random state that replaces the seeded state when the game is created, for example from a saved
         * game.
         *
         * @param initialRandomState the saved state, or {@code null} to start from the seed
         * @return this instance for chaining
         */
        public @NotNull Builder setInitialRandomState(@Nullable RandomState initialRandomState) {
            this.initialRandomState = initialRandomState;

            return this;
        }

        /**
         * Sets an existing generator that the game adopts as it is, instead of creating one from the seed: for a session
         * that draws from the game's generator before the game exists, such as a map initialization that the mod runs
         * before game creation. The generator must come from the definition's {@link RandomFactory} (or be of its
         * implementation), and a setup that carries one creates one game only.
         *
         * @param random the generator, or {@code null} to create one from the seed
         * @return this instance for chaining
         */
        public @NotNull Builder setRandom(@Nullable DeterministicRandom random) {
            this.random = random;

            return this;
        }

        /**
         * Sets the options.
         *
         * @param options the options
         * @return this instance for chaining
         */
        public @NotNull Builder setOptions(@NotNull GameOptions options) {
            this.options = options;

            return this;
        }

        /**
         * Sets pool sizes that replace the limits of the definition, for example a map that needs larger pools.
         *
         * @param limits the limits, or {@code null} for the limits of the definition
         * @return this instance for chaining
         */
        public @NotNull Builder setLimits(@Nullable EngineLimits limits) {
            this.limits = limits;

            return this;
        }


        /**
         * Builds a {@link GameSetup} instance.
         *
         * @return the built {@link GameSetup} instance
         */
        public @NotNull GameSetup build() {
            return new GameSetup(this.map, List.copyOf(this.players), this.seed, this.initialRandomState, this.random, this.options, this.limits);
        }
    }

    private final @NotNull MapReference map;
    private final @NotNull List<@NotNull PlayerSlot> players;

    private final long seed;
    private final @Nullable RandomState initialRandomState;
    private final @Nullable DeterministicRandom random;

    private final @NotNull GameOptions options;
    private final @Nullable EngineLimits limits;

    private GameSetup(@NotNull MapReference map, @NotNull List<@NotNull PlayerSlot> players,
                      long seed, @Nullable RandomState initialRandomState, @Nullable DeterministicRandom random,
                      @NotNull GameOptions options, @Nullable EngineLimits limits) {
        this.map     = map;
        this.players = players;

        this.seed               = seed;
        this.initialRandomState = initialRandomState;
        this.random             = random;

        this.options = options;
        this.limits  = limits;
    }

    /**
     * @return the map
     */
    public @NotNull MapReference getMap() {
        return this.map;
    }

    /**
     * @return the player slots in ascending slot index order
     */
    public @NotNull List<@NotNull PlayerSlot> getPlayers() {
        return this.players;
    }

    /**
     * @return the seed the random generator is created with
     */
    public long getSeed() {
        return this.seed;
    }

    /**
     * @return the saved random state that replaces the seeded state, or {@code null} to start from the seed
     */
    public @Nullable RandomState getInitialRandomState() {
        return this.initialRandomState;
    }

    /**
     * @return the existing generator the game adopts, or {@code null} to create one from the seed
     */
    public @Nullable DeterministicRandom getRandom() {
        return this.random;
    }

    /**
     * @return the options
     */
    public @NotNull GameOptions getOptions() {
        return this.options;
    }

    /**
     * @return the pool sizes that replace the limits of the definition, or {@code null} for the limits of the definition
     */
    public @Nullable EngineLimits getLimits() {
        return this.limits;
    }
}
