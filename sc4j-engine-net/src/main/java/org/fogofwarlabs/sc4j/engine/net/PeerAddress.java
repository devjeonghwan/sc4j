package org.fogofwarlabs.sc4j.engine.net;

import org.fogofwarlabs.sc4j.engine.lockstep.LockstepPeer;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * Holds one participant of a network lockstep session on a local network with the address its UDP datagram endpoint receives at.
 */
public final class PeerAddress implements PeerRoute<InetSocketAddress> {
    private final @NotNull LockstepPeer peer;
    private final @NotNull InetSocketAddress address;

    /**
     * Creates a {@link PeerAddress} instance.
     *
     * @param peer    the participant
     * @param address the address of its datagram endpoint
     */
    public PeerAddress(@NotNull LockstepPeer peer, @NotNull InetSocketAddress address) {
        this.peer    = peer;
        this.address = address;
    }

    /**
     * @return the participant
     */
    @Override
    public @NotNull LockstepPeer getPeer() {
        return this.peer;
    }

    /**
     * @return the participant's address on the link: the address of its datagram endpoint
     */
    @Override
    public @NotNull InetSocketAddress getAddress() {
        return this.address;
    }


    /**
     * @return the participant and its address, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return this.peer + " at " + this.address;
    }
}
