package org.fogofwarlabs.sc4j.engine.hook;

import org.fogofwarlabs.sc4j.engine.game.system.SystemContext;
import org.jetbrains.annotations.NotNull;

/**
 * Creates one hook for a new game.
 * <p>
 * Determinism contract: the created hook depends only on the setup and the game's systems; the factory draws no random value
 * and calls nothing on a system, it may only pass the systems of {@link SystemContext#require} to the hook or return one of
 * them as the hook. A hook that needs pools or attached systems also implements
 * {@link org.fogofwarlabs.sc4j.engine.game.system.GameSystem} and resolves them when the game attaches it, after every system
 * is attached.
 *
 * @param <H> the hook interface
 */
@FunctionalInterface
public interface HookFactory<H> {
    /**
     * Creates the hook of a new game.
     *
     * @param context the setup of the game and every system of the game, created and not attached yet
     * @return the created hook
     */
    @NotNull H create(@NotNull SystemContext context);
}
