package org.fogofwarlabs.sc4j.engine.client.game;

import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.controller.ControllerSource;
import org.fogofwarlabs.sc4j.engine.controller.PlayerController;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Carries the commands of the person at this machine into a local game: the client submits them as a {@link CommandSink} between frames, and
 * as the person's {@link PlayerController} the queue hands them to the game at the next frame's command point, in submission order.
 */
public final class LocalCommandQueue implements CommandSink, PlayerController {
    /**
     * Holds one queued command with its type, so that it is handed on with its own type parameter.
     *
     * @param <C> the command class
     */
    private static final class QueuedCommand<C> {
        private final @NotNull Player player;
        private final @NotNull CommandType<C> type;
        private final @NotNull C command;

        private QueuedCommand(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
            this.player  = player;
            this.type    = type;
            this.command = command;
        }

        private void submitTo(@NotNull CommandSink sink) {
            sink.submit(this.player, this.type, this.command);
        }
    }

    private final @NotNull List<@NotNull QueuedCommand<?>> commands;

    /**
     * Creates a {@link LocalCommandQueue} instance without commands.
     */
    public LocalCommandQueue() {
        this.commands = new ArrayList<>();
    }

    /**
     * @return the number of commands waiting for the next frame
     */
    public int getQueuedCount() {
        return this.commands.size();
    }

    /**
     * @return where the commands of this controller come from: {@link ControllerSource#HUMAN}, the person at this machine
     */
    @Override
    public @NotNull ControllerSource getSource() {
        return ControllerSource.HUMAN;
    }

    /**
     * Submits one command of a player: queues it for the next frame.
     *
     * @param <C>     the command class
     * @param player  the issuing player
     * @param type    the command type
     * @param command the command
     */
    @Override
    public <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
        this.commands.add(new QueuedCommand<>(player, type, command));
    }

    /**
     * Hands over the commands due at the frame about to run, in their order: every queued command in submission order; the queue is empty
     * afterwards.
     *
     * @param game the game
     * @param sink the sink of the commands
     */
    @Override
    public void collectCommands(@NotNull Game game, @NotNull CommandSink sink) {
        for (int index = 0; index < this.commands.size(); index++) {
            this.commands.get(index).submitTo(sink);
        }

        this.commands.clear();
    }
}
