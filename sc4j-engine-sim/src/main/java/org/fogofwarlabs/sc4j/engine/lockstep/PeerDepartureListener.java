package org.fogofwarlabs.sc4j.engine.lockstep;

import org.jetbrains.annotations.NotNull;

/**
 * Receives the departures a {@link LockstepSession} takes from its transport: participants that left or were dropped, and the local
 * participant when the others dropped it.
 */
@FunctionalInterface
public interface PeerDepartureListener {
    /**
     * Called when the session's transport reports a departure.
     *
     * @param departure the departure
     * @param frame     the frame about to run when the departure was taken, the same on every machine for another participant's departure
     */
    void onDeparture(@NotNull PeerDeparture departure, int frame);
}
