package org.fogofwarlabs.sc4j.engine.net.beaconlink;

import org.fogofwarlabs.sc4j.engine.lockstep.LockstepPeer;
import org.fogofwarlabs.sc4j.engine.net.PeerRoute;
import org.jetbrains.annotations.NotNull;

/**
 * Holds one participant of a network lockstep session over Beacon.Link with the Beacon.Link participant its datagrams go to on the overlay.
 */
public final class LinkPeer implements PeerRoute<LinkParticipant> {
    private final @NotNull LockstepPeer peer;
    private final @NotNull LinkParticipant participant;

    /**
     * Creates a {@link LinkPeer} instance.
     *
     * @param peer        the participant of the lockstep session
     * @param participant its Beacon.Link participant
     */
    public LinkPeer(@NotNull LockstepPeer peer, @NotNull LinkParticipant participant) {
        this.peer        = peer;
        this.participant = participant;
    }

    /**
     * @return the participant
     */
    @Override
    public @NotNull LockstepPeer getPeer() {
        return this.peer;
    }

    /**
     * @return the participant's address on the link: its Beacon.Link participant
     */
    @Override
    public @NotNull LinkParticipant getAddress() {
        return this.participant;
    }


    /**
     * @return the participant and its Beacon.Link participant, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return this.peer + " at " + this.participant;
    }
}
