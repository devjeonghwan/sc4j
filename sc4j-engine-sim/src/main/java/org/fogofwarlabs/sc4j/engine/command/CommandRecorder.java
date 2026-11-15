package org.fogofwarlabs.sc4j.engine.command;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.jetbrains.annotations.NotNull;

/**
 * Receives a copy of every command a game's command source applies, at the command point of each game loop: a tee at the command source, for
 * example a replay recorder. The source calls {@link #startLoop} at the command point of every game loop, before the loop's first command,
 * then {@link #submit} for each command of the loop before it executes, in execution order.
 * <p>
 * Determinism contract: a recorder only reads; it changes neither the game nor the commands, so a game runs the same with or without it.
 */
public interface CommandRecorder extends CommandSink {
    /**
     * Called at the command point of a game loop, before the loop's commands.
     *
     * @param game the game, in its state after the previous loop
     */
    void startLoop(@NotNull Game game);
}
