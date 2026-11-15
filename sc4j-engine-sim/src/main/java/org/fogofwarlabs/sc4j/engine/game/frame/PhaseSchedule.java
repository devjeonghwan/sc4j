package org.fogofwarlabs.sc4j.engine.game.frame;

/**
 * Says in which frames a phase runs. Every phase runs in the frames that advance the game; the schedule adds paused
 * frames and setup frames.
 */
public enum PhaseSchedule {
    /**
     * Only the frames that advance the game.
     */
    RUNNING(false, false),
    /**
     * The frames that advance the game and the paused frames, for example scripts that can end a pause.
     */
    RUNNING_AND_PAUSED(true, false),
    /**
     * The frames that advance the game and the setup frames before the first frame.
     */
    RUNNING_AND_SETUP(false, true),
    /**
     * Every frame: advancing, paused and setup frames.
     */
    EVERY_FRAME(true, true);

    private final boolean runWhilePaused;
    private final boolean runInSetup;

    PhaseSchedule(boolean runWhilePaused, boolean runInSetup) {
        this.runWhilePaused = runWhilePaused;
        this.runInSetup     = runInSetup;
    }

    /**
     * @return {@code true} if the phase runs in paused frames, {@code false} otherwise
     */
    public boolean isRunWhilePaused() {
        return this.runWhilePaused;
    }

    /**
     * @return {@code true} if the phase runs in setup frames, {@code false} otherwise
     */
    public boolean isRunInSetup() {
        return this.runInSetup;
    }
}
