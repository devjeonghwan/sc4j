package org.fogofwarlabs.sc4j.engine.computer;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the {@link ComputerController} of one computer slot when a {@link StandardComputerPlayerSystem} is attached to its game.
 */
@FunctionalInterface
public interface ComputerControllerFactory {
    /**
     * Creates the controller of a computer player.
     *
     * @param game   the game, with every system created
     * @param player the player of the computer slot
     * @return the created {@link ComputerController} instance, whose {@link ComputerController#getPlayer()} is the player
     */
    @NotNull ComputerController create(@NotNull Game game, @NotNull Player player);
}
