package org.fogofwarlabs.sc4j.engine.game.frame;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.jetbrains.annotations.NotNull;

/**
 * Represents one step of a frame. The mod lists the phases in the order they run; a phase is either a stateless object
 * or a game system that is also a phase.
 * <p>
 * Determinism contract: a phase runs inside the active random range of its frame, reads the game and its own fields
 * only, iterates entities in pool order, allocates nothing and uses no clock and no floating point. Its
 * {@link #getSchedule()} is read once, when the game is created.
 */
public interface FramePhase {
    /**
     * @return the frames this phase runs in; {@link PhaseSchedule#RUNNING} unless overridden
     */
    default @NotNull PhaseSchedule getSchedule() {
        return PhaseSchedule.RUNNING;
    }


    /**
     * Runs this phase of the current frame.
     *
     * @param game the game
     */
    void run(@NotNull Game game);
}
