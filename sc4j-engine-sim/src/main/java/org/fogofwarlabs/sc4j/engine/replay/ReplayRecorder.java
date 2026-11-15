package org.fogofwarlabs.sc4j.engine.replay;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.command.CommandOutput;
import org.fogofwarlabs.sc4j.engine.command.CommandRecorder;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Records a running game for the engine replay format: the {@link CommandRecorder} of the game's command source
 * ({@link org.fogofwarlabs.sc4j.engine.controller.ControllerCommandSource#setRecorder}), so it sees every command the game runs, of every
 * controller (the local person, the network turns, the computer players), at the game loop it runs. Every
 * {@link #getCheckpointInterval() checkpoint interval} game loops it also takes the game's state hash, which a replay viewer compares with its
 * own. {@link #createReplay()} gives the replay recorded so far; {@link EngineReplayFormat#write} writes it.
 * <p>
 * The recorder is set on the command source before the game's first loop and used on the game's thread. It encodes each command's payload
 * when the command runs, so a command object changed later does not change the recording.
 */
public final class ReplayRecorder implements CommandRecorder {
    private final @NotNull ModIdentity identity;
    private final @NotNull GameSetup setup;
    private final int checkpointInterval;

    private final @NotNull CommandOutput payload;
    private final @NotNull List<@NotNull RecordedCommand> commands;
    private final @NotNull List<@NotNull StateCheckpoint> checkpoints;
    private long loopCount;

    /**
     * Creates a {@link ReplayRecorder} instance before the game's first loop. {@link EngineReplayFormat#createRecorder} creates it with the
     * format's identity.
     *
     * @param identity           the identity of the mod that records the game
     * @param setup              the setup the game was created from
     * @param checkpointInterval the game loops between two state hash checkpoints, {@code 0} for none
     * @throws IllegalArgumentException if the interval is negative
     */
    ReplayRecorder(@NotNull ModIdentity identity, @NotNull GameSetup setup, int checkpointInterval) {
        if (checkpointInterval < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid replay checkpoint interval: {0}", checkpointInterval);
        }

        this.identity           = identity;
        this.setup              = setup;
        this.checkpointInterval = checkpointInterval;

        this.payload     = new CommandOutput();
        this.commands    = new ArrayList<>();
        this.checkpoints = new ArrayList<>();
        this.loopCount   = 0L;
    }

    /**
     * @return the game loops between two state hash checkpoints, {@code 0} for none
     */
    public int getCheckpointInterval() {
        return this.checkpointInterval;
    }

    /**
     * @return the game loops recorded so far
     */
    public long getLoopCount() {
        return this.loopCount;
    }


    /**
     * Called at the command point of a game loop, before the loop's commands: counts the loop and, at every checkpoint interval after the
     * first loop, takes the game's state hash.
     *
     * @param game the game, in its state after the previous loop
     */
    @Override
    public void startLoop(@NotNull Game game) {
        long loop = this.loopCount;

        if (this.checkpointInterval > 0 && loop > 0 && loop % this.checkpointInterval == 0) {
            this.checkpoints.add(new StateCheckpoint(loop, game.computeStateHash()));
        }

        this.loopCount = loop + 1;
    }

    /**
     * Submits one command of a player: records it with the current game loop and its encoded payload.
     *
     * @param <C>     the command class
     * @param player  the issuing player
     * @param type    the command type
     * @param command the command
     * @throws IllegalStateException    if no game loop started yet
     * @throws IllegalArgumentException if the player, the type id or the payload does not fit the replay format
     */
    @Override
    public <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
        if (this.loopCount == 0L) {
            throw Exceptions.newIllegalStateException("A command of {0} is recorded before the first game loop.", player);
        }

        if (player.getIndex() > EngineReplayFormat.MAXIMUM_PLAYER_INDEX || type.getId() > EngineReplayFormat.MAXIMUM_FIELD_VALUE) {
            throw Exceptions.newIllegalArgumentException("The command id {0} of {1} does not fit the replay format.", type.getId(), player);
        }

        this.payload.clear();
        type.write(command, this.payload);

        if (this.payload.getLength() > EngineReplayFormat.MAXIMUM_FIELD_VALUE) {
            throw Exceptions.newIllegalArgumentException("The payload of {0} bytes of command id {1} does not fit the replay format.",
                                                         this.payload.getLength(), type.getId());
        }

        this.commands.add(new RecordedCommand(this.loopCount - 1, player.getIndex(), type, this.payload.toByteArray()));
    }

    /**
     * Creates the replay of the game recorded so far: the commands and checkpoints of the loops recorded until now.
     *
     * @return the created {@link EngineReplay} instance
     */
    public @NotNull EngineReplay createReplay() {
        return new EngineReplay(this.identity, this.setup, this.loopCount, this.commands, this.checkpoints);
    }
}
