package org.fogofwarlabs.sc4j.beaconlink.peer;

import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * Sends the datagrams of a {@link MeshPeer} through its owner's socket. A datagram that cannot be sent is dropped; requests and gossip
 * repeat themselves.
 */
@FunctionalInterface
public interface DatagramSink {
    /**
     * Sends one datagram.
     *
     * @param datagram the signed datagram
     * @param target   the receiver's address
     */
    void send(byte @NotNull [] datagram, @NotNull InetSocketAddress target);
}
