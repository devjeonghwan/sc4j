package org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * Holds one entry of the learned peers ({@link PeerStore}): the peer's key and address, when it was first seen and last answered, and the
 * number of starts in a row it was tried without answering.
 */
public final class LearnedPeer {
    /**
     * The last success time of a peer that another peer reported and that never answered itself.
     */
    public static final long NEVER_ANSWERED = 0L;

    private final @NotNull NodeEndpoint endpoint;
    private final long firstSeenMillis;
    private final long lastSuccessMillis;
    private final int failedStarts;

    /**
     * Creates a {@link LearnedPeer} instance.
     *
     * @param endpoint          the peer's key and address
     * @param firstSeenMillis   the time the peer was first seen
     * @param lastSuccessMillis the time the peer last answered, or {@link #NEVER_ANSWERED}
     * @param failedStarts      the starts in a row in which the peer was tried and did not answer
     */
    LearnedPeer(@NotNull NodeEndpoint endpoint, long firstSeenMillis, long lastSuccessMillis, int failedStarts) {
        this.endpoint          = endpoint;
        this.firstSeenMillis   = firstSeenMillis;
        this.lastSuccessMillis = lastSuccessMillis;
        this.failedStarts      = failedStarts;
    }

    /**
     * @return the peer's key and address
     */
    public @NotNull NodeEndpoint getEndpoint() {
        return this.endpoint;
    }

    /**
     * @return the peer's key
     */
    public @NotNull IdentityKey getKey() {
        return this.endpoint.getKey();
    }

    /**
     * @return the peer's address
     */
    public @NotNull InetSocketAddress getAddress() {
        return this.endpoint.getAddress();
    }

    /**
     * @return the time the peer was first seen, in epoch milliseconds
     */
    public long getFirstSeenMillis() {
        return this.firstSeenMillis;
    }

    /**
     * @return the time the peer last answered, in epoch milliseconds, or {@link #NEVER_ANSWERED}
     */
    public long getLastSuccessMillis() {
        return this.lastSuccessMillis;
    }

    /**
     * @return the starts in a row in which the peer was tried and did not answer
     */
    public int getFailedStarts() {
        return this.failedStarts;
    }

    /**
     * @return the time the peer last answered, or the time it was first seen if it never answered
     */
    long getLastActivityMillis() {
        return Math.max(this.lastSuccessMillis, this.firstSeenMillis);
    }
}
