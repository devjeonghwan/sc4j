package org.fogofwarlabs.sc4j.engine.game.system;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.jetbrains.annotations.NotNull;

/**
 * Represents one replaceable part of the simulation, created once per game.
 * <p>
 * Determinism contract:
 * <ul>
 *     <li>The game creates the systems in registration order and then calls {@link #attach} on each, in the same
 *     order, after every system, pool and the random generator exist.</li>
 *     <li>{@link #attach} only resolves what the system uses (other systems, columns, pools) into fields; it draws no
 *     random value and changes no simulation state. The world is set up afterwards, by the mod.</li>
 *     <li>A system that holds simulation state implements
 *     {@link org.fogofwarlabs.sc4j.engine.state.StateHashContributor}; the game mixes it after the engine parts, in
 *     registration order.</li>
 *     <li>A system iterates entities in pool order and its own data by index, never in a hash-dependent order, and uses
 *     no clock and no floating point.</li>
 * </ul>
 */
public interface GameSystem {
    /**
     * Resolves what this system uses from the game. Called once, after every system exists.
     *
     * @param game the game
     */
    void attach(@NotNull Game game);
}
