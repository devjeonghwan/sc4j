package org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.beaconlink.peer.MeshPeer;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Joins a peer to the Beacon.Link network at its start and keeps its learned peers ({@link KnownNodes}): the seeds and the
 * {@value KnownNodes#BOOTSTRAP_PEERS} best learned peers are greeted together, and whichever answers first connects the peer; a seed that is
 * down does not matter once the peers file holds other peers. A seed with a pinned key, and every learned peer, is greeted as a known peer,
 * so that only that key's answer counts.
 * <p>
 * The bootstrap runs on the peer's thread: {@link #start} once (it resolves the seeds' host names), {@link #update} from the peer's loop,
 * {@link #close(long)} when the peer stops; {@link #run} is that loop for a peer on a socket of its own. It records the full peers of the
 * peer's routing table (never leaves) as answering peers, merges them into the peers file every {@value PeerStore#MERGE_INTERVAL_MILLIS} ms
 * and at the close, and tells the user why no peer answers ({@link BootstrapState}).
 */
public final class PeerBootstrap {
    /**
     * The time a start waits for the first answer before it reports that no peer is reachable, in milliseconds.
     */
    public static final long DEFAULT_CONNECT_TIMEOUT_MILLIS = 30_000L;

    private static final @NotNull Logger LOGGER = Logger.of(PeerBootstrap.class);
    private static final long UPDATE_INTERVAL_MILLIS = 1_000L;
    private static final long POLL_MILLIS = 100L;


    private final @NotNull MeshPeer peer;
    private final @NotNull KnownNodes knownNodes;
    private final long connectTimeoutMillis;

    private @NotNull BootstrapState state;
    private int knownCount;
    private long deadlineMillis;
    private long nextUpdateMillis;

    /**
     * Creates a {@link PeerBootstrap} instance; nothing is greeted before {@link #start}.
     *
     * @param peer                 the peer
     * @param knownNodes           the seeds and the learned peers to try and to keep
     * @param connectTimeoutMillis the time a start waits for the first answer, in milliseconds
     */
    public PeerBootstrap(@NotNull MeshPeer peer, @NotNull KnownNodes knownNodes, long connectTimeoutMillis) {
        this.peer                 = peer;
        this.knownNodes           = knownNodes;
        this.connectTimeoutMillis = connectTimeoutMillis;

        this.state            = BootstrapState.NO_PEERS_KNOWN;
        this.knownCount       = 0;
        this.deadlineMillis   = Long.MIN_VALUE;
        this.nextUpdateMillis = Long.MIN_VALUE;
    }

    /**
     * @return how far the peer got in joining the network
     */
    public @NotNull BootstrapState getState() {
        return this.state;
    }

    /**
     * @return the number of seeds and learned peers the start greets
     */
    public int getKnownCount() {
        return this.knownCount;
    }

    /**
     * @return the seeds and the learned peers the bootstrap tries and keeps
     */
    public @NotNull KnownNodes getKnownNodes() {
        return this.knownNodes;
    }


    /**
     * Starts joining: resolves the contacts ({@link KnownNodes#getContacts}; a host name that cannot be resolved is skipped and logged) and
     * adds them to the peer, a seed without a pinned key as a seed, the others as known peers.
     *
     * @param nowMillis the current time
     */
    public void start(long nowMillis) {
        for (ConfiguredNode contact : this.knownNodes.getContacts(KnownNodes.BOOTSTRAP_PEERS, List.of(this.peer.getKey()))) {
            InetSocketAddress address = contact.resolve();
            IdentityKey pinned = contact.getKey();

            if (address == null) {
                LOGGER.warning("The seed {0} cannot be resolved.", contact);
            } else if (pinned != null) {
                this.peer.addKnownNode(new NodeEndpoint(pinned, address));
                this.knownCount++;
            } else {
                this.peer.addSeed(address);
                this.knownCount++;
            }
        }

        this.state          = this.knownCount == 0 ? BootstrapState.NO_PEERS_KNOWN : BootstrapState.CONNECTING;
        this.deadlineMillis = nowMillis + this.connectTimeoutMillis;
        this.knownNodes.flushWhenDue(nowMillis);
        LOGGER.info("Joining the network through {0} seeds and learned peers.", this.knownCount);
    }

    /**
     * Updates the state from the peers proved at their address (at most once a second), records the full peers of the routing table as
     * answering peers, and merges them into the peers file when it is due.
     *
     * @param nowMillis the current time
     */
    public void update(long nowMillis) {
        if (nowMillis < this.nextUpdateMillis) {
            return;
        }

        List<NodeEndpoint> live = this.peer.getLiveNodes();
        BootstrapState previous = this.state;

        this.nextUpdateMillis = nowMillis + UPDATE_INTERVAL_MILLIS;
        this.knownNodes.recordSuccess(this.peer.getKnownNodes(), nowMillis);

        if (!live.isEmpty()) {
            this.state = BootstrapState.CONNECTED;
        } else if (this.knownCount == 0 && previous == BootstrapState.NO_PEERS_KNOWN) {
            this.state = BootstrapState.NO_PEERS_KNOWN;
        } else if (previous == BootstrapState.CONNECTED || nowMillis >= this.deadlineMillis) {
            this.state = BootstrapState.NO_PEER_REACHABLE;
        }

        if (this.state != previous) {
            LOGGER.info("Network state: {0}", this.state);
        }

        this.knownNodes.flushWhenDue(nowMillis);
    }

    /**
     * Runs the bootstrap's peer on its own socket until the socket is stopped: the headless node program and the launcher's peer. Starts
     * joining, then polls the socket, updates the bootstrap and calls the listener after every poll, and closes the bootstrap at the end
     * (the learned peers are merged into the peers file), also when the socket fails.
     *
     * @param socket    the socket that runs the bootstrap's peer
     * @param clock     the protocol clock
     * @param afterPoll called on the peer's thread after every poll
     * @throws IOException              if the socket fails
     * @throws IllegalArgumentException if the socket runs another peer
     */
    public void run(@NotNull PeerSocket socket, @NotNull ProtocolClock clock, @NotNull Runnable afterPoll) throws IOException {
        if (socket.getPeer() != this.peer) {
            throw Exceptions.newIllegalArgumentException("The socket at {0} runs another peer than the bootstrap.", socket.getLocalAddress());
        }

        this.start(clock.getEpochMillis());

        try {
            while (!socket.isStopped()) {
                socket.poll(POLL_MILLIS);
                this.update(clock.getEpochMillis());
                afterPoll.run();
            }
        } finally {
            this.close(clock.getEpochMillis());
        }
    }

    /**
     * Records the last full peers of the routing table and merges the learned peers into the peers file; called when the peer stops.
     *
     * @param nowMillis the current time
     */
    public void close(long nowMillis) {
        this.knownNodes.recordSuccess(this.peer.getKnownNodes(), nowMillis);
        this.knownNodes.flush(nowMillis);
    }
}
