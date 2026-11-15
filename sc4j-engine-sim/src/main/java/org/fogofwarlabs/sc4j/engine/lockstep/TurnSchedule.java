package org.fogofwarlabs.sc4j.engine.lockstep;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Describes the turn timing of a lockstep session: a turn step every {@link #getFramesPerTurn()} game loops, and the latency in turn steps,
 * the local turns kept in transit ({@link TurnExchange}). A command submitted between two turn steps runs on every machine
 * {@link #getLatencyTurns()} turn steps after the next one.
 * <p>
 * Turn steps count game loops, not advanced frames: a paused game keeps exchanging turns, so the command that resumes it arrives.
 */
public final class TurnSchedule {
    private final int framesPerTurn;
    private final int latencyTurns;

    /**
     * Creates a {@link TurnSchedule} instance.
     *
     * @param framesPerTurn the game loops of one turn, at least {@code 1}
     * @param latencyTurns  the local turns kept in transit, at least {@code 1}
     * @throws IllegalArgumentException if a value is below {@code 1}
     */
    public TurnSchedule(int framesPerTurn, int latencyTurns) {
        if (framesPerTurn < 1 || latencyTurns < 1) {
            throw Exceptions.newIllegalArgumentException("Invalid turn schedule: {0} frames per turn, {1} turns in transit.", framesPerTurn,
                                                         latencyTurns);
        }

        this.framesPerTurn = framesPerTurn;
        this.latencyTurns  = latencyTurns;
    }

    /**
     * @return the game loops of one turn
     */
    public int getFramesPerTurn() {
        return this.framesPerTurn;
    }

    /**
     * @return the local turns kept in transit, the latency in turn steps
     */
    public int getLatencyTurns() {
        return this.latencyTurns;
    }


    /**
     * Tests whether a game loop runs a turn step.
     *
     * @param step the game loop, {@code 0} for the first
     * @return {@code true} if the loop runs a turn step, {@code false} otherwise
     */
    public boolean isTurnStep(long step) {
        return step % this.framesPerTurn == 0;
    }

    /**
     * @return the frames per turn and the latency, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return "TurnSchedule[" + this.framesPerTurn + " frames per turn, " + this.latencyTurns + " turns in transit]";
    }
}
