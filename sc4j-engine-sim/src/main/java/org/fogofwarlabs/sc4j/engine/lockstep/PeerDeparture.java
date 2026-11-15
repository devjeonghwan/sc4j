package org.fogofwarlabs.sc4j.engine.lockstep;

import org.jetbrains.annotations.NotNull;

/**
 * Holds the departure of one participant from a lockstep session as its transport reports it ({@link LockstepTransport#takeDepartures()}):
 * the participant, how it left and its departure reason.
 * <p>
 * Determinism contract: a transport reports a departure at the receive that is the first one without a turn of the participant, the same
 * turn step on every machine. The kind and the reason are for messages: a machine that saw the participant's own departure notice reports
 * {@link DepartureKind#LEFT}, a machine that did not may report {@link DepartureKind#DROPPED}, so the game reacts to every departure with one
 * rule.
 */
public final class PeerDeparture {
    /**
     * The reason of a departure without a reason of the participant's own, a drop.
     */
    public static final int NO_REASON = 0;

    private final @NotNull LockstepPeer peer;
    private final @NotNull DepartureKind kind;
    private final int reason;

    /**
     * Creates a {@link PeerDeparture} instance.
     *
     * @param peer   the participant that left
     * @param kind   how it left
     * @param reason the departure reason it sent, the session's own code, or {@link #NO_REASON}
     */
    public PeerDeparture(@NotNull LockstepPeer peer, @NotNull DepartureKind kind, int reason) {
        this.peer   = peer;
        this.kind   = kind;
        this.reason = reason;
    }

    /**
     * @return the participant that left
     */
    public @NotNull LockstepPeer getPeer() {
        return this.peer;
    }

    /**
     * @return how the participant left
     */
    public @NotNull DepartureKind getKind() {
        return this.kind;
    }

    /**
     * @return the departure reason the participant sent, the session's own code, or {@link #NO_REASON}
     */
    public int getReason() {
        return this.reason;
    }


    /**
     * @return the participant, the kind and the reason, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return this.peer + " " + this.kind + " (" + this.reason + ")";
    }
}
