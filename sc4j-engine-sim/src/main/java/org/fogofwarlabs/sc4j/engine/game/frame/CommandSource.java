package org.fogofwarlabs.sc4j.engine.game.frame;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.jetbrains.annotations.NotNull;

/**
 * Applies the commands due at a frame: the only point where input from players, the network or a replay changes the
 * simulation.
 * <p>
 * Determinism contract: the frame runner calls {@link #applyCommands} once per frame, before the frame counter
 * advances and before any phase, inside the active random range. The source applies the due commands in their recorded
 * order; which commands are due depends only on the turn and command stream, never on a clock of this machine.
 */
@FunctionalInterface
public interface CommandSource {
    /**
     * Applies the commands due at the frame about to run.
     *
     * @param game the game
     */
    void applyCommands(@NotNull Game game);
}
