package org.fogofwarlabs.sc4j.engine.replay;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.command.CommandInput;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.controller.ControllerSource;
import org.fogofwarlabs.sc4j.engine.controller.PlayerController;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Plays an {@link EngineReplay} back: the {@link ControllerSource#REPLAY} controller of a replay session (the client's
 * {@code GameSessionFactory#createReplay}) of a game created from the replay's setup. Each game loop it hands over the recorded commands of
 * that loop, every player's, in their recorded order, and compares the game's state hash with the loop's checkpoint, if the recording took
 * one; a mismatch is counted and reported to its {@link CheckpointMismatchListener}. After the last recorded loop it hands over nothing.
 * <p>
 * The controller is the first controller of its session, so the checkpoints are compared where the recorder took them, at the command point
 * before any command of the loop. One controller plays one game; {@link EngineReplay#createController} creates one per playback, from
 * the first game loop or, for a seek, from the loop of a restored state snapshot.
 * <p>
 * Determinism contract: what the controller hands over at a game loop depends only on the replay.
 */
public final class ReplayController implements PlayerController {
    private static <C> void submit(@NotNull CommandType<C> type, @NotNull CommandInput input, @NotNull Player player,
                                   @NotNull CommandSink sink) throws IOException {
        sink.submit(player, type, type.read(input));
    }


    private final @NotNull List<@NotNull RecordedCommand> commands;
    private final @NotNull List<@NotNull StateCheckpoint> checkpoints;
    private final long loopCount;
    private final @Nullable CheckpointMismatchListener listener;

    private final @NotNull CommandInput input;
    private long loop;
    private int nextCommand;
    private int nextCheckpoint;
    private int mismatchCount;

    /**
     * Creates a {@link ReplayController} instance at a game loop of the replay: the first command and checkpoint it hands over or compares are
     * the first ones of that loop or later.
     *
     * @param replay    the replay
     * @param listener  the receiver of the checkpoint mismatches, or {@code null} to only count them
     * @param startLoop the game loop to start at, {@code 0} up to the replay's loop count
     */
    ReplayController(@NotNull EngineReplay replay, @Nullable CheckpointMismatchListener listener, long startLoop) {
        this.commands    = replay.getCommands();
        this.checkpoints = replay.getCheckpoints();
        this.loopCount   = replay.getLoopCount();
        this.listener    = listener;

        this.input          = new CommandInput();
        this.loop           = startLoop;
        this.nextCommand    = 0;
        this.nextCheckpoint = 0;
        this.mismatchCount  = 0;

        while (this.nextCommand < this.commands.size() && this.commands.get(this.nextCommand).getLoop() < startLoop) {
            this.nextCommand++;
        }

        while (this.nextCheckpoint < this.checkpoints.size() && this.checkpoints.get(this.nextCheckpoint).getLoop() < startLoop) {
            this.nextCheckpoint++;
        }
    }

    /**
     * @return where the commands of this controller come from: {@link ControllerSource#REPLAY}, the recorded game
     */
    @Override
    public @NotNull ControllerSource getSource() {
        return ControllerSource.REPLAY;
    }

    /**
     * @return the game loops played back so far, the start loop of a seek included
     */
    public long getLoop() {
        return this.loop;
    }

    /**
     * @return {@code true} if every recorded game loop was played back, {@code false} otherwise
     */
    public boolean isFinished() {
        return this.loop >= this.loopCount;
    }

    /**
     * @return the checkpoints the replayed game did not reach so far
     */
    public int getMismatchCount() {
        return this.mismatchCount;
    }


    /**
     * Hands over the commands due at the frame about to run, in their order: the recorded commands of the current game loop, after the loop's
     * checkpoint, if any, was compared with the game's state hash.
     *
     * @param game the game
     * @param sink the sink of the commands
     * @throws IndexOutOfBoundsException if a recorded player is not a player of the game
     */
    @Override
    public void collectCommands(@NotNull Game game, @NotNull CommandSink sink) {
        long loop = this.loop;

        while (this.nextCheckpoint < this.checkpoints.size() && this.checkpoints.get(this.nextCheckpoint).getLoop() == loop) {
            this.checkCheckpoint(game, this.checkpoints.get(this.nextCheckpoint++));
        }

        while (this.nextCommand < this.commands.size() && this.commands.get(this.nextCommand).getLoop() == loop) {
            RecordedCommand command = this.commands.get(this.nextCommand++);
            byte[] payload = command.getPayload();

            try {
                submit(command.getType(), this.input.reset(payload, 0, payload.length), game.getPlayer(command.getPlayerIndex()), sink);
            } catch (IOException exception) {
                // The format read every payload with its type when it read the replay.
                throw Exceptions.newUncheckedIOException(exception);
            }
        }

        this.loop = loop + 1;
    }

    private void checkCheckpoint(@NotNull Game game, @NotNull StateCheckpoint checkpoint) {
        long localHash = game.computeStateHash();

        if (localHash == checkpoint.getStateHash()) {
            return;
        }

        this.mismatchCount++;

        CheckpointMismatchListener mismatchListener = this.listener;

        if (mismatchListener != null) {
            mismatchListener.onCheckpointMismatch(checkpoint.getLoop(), checkpoint.getStateHash(), localHash, this.mismatchCount);
        }
    }
}
