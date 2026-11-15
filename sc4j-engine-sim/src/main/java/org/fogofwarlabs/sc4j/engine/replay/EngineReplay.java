package org.fogofwarlabs.sc4j.engine.replay;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holds one recorded game of the engine replay format ({@link EngineReplayFormat}): the identity of the mod that recorded it, the setup the game
 * was created from, the number of game loops it ran, every command it ran with its game loop and player, and the state hash checkpoints taken
 * on the way. A {@link ReplayRecorder} creates it from a running game, {@link EngineReplayFormat#read} from a file; {@link #createController}
 * plays it back.
 * <p>
 * A replay is immutable. It names players by their slot index, which {@link org.fogofwarlabs.sc4j.engine.game.Game#getPlayer} decodes, so it is
 * not bound to the game that recorded it.
 */
public final class EngineReplay {
    private final @NotNull ModIdentity identity;
    private final @NotNull GameSetup setup;
    private final long loopCount;
    private final @NotNull List<@NotNull RecordedCommand> commands;
    private final @NotNull List<@NotNull StateCheckpoint> checkpoints;

    /**
     * Creates an {@link EngineReplay} instance.
     *
     * @param identity    the identity of the mod that recorded the game
     * @param setup       the setup of the game
     * @param loopCount   the game loops the game ran
     * @param commands    the commands in the order they ran; copied
     * @param checkpoints the state hash checkpoints in loop order; copied
     */
    EngineReplay(@NotNull ModIdentity identity, @NotNull GameSetup setup, long loopCount, @NotNull List<@NotNull RecordedCommand> commands,
                 @NotNull List<@NotNull StateCheckpoint> checkpoints) {
        this.identity    = identity;
        this.setup       = setup;
        this.loopCount   = loopCount;
        this.commands    = List.copyOf(commands);
        this.checkpoints = List.copyOf(checkpoints);
    }

    /**
     * @return the identity of the mod that recorded the game
     */
    public @NotNull ModIdentity getIdentity() {
        return this.identity;
    }

    /**
     * @return the setup the game was created from; a replay viewer creates its game from it
     */
    public @NotNull GameSetup getSetup() {
        return this.setup;
    }

    /**
     * @return the game loops the recorded game ran, paused loops included
     */
    public long getLoopCount() {
        return this.loopCount;
    }

    /**
     * @return the number of recorded commands
     */
    public int getCommandCount() {
        return this.commands.size();
    }

    /**
     * @return the number of state hash checkpoints
     */
    public int getCheckpointCount() {
        return this.checkpoints.size();
    }

    /**
     * @return the commands in the order they ran
     */
    @NotNull List<@NotNull RecordedCommand> getCommands() {
        return this.commands;
    }

    /**
     * @return the state hash checkpoints in loop order
     */
    @NotNull List<@NotNull StateCheckpoint> getCheckpoints() {
        return this.checkpoints;
    }


    /**
     * Creates the controller that plays the replay back in a replay session of a game created from {@link #getSetup()}.
     *
     * @param listener the receiver of the checkpoints the replayed game does not reach, or {@code null} to only count them
     * @return the created {@link ReplayController} instance
     */
    public @NotNull ReplayController createController(@Nullable CheckpointMismatchListener listener) {
        return new ReplayController(this, listener, 0L);
    }

    /**
     * Creates the controller that plays the replay back from a game loop on, for a seek: in a replay session of a game into which the state
     * snapshot of that loop was restored ({@link org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshot#restore}), captured after the
     * replayed or recorded game ran that many loops. The controller hands over the commands and compares the checkpoints of that loop on.
     *
     * @param listener  the receiver of the checkpoints the replayed game does not reach, or {@code null} to only count them
     * @param startLoop the game loop to start at, {@code 0} up to {@link #getLoopCount()}
     * @return the created {@link ReplayController} instance
     * @throws IllegalArgumentException if the start loop lies outside the replay
     */
    public @NotNull ReplayController createController(@Nullable CheckpointMismatchListener listener, long startLoop) {
        if (startLoop < 0 || startLoop > this.loopCount) {
            throw Exceptions.newIllegalArgumentException("The replay of {0} game loops has no loop {1}.", this.loopCount, startLoop);
        }

        return new ReplayController(this, listener, startLoop);
    }
}
