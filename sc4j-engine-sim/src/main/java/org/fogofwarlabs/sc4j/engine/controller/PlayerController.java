package org.fogofwarlabs.sc4j.engine.controller;

import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.jetbrains.annotations.NotNull;

/**
 * Supplies the commands of players for each frame: a person at this machine, a network session, a replay or a computer
 * player ({@link ControllerSource}). The frame's command source asks every controller once per frame, in a fixed order,
 * and each hands over the commands due at that frame.
 * <p>
 * Determinism contract: what a controller submits for a frame depends only on its command stream (recorded input, the
 * turns of the network session, the replay) or, for a computer player, on the game; never on a clock of this machine.
 */
public interface PlayerController {
    /**
     * @return where the commands of this controller come from
     */
    @NotNull ControllerSource getSource();

    /**
     * Hands over the commands due at the frame about to run, in their order.
     *
     * @param game the game
     * @param sink the sink of the commands
     */
    void collectCommands(@NotNull Game game, @NotNull CommandSink sink);
}
