package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.CoordinateReport;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerStamp;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.*;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkNode;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkNodeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds a client's session with one node: the registration with its address token exchange and its refresh after half the granted lifetime,
 * the observed address, the node's key (pinned, or taken from its first answer), the node list requests, and the retry of a node that did not
 * answer or refused.
 * <p>
 * A refresh from the registered address needs no token; when the node answers {@code TOKEN_REQUIRED} (its registration is gone, or the
 * client's address changed behind a NAT), the session repeats the registration with the token.
 * <p>
 * The registration names the session's connection and its start, so that the node holds it beside the other connections of the account; a
 * node that holds the most connections of the account refuses the session, which tries again after the retry time (another connection's
 * registration may have ended by then). A node that holds a sign-out of the account that ends this connection refuses it for good: the session
 * is signed out ({@link BeaconLinkClient#signOut}).
 * <p>
 * A node of the install's list hands the client the nodes it reports; a node the client learned earlier does not, so that one node list
 * cannot lead to nodes no listed node vouched for. Every node is asked once after its first registration; a node of the install's list is
 * asked again while the client knows few nodes ({@link BeaconLinkClient#needsMoreNodes}), {@value #NODE_LIST_INTERVAL_MILLIS} ms after its
 * last answer, the interval doubling every time, at most {@value #MAXIMUM_NODE_LISTS} times in a session, so that a session started when
 * its nodes knew few others learns the ones that join later.
 */
final class NodeSession implements LinkNode, RequestOwner {
    private static final @NotNull Logger LOGGER = Logger.of(NodeSession.class);
    private static final byte @NotNull [] NO_TOKEN = new byte[0];
    private static final long NO_REQUEST = -1L;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long REFRESH_DIVISOR = 2L;
    /**
     * The time from the first node list to the second, in milliseconds; it doubles for every later one.
     */
    private static final long NODE_LIST_INTERVAL_MILLIS = 30_000L;
    /**
     * The most node list requests to one node in a session: the last one goes out about an hour after the first.
     */
    private static final int MAXIMUM_NODE_LISTS = 8;


    private final @NotNull BeaconLinkClient client;
    private final @NotNull ConfiguredNode configured;
    private final @Nullable InetSocketAddress address;
    private final boolean learned;

    private @Nullable IdentityKey key;
    private @NotNull LinkNodeState state;
    private @Nullable InetSocketAddress observedAddress;
    private @Nullable CoordinateReport coordinate;
    private int registrationCount;
    private long refreshMillis;
    private long nextAttemptMillis;

    private long registerRequest;
    private long nodeListRequest;
    private int nodeListCount;
    private long nodeListMillis;

    /**
     * Creates a {@link NodeSession} instance that registers with the next maintenance.
     *
     * @param client     the client
     * @param configured the node as listed, or as learned with its key
     * @param address    the node's resolved address, or {@code null} if its host cannot be resolved
     * @param learned    whether the node was learned from a node list instead of listed by the install
     * @param nowMillis  the protocol clock time
     */
    NodeSession(@NotNull BeaconLinkClient client, @NotNull ConfiguredNode configured, @Nullable InetSocketAddress address, boolean learned,
                long nowMillis) {
        this.client     = client;
        this.configured = configured;
        this.address    = address;
        this.learned    = learned;

        this.key               = configured.getKey();
        this.state             = address == null ? LinkNodeState.UNRESOLVED : LinkNodeState.CONNECTING;
        this.observedAddress   = null;
        this.coordinate        = null;
        this.registrationCount = 0;
        this.refreshMillis     = nowMillis;
        this.nextAttemptMillis = nowMillis;

        this.registerRequest = NO_REQUEST;
        this.nodeListRequest = NO_REQUEST;
        this.nodeListCount   = 0;
        this.nodeListMillis  = nowMillis;
    }

    /**
     * @return the node's address as listed, {@code host:port} with an IPv6 host in brackets
     */
    @Override
    public @NotNull String getAddress() {
        return this.configured.toString();
    }

    /**
     * @return how the session stands with the node
     */
    @Override
    public @NotNull LinkNodeState getState() {
        return this.state;
    }

    /**
     * @return the source address the node last observed for the session, or {@code null} if it never answered
     */
    @Override
    public @Nullable InetSocketAddress getObservedAddress() {
        return this.observedAddress;
    }

    /**
     * @return the node's network coordinate and its error from its last answer that carried them, or {@code null} before one came
     */
    @Nullable CoordinateReport getCoordinate() {
        return this.coordinate;
    }

    /**
     * Sets the node's network coordinate and its error from an answer that carried them.
     *
     * @param coordinate the node's report
     */
    void setCoordinate(@NotNull CoordinateReport coordinate) {
        this.coordinate = coordinate;
    }

    /**
     * @return the node's resolved address, or {@code null} if its host cannot be resolved
     */
    @Nullable InetSocketAddress getSocketAddress() {
        return this.address;
    }

    /**
     * @return the node's key: pinned by the list, or taken from its first answer; {@code null} before it answered
     */
    @Nullable IdentityKey getKey() {
        return this.key;
    }

    /**
     * @return the node's key and resolved address, or {@code null} before the node answered or if its host cannot be resolved
     */
    @Nullable NodeEndpoint getEndpoint() {
        IdentityKey known = this.key;
        InetSocketAddress resolved = this.address;

        return known == null || resolved == null ? null : new NodeEndpoint(known, resolved);
    }

    /**
     * @return {@code true} if the node was learned from a node list, {@code false} otherwise (the install lists it)
     */
    boolean isLearned() {
        return this.learned;
    }

    /**
     * @return how often the node registered the session; a new registration means the node may have forgotten what the session published
     */
    int getRegistrationCount() {
        return this.registrationCount;
    }

    /**
     * @return the number of node list requests to the node that ended (answered or not) in this session
     */
    int getNodeListCount() {
        return this.nodeListCount;
    }

    /**
     * @return {@code true} if the node holds a registration of the session, {@code false} otherwise
     */
    boolean isRegistered() {
        return this.state == LinkNodeState.REGISTERED;
    }

    /**
     * @return {@code true} if the session has an outcome with the node (registered or failed), {@code false} otherwise (while it is connecting)
     */
    boolean isSettled() {
        return this.state != LinkNodeState.CONNECTING;
    }

    /**
     * @return {@code true} if the node refused the session or answered with the wrong key, {@code false} otherwise
     */
    boolean isRefused() {
        return this.state == LinkNodeState.REFUSED || this.state == LinkNodeState.TOO_MANY_CONNECTIONS || this.state == LinkNodeState.WRONG_KEY;
    }


    /**
     * Checks the key of a valid reply from the node's address; without a pinned key, the first answer's key becomes the node's key.
     *
     * @param sender the reply's sender key
     * @return {@code true} if the key is the node's key, {@code false} otherwise
     */
    boolean acceptsKey(@NotNull IdentityKey sender) {
        IdentityKey expected = this.key;

        if (expected == null) {
            this.key = sender;

            return true;
        }

        return expected.equals(sender);
    }

    /**
     * Sends the due request: the first registration, a retry, a refresh, or a due node list request while the node registers the session.
     *
     * @param nowMillis the protocol clock time
     */
    void maintain(long nowMillis) {
        if (this.address == null || this.registerRequest != NO_REQUEST || this.state == LinkNodeState.SIGNED_OUT) {
            return;
        }

        if (this.state != LinkNodeState.REGISTERED) {
            if (nowMillis >= this.nextAttemptMillis) {
                this.sendRegister(NO_TOKEN);
            }
        } else if (nowMillis >= this.refreshMillis) {
            this.sendRegister(NO_TOKEN);
        } else if (this.nodeListRequest == NO_REQUEST &&
                   (this.nodeListCount == 0 || !this.learned && this.nodeListCount < MAXIMUM_NODE_LISTS && nowMillis >= this.nodeListMillis &&
                                               this.client.needsMoreNodes())) {
            this.nodeListRequest = this.client.sendRequest(this, new FindPeersMessage(this.client.createRandomTarget(), null), true, this);
        }
    }

    /**
     * Removes the registration at the node without waiting for the answer.
     */
    void unregister() {
        if (this.state == LinkNodeState.REGISTERED) {
            this.client.sendOnce(this, this.createRegister(NO_TOKEN, 0), true);
        }
    }

    /**
     * Records that the session was signed out: it never registers at the node again.
     */
    void markSignedOut() {
        this.state = LinkNodeState.SIGNED_OUT;
    }

    /**
     * Records that the node did not answer a request: the session tries again after the retry time.
     *
     * @param nowMillis the protocol clock time
     */
    void markNotAnswering(long nowMillis) {
        if (this.state != LinkNodeState.NOT_ANSWERING) {
            LOGGER.info("BEACON.LINK: node {0} does not answer", this.configured);
        }

        this.state             = LinkNodeState.NOT_ANSWERING;
        this.nextAttemptMillis = nowMillis + this.client.getConfiguration().getRetryMillis();
    }

    /**
     * Records that the node answered with another key than its pinned or first key: the session tries again after the retry time.
     *
     * @param sender    the key the node answered with
     * @param nowMillis the protocol clock time
     */
    void markWrongKey(@NotNull IdentityKey sender, long nowMillis) {
        if (this.state != LinkNodeState.WRONG_KEY) {
            LOGGER.warning("BEACON.LINK: node {0} answered with the key {1}, not {2}", this.configured, sender, this.key);
        }

        this.state             = LinkNodeState.WRONG_KEY;
        this.nextAttemptMillis = nowMillis + this.client.getConfiguration().getRetryMillis();
    }

    /**
     * Records that the node no longer holds the registration (it answered {@code NOT_REGISTERED}): the session registers again at once.
     *
     * @param nowMillis the protocol clock time
     */
    void markRegistrationLost(long nowMillis) {
        if (this.state == LinkNodeState.REGISTERED) {
            LOGGER.info("BEACON.LINK: node {0} lost the registration", this.configured);
            this.state             = LinkNodeState.CONNECTING;
            this.nextAttemptMillis = nowMillis;
        }
    }

    /**
     * Called when a node answered a request: the reply came from the node's address, with the node's key, inside the replay window and with
     * a valid signature.
     *
     * @param node      the node
     * @param reply     the reply; its request number is the request's
     * @param nowMillis the protocol clock time
     */
    @Override
    public void onReply(@NotNull NodeSession node, @NotNull Envelope reply, long nowMillis) {
        if (reply.getRequestNumber() == this.nodeListRequest) {
            this.finishNodeList(nowMillis);

            if (reply.getBody() instanceof PeersMessage peers) {
                List<NodeEndpoint> endpoints = new ArrayList<>(peers.getPeers().size());

                for (PeerContact peer : peers.getPeers()) {
                    endpoints.add(peer.getEndpoint());
                }

                this.client.learnNodes(this, endpoints);
            }

            return;
        }

        if (reply.getRequestNumber() != this.registerRequest) {
            return;
        }

        this.registerRequest = NO_REQUEST;

        if (!(reply.getBody() instanceof ObservedAddressMessage observed)) {
            return;
        }

        this.observedAddress = observed.getAddress();

        switch (observed.getStatus()) {
            case TOKEN_REQUIRED -> this.sendRegister(observed.getToken());
            case REGISTERED -> {
                if (this.state != LinkNodeState.REGISTERED) {
                    this.registrationCount++;
                    LOGGER.info("BEACON.LINK: registered at node {0} ({1}), observed address {2}", this.configured, this.key,
                                observed.getAddress());
                    this.client.onNodeRegistered(this);
                }

                this.state         = LinkNodeState.REGISTERED;
                this.refreshMillis = nowMillis + observed.getLifetimeSeconds() * MILLIS_PER_SECOND / REFRESH_DIVISOR;
            }
            case TOO_MANY_CONNECTIONS -> {
                if (this.state != LinkNodeState.TOO_MANY_CONNECTIONS) {
                    LOGGER.info("BEACON.LINK: node {0} holds the most connections of the account", this.configured);
                }

                this.state             = LinkNodeState.TOO_MANY_CONNECTIONS;
                this.nextAttemptMillis = nowMillis + this.client.getConfiguration().getRetryMillis();
            }
            case SIGNED_OUT -> {
                LOGGER.info("BEACON.LINK: node {0} holds a sign-out of the account that ends the session", this.configured);
                this.client.signOut();
                this.state = LinkNodeState.SIGNED_OUT;
            }
            case INVALID_NAME -> {
                LOGGER.warning("BEACON.LINK: node {0} refused the player name {1}", this.configured, this.client.getDisplayName());
                this.state             = LinkNodeState.REFUSED;
                this.nextAttemptMillis = nowMillis + this.client.getConfiguration().getRetryMillis();
            }
            case FULL -> {
                LOGGER.info("BEACON.LINK: node {0} is full", this.configured);
                this.state             = LinkNodeState.REFUSED;
                this.nextAttemptMillis = nowMillis + this.client.getConfiguration().getRetryMillis();
            }
            case UNREGISTERED -> {
                // Not an answer to a registration with a lifetime; the session keeps its state.
            }
        }
    }

    /**
     * Called when a node did not answer a request after every attempt, or answered with another key than the expected one.
     *
     * @param node          the node
     * @param requestNumber the request number
     * @param nowMillis     the protocol clock time
     */
    @Override
    public void onNoAnswer(@NotNull NodeSession node, long requestNumber, long nowMillis) {
        if (requestNumber == this.nodeListRequest) {
            this.finishNodeList(nowMillis);
        } else if (requestNumber == this.registerRequest) {
            this.registerRequest = NO_REQUEST;
        }
    }

    private void finishNodeList(long nowMillis) {
        this.nodeListRequest = NO_REQUEST;
        this.nodeListMillis  = nowMillis + (NODE_LIST_INTERVAL_MILLIS << this.nodeListCount);
        this.nodeListCount++;
    }

    private void sendRegister(byte @NotNull [] token) {
        this.registerRequest = this.client.sendRequest(this, this.createRegister(token, this.client.getConfiguration().getRegistrationSeconds()),
                                                       true, this);
    }

    private @NotNull RegisterMessage createRegister(byte @NotNull [] token, int lifetimeSeconds) {
        PlayerStamp stamp = this.client.getStamp();

        return new RegisterMessage(token, lifetimeSeconds, stamp.getName(), stamp.getNonce(), this.client.getFingerprint(),
                                   this.client.getConnection(), this.client.getConnectionStartMillis());
    }

    /**
     * @return the node's address and state, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.configured + " " + this.state;
    }
}
