package org.fogofwarlabs.sc4j.engine.lockstep;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detects desyncs of a lockstep session: it checks every received turn with the session's {@link SyncCheck}, counts the mismatches and
 * reports each one to its {@link DesyncListener}. A mismatch does not stop the session; what to do about it (a message, a log, ending the
 * game) is the listener's decision.
 */
public final class DesyncMonitor {
    private final @NotNull SyncCheck check;
    private final @Nullable DesyncListener listener;

    private int mismatchCount;

    /**
     * Creates a {@link DesyncMonitor} instance without mismatches.
     *
     * @param check    the check of the received turns
     * @param listener the receiver of the detected desyncs, or {@code null} to only count them
     */
    public DesyncMonitor(@NotNull SyncCheck check, @Nullable DesyncListener listener) {
        this.check    = check;
        this.listener = listener;

        this.mismatchCount = 0;
    }

    /**
     * @return the received turns that did not match the local simulation so far
     */
    public int getMismatchCount() {
        return this.mismatchCount;
    }


    /**
     * Checks one received turn and reports a mismatch.
     *
     * @param turn  the received turn
     * @param frame the frame about to run
     * @return {@code true} if the turn matches or carries no sync value, {@code false} otherwise
     */
    public boolean check(@NotNull PeerTurn turn, int frame) {
        String failure = this.check.check(turn);

        if (failure == null) {
            return true;
        }

        this.mismatchCount++;

        DesyncListener desyncListener = this.listener;

        if (desyncListener != null) {
            desyncListener.onDesync(turn, frame, failure, this.mismatchCount);
        }

        return false;
    }
}
