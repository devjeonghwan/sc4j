package org.fogofwarlabs.sc4j.engine.net;

import org.fogofwarlabs.sc4j.engine.lockstep.LockstepPeer;
import org.jetbrains.annotations.NotNull;

/**
 * Names where one participant of a network lockstep session receives its datagrams: the participant and its address on the network's
 * {@link DatagramLink}, the endpoint address of a UDP participant ({@link PeerAddress}) or the Beacon.Link participant of an overlay
 * ({@link org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkPeer}).
 *
 * @param <A> the type of the addresses
 */
public interface PeerRoute<A> {
    /**
     * @return the participant
     */
    @NotNull LockstepPeer getPeer();

    /**
     * @return the participant's address on the link
     */
    @NotNull A getAddress();
}
