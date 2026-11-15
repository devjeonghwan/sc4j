package org.fogofwarlabs.sc4j.engine.game.system;

import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.jetbrains.annotations.NotNull;

/**
 * Gives a system or hook factory of a new game its setup and the systems the game created before: a factory passes those systems to the
 * constructor of what it creates, so that parts one game shares, such as its unit bounds, are created per game. A game definition can so
 * serve any number of games, also at the same time.
 * <p>
 * Determinism contract: a factory only passes the systems on; it calls nothing on them, since they are not attached yet.
 */
public interface SystemContext {
    /**
     * @return the setup of the new game
     */
    @NotNull GameSetup getSetup();

    /**
     * Returns a system the game created before: for a system factory one registered before its own key, for a hook factory any system.
     *
     * @param <S> the system type
     * @param key the key
     * @return the created system, not attached yet
     * @throws IllegalStateException if the system of the key is not created yet or not registered
     */
    <S extends GameSystem> @NotNull S require(@NotNull SystemKey<S> key);
}
