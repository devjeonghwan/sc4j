package org.fogofwarlabs.sc4j.beaconlink.test.simulation;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.peer.MeshPeer;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

/**
 * Runs one {@link MeshPeer} on a host of a {@link SimulatedNetwork}: the network hands it the datagrams that passed its NAT and calls its
 * maintenance every tick until the peer is stopped.
 */
public final class SimulatedPeer implements SimulatedNetwork.Endpoint {
    private final @NotNull MeshPeer peer;
    private final @Nullable NatType nat;

    private @Nullable InetSocketAddress address;
    private boolean running;

    /**
     * Creates a {@link SimulatedPeer} instance; the network sets its address when the host is added.
     *
     * @param peer the peer
     * @param nat  the NAT in front of the peer, or {@code null} for a public host
     */
    SimulatedPeer(@NotNull MeshPeer peer, @Nullable NatType nat) {
        this.peer = peer;
        this.nat  = nat;

        this.address = null;
        this.running = true;
    }

    /**
     * @return the peer
     */
    public @NotNull MeshPeer getPeer() {
        return this.peer;
    }

    /**
     * @return the NAT in front of the peer, or {@code null} for a public host
     */
    public @Nullable NatType getNat() {
        return this.nat;
    }

    /**
     * @return the peer's public address (behind a symmetric NAT, the address of its first mapping is another one)
     */
    public @NotNull InetSocketAddress getAddress() {
        InetSocketAddress current = this.address;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The simulated peer has no address yet.");
        }

        return current;
    }

    /**
     * Sets the peer's public address.
     *
     * @param address the address
     */
    void setAddress(@NotNull InetSocketAddress address) {
        this.address = address;
    }

    /**
     * @return {@code true} while the peer runs, {@code false} otherwise (after the network stopped it)
     */
    public boolean isRunning() {
        return this.running;
    }


    /**
     * Records that the network stopped the peer.
     */
    void markStopped() {
        this.running = false;
    }

    /**
     * Handles a datagram that passed the host's NAT.
     *
     * @param datagram the datagram, owned by the endpoint
     * @param source   the sender's public address
     */
    @Override
    public void receive(byte @NotNull [] datagram, @NotNull InetSocketAddress source) {
        this.peer.receive(datagram, datagram.length, source);
    }

    /**
     * Runs the host's due maintenance at the end of a tick.
     */
    @Override
    public void tick() {
        this.peer.maintain();
    }
}
