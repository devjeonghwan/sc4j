package org.fogofwarlabs.sc4j.engine.command;

import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Receives the commands of players: what a player controller hands over each frame, and what an input layer, a network
 * session or a replay reader submits. A sink executes the commands, queues them for the next turn, encodes them for the
 * network or records them; it receives them in the order they were submitted and keeps that order.
 * <p>
 * The submit method is generic, so a sink is a class, not a lambda.
 */
public interface CommandSink {
    /**
     * Submits one command of a player.
     *
     * @param <C>     the command class
     * @param player  the issuing player
     * @param type    the command type
     * @param command the command
     */
    <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command);
}
