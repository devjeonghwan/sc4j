package org.fogofwarlabs.sc4j.engine.lockstep;

import org.jetbrains.annotations.NotNull;

/**
 * Receives the desyncs a {@link DesyncMonitor} detects: received turns whose sync value does not match the local one.
 */
@FunctionalInterface
public interface DesyncListener {
    /**
     * Called when a received turn does not match the local simulation.
     *
     * @param turn          the received turn
     * @param frame         the frame about to run when the turn was checked
     * @param failure       the description of the mismatch
     * @param mismatchCount the mismatches detected so far, this one included
     */
    void onDesync(@NotNull PeerTurn turn, int frame, @NotNull String failure, int mismatchCount);
}
