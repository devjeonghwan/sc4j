package org.fogofwarlabs.sc4j.engine.game.frame;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.random.DeterministicRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs the frames of one game inside the engine frame skeleton (see the package description): random range, command
 * application point, frame counter, phases and sync hash point.
 * <p>
 * A frame whose command source or phase throws fails the game: the random generator becomes inactive, the exception
 * propagates and every later frame is refused, because the game state is then only partly updated.
 */
public final class FrameRunner {
    private final @NotNull Game game;
    private final @NotNull FramePhaseList phases;

    private @Nullable CommandSource commandSource;
    private @Nullable SyncHashListener syncHashListener;
    private int syncHashInterval;
    private boolean failed;

    /**
     * Creates a {@link FrameRunner} instance without command source and sync hash listener. The game creates its runner.
     *
     * @param game   the game
     * @param phases the phases of the game's frames
     */
    public FrameRunner(@NotNull Game game, @NotNull FramePhaseList phases) {
        this.game   = game;
        this.phases = phases;

        this.commandSource    = null;
        this.syncHashListener = null;
        this.syncHashInterval = 0;
        this.failed           = false;
    }

    /**
     * @return the phases of the game's frames
     */
    public @NotNull FramePhaseList getPhases() {
        return this.phases;
    }

    /**
     * @return the source of the commands, or {@code null} if the game runs without commands
     */
    public @Nullable CommandSource getCommandSource() {
        return this.commandSource;
    }

    /**
     * Sets the source of the commands, for example the lockstep turn buffer or a replay.
     *
     * @param commandSource the source, or {@code null} to run without commands
     */
    public void setCommandSource(@Nullable CommandSource commandSource) {
        this.commandSource = commandSource;
    }

    /**
     * Sets the sync hash point: the listener receives the state hash after every frame whose number is a multiple of the
     * interval.
     *
     * @param listener the listener, or {@code null} to compute no hash
     * @param interval the number of frames between two hashes, at least {@code 1}
     * @throws IllegalArgumentException if the interval is lower than {@code 1}
     */
    public void setSyncHashListener(@Nullable SyncHashListener listener, int interval) {
        if (interval < 1) {
            throw Exceptions.newIllegalArgumentException("The sync hash interval must be at least 1: {0}", interval);
        }

        this.syncHashListener = listener;
        this.syncHashInterval = interval;
    }

    /**
     * @return {@code true} if a frame failed and the game can run no further frame, {@code false} otherwise
     */
    public boolean isFailed() {
        return this.failed;
    }


    /**
     * Runs one frame: activates the random generator, applies the due commands, advances the frame counter and runs
     * the running phases (or, while paused, only the phases that run while paused), deactivates the random generator
     * and then, after a frame that advanced the counter, reaches the sync hash point.
     *
     * @throws IllegalStateException if an earlier frame failed
     */
    public void runFrame() {
        this.requireUsable();

        DeterministicRandom random = this.game.getRandom();
        FrameClock clock = this.game.getClock();
        boolean advanced = false;
        boolean completed = false;

        random.setActive(true);

        try {
            CommandSource commands = this.commandSource;

            if (commands != null) {
                commands.applyCommands(this.game);
            }

            if (clock.isPaused()) {
                this.phases.runPaused(this.game);
            } else {
                clock.advance();
                advanced = true;

                this.phases.runRunning(this.game);
            }

            completed = true;
        } finally {
            random.setActive(false);

            if (!completed) {
                this.failed = true;
            }
        }

        SyncHashListener listener = this.syncHashListener;

        if (advanced && listener != null && clock.getFrame() % this.syncHashInterval == 0) {
            listener.onSyncHash(clock.getFrame(), this.game.computeStateHash());
        }
    }

    /**
     * Runs one setup frame: the setup phases inside an active random range, without commands, frame counter or sync
     * hash.
     *
     * @throws IllegalStateException if an earlier frame failed
     */
    public void runSetupFrame() {
        this.requireUsable();

        DeterministicRandom random = this.game.getRandom();
        boolean completed = false;

        random.setActive(true);

        try {
            this.phases.runSetup(this.game);

            completed = true;
        } finally {
            random.setActive(false);

            if (!completed) {
                this.failed = true;
            }
        }
    }

    private void requireUsable() {
        if (this.failed) {
            throw Exceptions.newIllegalStateException("An earlier frame failed; the game cannot run further frames.");
        }
    }
}
