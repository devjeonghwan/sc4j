package org.fogofwarlabs.sc4j.engine.lockstep;

/**
 * Names how a participant left a lockstep session ({@link PeerDeparture}).
 */
public enum DepartureKind {
    /**
     * The participant left by itself ({@link LockstepTransport#leave}) after its last turn.
     */
    LEFT,
    /**
     * The other participants stopped waiting for the participant's turns: it was overdue past the drop timeout, or a person dropped it.
     */
    DROPPED
}
