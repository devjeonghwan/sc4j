package org.fogofwarlabs.sc4j.engine.controller;

import org.fogofwarlabs.sc4j.engine.command.CommandRecorder;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.frame.CommandSource;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Applies the commands of a fixed list of {@link PlayerController}s at each frame: the engine default {@link CommandSource}. Each frame it asks
 * the controllers in list order and executes every submitted command at once, in submission order ({@link CommandType#execute}). A
 * {@link CommandRecorder} set on the source ({@link #setRecorder}) gets every game loop's start and a copy of every command before it executes,
 * for example to record a replay.
 * <p>
 * Determinism contract: the controller order is the list order, fixed when the source is created; commands run in the order the controllers
 * submit them, before the frame's phases.
 */
public final class ControllerCommandSource implements CommandSource {
    /**
     * Executes the commands it receives.
     */
    private final class ExecutingSink implements CommandSink {
        private final @NotNull Game game;

        private ExecutingSink(@NotNull Game game) {
            this.game = game;
        }

        /**
         * Submits one command of a player: hands a copy to the source's recorder, if one is set, and executes it now.
         *
         * @param <C>     the command class
         * @param player  the issuing player
         * @param type    the command type
         * @param command the command
         */
        @Override
        public <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
            CommandRecorder recorder = ControllerCommandSource.this.recorder;

            if (recorder != null) {
                recorder.submit(player, type, command);
            }

            type.execute(this.game, player, command);
        }
    }

    private final @NotNull List<@NotNull PlayerController> controllers;

    private @Nullable ExecutingSink sink;
    private @Nullable CommandRecorder recorder;

    /**
     * Creates a {@link ControllerCommandSource} instance without recorder.
     *
     * @param controllers the controllers in the order they are asked; copied
     */
    public ControllerCommandSource(@NotNull List<@NotNull PlayerController> controllers) {
        this.controllers = List.copyOf(controllers);

        this.sink     = null;
        this.recorder = null;
    }

    /**
     * @return the controllers in the order they are asked
     */
    public @NotNull List<@NotNull PlayerController> getControllers() {
        return this.controllers;
    }

    /**
     * @return the recorder of the applied commands, or {@code null} if none is set
     */
    public @Nullable CommandRecorder getRecorder() {
        return this.recorder;
    }

    /**
     * Sets the recorder of the applied commands: from the next game loop on it gets every loop's start and every command. A replay recorder is
     * set before the game's first loop.
     *
     * @param recorder the recorder, or {@code null} to record nothing
     */
    public void setRecorder(@Nullable CommandRecorder recorder) {
        this.recorder = recorder;
    }

    /**
     * Applies the commands due at the frame about to run: those of every controller, in controller order, after the recorder, if one is set,
     * got the loop's start. The sink of a game is created at its first frame and kept.
     *
     * @param game the game
     */
    @Override
    public void applyCommands(@NotNull Game game) {
        ExecutingSink sink = this.sink;
        CommandRecorder recorder = this.recorder;

        if (sink == null || sink.game != game) {
            sink = new ExecutingSink(game);

            this.sink = sink;
        }

        if (recorder != null) {
            recorder.startLoop(game);
        }

        for (PlayerController controller : this.controllers) {
            controller.collectCommands(game, sink);
        }
    }
}
