package org.fogofwarlabs.sc4j.beaconlink.peer.reachability;

/**
 * Lists what a peer knows of its own reachability from the Internet, which decides its role: a reachable peer is a full peer that strangers
 * can contact first (it serves as a seed and a home peer for others); any other peer is a leaf that takes part through the full peers it
 * contacts itself.
 */
public enum Reachability {
    /**
     * No dial-back has given an answer and no unrequested message arrived yet: the peer has no helper with a third peer, or the test runs.
     */
    NOT_VERIFIED,
    /**
     * A probe of a dial-back or an unrequested hello arrived from an address the peer never sent to: its NAT or firewall lets strangers
     * through.
     */
    REACHABLE,
    /**
     * A helper sent a probe to the peer's observed address and it did not arrive: a NAT or a firewall drops datagrams of strangers.
     */
    NOT_REACHABLE;

    /**
     * @return {@code true} if the peer is a full peer that strangers can contact first, {@code false} otherwise (it is a leaf)
     */
    public boolean isFullPeer() {
        return this == REACHABLE;
    }
}
