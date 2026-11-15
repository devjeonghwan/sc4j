package org.fogofwarlabs.sc4j.beaconlink.peer.routing;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.PeerContact;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * Holds what a peer knows of another peer that proved its address and key (a full peer in the routing table, or a leaf): its contact, whether
 * it says that strangers reach it, when it was last heard from, the outstanding hello and the requests it left unanswered in a row.
 */
final class RoutingContact {
    /**
     * The request number of no outstanding hello.
     */
    static final long NO_REQUEST = -1L;

    private final @NotNull PeerContact contact;

    private boolean full;
    private long heardMillis;
    private long helloRequest;
    private long helloMillis;
    private int failures;

    /**
     * Creates a {@link RoutingContact} instance of a peer that was just heard from at its address.
     *
     * @param contact     the peer's key, proof nonce and address
     * @param full        whether the peer says that strangers reach it
     * @param heardMillis the current time
     */
    RoutingContact(@NotNull PeerContact contact, boolean full, long heardMillis) {
        this.contact = contact;

        this.full         = full;
        this.heardMillis  = heardMillis;
        this.helloRequest = NO_REQUEST;
        this.helloMillis  = heardMillis;
        this.failures     = 0;
    }

    /**
     * @return the peer's key, proof nonce and address
     */
    @NotNull PeerContact getContact() {
        return this.contact;
    }

    /**
     * @return the peer's key
     */
    @NotNull IdentityKey getKey() {
        return this.contact.getKey();
    }

    /**
     * @return the peer's ID
     */
    @NotNull PeerId getId() {
        return this.contact.getId();
    }

    /**
     * @return the peer's address
     */
    @NotNull InetSocketAddress getAddress() {
        return this.contact.getAddress();
    }

    /**
     * @return the peer's key and address
     */
    @NotNull NodeEndpoint getEndpoint() {
        return this.contact.getEndpoint();
    }

    /**
     * @return {@code true} if the peer says that strangers reach it, {@code false} otherwise (a leaf)
     */
    boolean isFull() {
        return this.full;
    }

    /**
     * Sets whether the peer says that strangers reach it.
     *
     * @param full the peer's last claim
     */
    void setFull(boolean full) {
        this.full = full;
    }

    /**
     * @return the time the peer was last heard from at its address
     */
    long getHeardMillis() {
        return this.heardMillis;
    }

    /**
     * @return the request number of the outstanding hello, or {@link #NO_REQUEST}
     */
    long getHelloRequest() {
        return this.helloRequest;
    }

    /**
     * @return the time the last hello was sent
     */
    long getHelloMillis() {
        return this.helloMillis;
    }

    /**
     * @return the number of requests the peer left unanswered in a row
     */
    int getFailures() {
        return this.failures;
    }


    /**
     * Records that the peer answered or sent a request at its address.
     *
     * @param nowMillis the current time
     */
    void markHeard(long nowMillis) {
        this.heardMillis  = nowMillis;
        this.helloRequest = NO_REQUEST;
        this.failures     = 0;
    }

    /**
     * Records a sent hello; a hello still outstanding counts as an unanswered request.
     *
     * @param request   the request number
     * @param nowMillis the current time
     */
    void startHello(long request, long nowMillis) {
        if (this.helloRequest != NO_REQUEST) {
            this.failures++;
        }

        this.helloRequest = request;
        this.helloMillis  = nowMillis;
    }

    /**
     * Records a request the peer did not answer in time.
     */
    void countFailure() {
        this.failures++;
    }
}
