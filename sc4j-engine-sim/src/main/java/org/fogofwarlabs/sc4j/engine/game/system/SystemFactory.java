package org.fogofwarlabs.sc4j.engine.game.system;

import org.jetbrains.annotations.NotNull;

/**
 * Creates one game system for a new game.
 * <p>
 * Determinism contract: the created system depends only on the setup and the systems created before it; the factory draws no
 * random value and calls nothing on another system (that happens in {@link GameSystem#attach}), it may only pass the systems of
 * {@link SystemContext#require} to the new system.
 *
 * @param <S> the system type
 */
@FunctionalInterface
public interface SystemFactory<S extends GameSystem> {
    /**
     * Creates the system of a new game.
     *
     * @param context the setup of the game and the systems created before this one
     * @return the created system
     */
    @NotNull S create(@NotNull SystemContext context);
}
