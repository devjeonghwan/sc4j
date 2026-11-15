package org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap;

/**
 * Lists how far a peer got in joining the Beacon.Link network, for the status a user sees.
 */
public enum BootstrapState {
    /**
     * The peer knows no seed and no learned peer: a first start of a release without a seed. The user can add a seed.
     */
    NO_PEERS_KNOWN,
    /**
     * The peer greets its seeds and learned peers and waits for the first answer.
     */
    CONNECTING,
    /**
     * At least one peer answered at its address with its key.
     */
    CONNECTED,
    /**
     * No seed and no learned peer answered in time, or every peer the peer knew went quiet. The peer keeps greeting them.
     */
    NO_PEER_REACHABLE
}
