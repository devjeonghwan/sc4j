package org.fogofwarlabs.sc4j.engine.random;

import org.jetbrains.annotations.NotNull;

/**
 * Creates the random generator of a game. A mod registers one when it must reproduce a generator of its own.
 * <p>
 * Determinism contract: the created generator depends only on the seed.
 */
@FunctionalInterface
public interface RandomFactory {
    /**
     * Creates the generator of a new game.
     *
     * @param seed the seed of the game setup
     * @return the created generator
     */
    @NotNull DeterministicRandom create(long seed);
}
