package org.fogofwarlabs.sc4j.engine.computer;

import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.controller.ControllerSource;
import org.fogofwarlabs.sc4j.engine.controller.PlayerController;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.order.OrderSystem;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Controls one computer player: the {@link PlayerController} of a slot that a computer plays. Its {@link ComputerPlayerSystem} creates one
 * controller per computer slot and runs it in the system's frame phase ({@link #update}).
 * <p>
 * A computer player acts in that phase, directly through the game's systems: it issues orders through {@link Game#getOrders()} (and uses
 * the production, economy and other systems the same way), and it submits no commands ({@link #collectCommands}). Every machine of a lockstep
 * game runs the same computer players on the same game state, so their actions need no command: the command stream, the network turns and
 * the replays carry the commands of people only, and a replay replays a computer player by running it again.
 * <p>
 * Determinism contract: what {@link #update} does depends only on the game state and the game's random generator; a controller allocates
 * nothing per frame.
 */
public interface ComputerController extends PlayerController {
    /**
     * @return the player this controller plays
     */
    @NotNull Player getPlayer();

    /**
     * @return where the commands of this controller come from: {@link ControllerSource#COMPUTER}
     */
    @Override
    default @NotNull ControllerSource getSource() {
        return ControllerSource.COMPUTER;
    }


    /**
     * Hands over the commands due at the frame about to run, in their order.
     * <p>
     * A computer player submits none: it acts through the game's systems when its system runs it ({@link #update}).
     *
     * @param game the game
     * @param sink the sink of the commands
     */
    @Override
    default void collectCommands(@NotNull Game game, @NotNull CommandSink sink) {
    }

    /**
     * Runs the decisions of this computer player at the point of the frame where its system's schedule reaches it: the player issues its
     * orders ({@link OrderSystem}) and uses the other systems of the game.
     *
     * @param game the game
     */
    void update(@NotNull Game game);
}
