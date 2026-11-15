package org.fogofwarlabs.sc4j.engine.lockstep;

import org.jetbrains.annotations.NotNull;

/**
 * Holds one participant's turn as a {@link LockstepTransport} received it: the sending peer and the turn bytes, which the transport carries
 * unchanged. The turn keeps the supplied array without copying.
 */
public final class PeerTurn {
    private final @NotNull LockstepPeer peer;
    private final byte @NotNull [] turn;

    /**
     * Creates a {@link PeerTurn} instance.
     *
     * @param peer the sending peer
     * @param turn the turn bytes, kept without copying
     */
    public PeerTurn(@NotNull LockstepPeer peer, byte @NotNull [] turn) {
        this.peer = peer;
        this.turn = turn;
    }

    /**
     * @return the sending peer
     */
    public @NotNull LockstepPeer getPeer() {
        return this.peer;
    }

    /**
     * @return the turn bytes, without copying
     */
    public byte @NotNull [] getTurn() {
        return this.turn;
    }
}
