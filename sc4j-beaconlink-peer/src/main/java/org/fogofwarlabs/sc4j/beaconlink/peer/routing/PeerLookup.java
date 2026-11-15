package org.fogofwarlabs.sc4j.beaconlink.peer.routing;

import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.PeerContact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Runs one iterative Kademlia lookup of the full peers closest to a target over one or more disjoint paths (S/Kademlia).
 * <p>
 * The start peers, closest first and under the network limit below, are dealt to the paths in turn. Each path asks up to the parallelism of its
 * unasked peers among its closest ones that did not fail (a peer that does not answer in time is asked once more, so that one lost datagram does not
 * lose a peer); the peers an answer names join the path that asked, unless another path knows them already, so that no peer is on two paths and one
 * bad region of the network cannot steer every path. Among the closest peers a path considers, one IPv4 /16 (IPv6 /32) network counts only a few
 * times ({@link PeerConfiguration#getMaximumPerWideNetworkInLookup()}), so that many IDs of one operator that name only each other cannot fill a path
 * and end it before it reaches the target. A path ends when its considered peers have all answered or failed and no request is outstanding; the
 * lookup ends when every path ended or its time is up. The result is the union of the peers that answered on any path, closest first, under the same
 * network limit.
 * <p>
 * The lookup sends nothing itself: {@link PeerRouting} sends its requests, checks the named peers and hands it the answers and failures.
 */
final class PeerLookup {
    /**
     * Sends a lookup request to a peer.
     */
    interface RequestSender {
        /**
         * Sends a request for the peers closest to the target.
         *
         * @param query the query to send
         * @return the request number of the sent request
         */
        long send(@NotNull Query query);
    }

    /**
     * Lists the states of one peer in a lookup.
     */
    enum QueryState {
        UNASKED,
        PENDING,
        ANSWERED,
        FAILED
    }

    /**
     * Holds one peer of a lookup: its contact, its path and its state.
     */
    static final class Query {
        private final @NotNull PeerLookup lookup;
        private final @NotNull PeerContact contact;
        private final int path;

        private @NotNull QueryState state;
        private long request;
        private long sentMillis;
        private boolean resent;

        private Query(@NotNull PeerLookup lookup, @NotNull PeerContact contact, int path) {
            this.lookup  = lookup;
            this.contact = contact;
            this.path    = path;

            this.state      = QueryState.UNASKED;
            this.request    = RoutingContact.NO_REQUEST;
            this.sentMillis = 0L;
            this.resent     = false;
        }

        /**
         * @return the lookup the query belongs to
         */
        @NotNull PeerLookup getLookup() {
            return this.lookup;
        }

        /**
         * @return the asked peer
         */
        @NotNull PeerContact getContact() {
            return this.contact;
        }

        /**
         * @return the request number of the last sent request, or {@link RoutingContact#NO_REQUEST} before the first was sent
         */
        long getRequest() {
            return this.request;
        }

        /**
         * @return the time the last request was sent
         */
        long getSentMillis() {
            return this.sentMillis;
        }

        /**
         * @return the target of the lookup
         */
        @NotNull PeerId getTarget() {
            return this.lookup.target;
        }
    }


    private final @NotNull PeerId target;
    private final int resultSize;
    private final int parallelism;
    private final int perNetwork;
    private final long requestTimeoutMillis;
    private final long deadlineMillis;
    private final @Nullable Consumer<@NotNull List<@NotNull PeerContact>> listener;

    private final @NotNull Map<@NotNull IdentityKey, @NotNull Query> queries;
    private final @NotNull List<@NotNull List<@NotNull Query>> paths;
    private boolean finished;

    /**
     * Creates a {@link PeerLookup} instance; {@link #advance} sends its first requests.
     *
     * @param target         the target ID
     * @param start          the start peers, closest first
     * @param configuration  the result size (the bucket size), the parallelism, the network limit and the request timeout
     * @param pathCount      the number of disjoint paths
     * @param deadlineMillis the time the lookup ends at the latest
     * @param listener       the receiver of the result, or {@code null}
     */
    PeerLookup(@NotNull PeerId target, @NotNull List<@NotNull PeerContact> start, @NotNull PeerConfiguration configuration, int pathCount,
               long deadlineMillis, @Nullable Consumer<@NotNull List<@NotNull PeerContact>> listener) {
        int disjointPathCount = Math.max(1, Math.min(pathCount, start.size()));

        this.target               = target;
        this.resultSize           = configuration.getBucketSize();
        this.parallelism          = configuration.getLookupParallelism();
        this.perNetwork           = configuration.getMaximumPerWideNetworkInLookup();
        this.requestTimeoutMillis = configuration.getLookupRequestTimeoutMillis();
        this.deadlineMillis       = deadlineMillis;
        this.listener             = listener;

        this.queries  = new HashMap<>();
        this.paths    = new ArrayList<>(disjointPathCount);
        this.finished = false;

        for (int path = 0; path < disjointPathCount; path++) {
            this.paths.add(new ArrayList<>());
        }

        Map<Long, Integer> startNetworks = new HashMap<>();
        int dealt = 0;

        for (PeerContact contact : start) {
            if (this.countNetwork(startNetworks, contact)) {
                this.addQuery(contact, dealt % disjointPathCount);
                dealt++;
            }
        }
    }

    /**
     * @return {@code true} if the lookup ended and handed its result to its listener, {@code false} otherwise (it runs)
     */
    boolean isFinished() {
        return this.finished;
    }

    /**
     * @return the peers that answered on any path, closest first, at most the result size
     */
    @NotNull List<@NotNull PeerContact> getResult() {
        List<Query> answered = new ArrayList<>();

        for (Query query : this.queries.values()) {
            if (query.state == QueryState.ANSWERED) {
                answered.add(query);
            }
        }

        answered.sort((first, second) -> this.target.compareDistance(first.contact.getId(), second.contact.getId()));

        List<PeerContact> result = new ArrayList<>(Math.min(answered.size(), this.resultSize));
        Map<Long, Integer> networks = new HashMap<>();

        for (Query query : answered) {
            if (result.size() >= this.resultSize) {
                break;
            }

            if (this.countNetwork(networks, query.contact)) {
                result.add(query.contact);
            }
        }

        return result;
    }


    /**
     * Records a peer's answer: the checked peers it named join its path unless the lookup knows them already.
     *
     * @param query the answered query
     * @param named the peers the answer named, checked by the caller
     */
    void handleAnswer(@NotNull Query query, @NotNull List<@NotNull PeerContact> named) {
        if (query.state != QueryState.PENDING) {
            return;
        }

        query.state = QueryState.ANSWERED;

        for (PeerContact contact : named) {
            if (!this.queries.containsKey(contact.getKey())) {
                this.addQuery(contact, query.path);
            }
        }
    }

    /**
     * Gives up the outstanding requests past their time, sends the next requests of every path and ends the lookup when every path ended or
     * its time is up.
     *
     * @param sender    the sender of the requests
     * @param timedOut  the receiver of the queries that timed out
     * @param nowMillis the current time
     */
    void advance(@NotNull RequestSender sender, @NotNull Consumer<@NotNull Query> timedOut, long nowMillis) {
        if (this.finished) {
            return;
        }

        boolean running = false;

        for (List<Query> path : this.paths) {
            running |= this.advancePath(path, sender, timedOut, nowMillis);
        }

        if (!running || nowMillis >= this.deadlineMillis) {
            this.finish(timedOut);
        }
    }

    private boolean advancePath(@NotNull List<@NotNull Query> path, @NotNull RequestSender sender, @NotNull Consumer<@NotNull Query> timedOut,
                                long nowMillis) {
        int pending = 0;

        for (Query query : path) {
            if (query.state == QueryState.PENDING && nowMillis - query.sentMillis >= this.requestTimeoutMillis) {
                if (query.resent) {
                    query.state = QueryState.FAILED;
                    timedOut.accept(query);
                } else {
                    query.resent     = true;
                    query.sentMillis = nowMillis;
                    query.request    = sender.send(query);
                }
            }

            if (query.state == QueryState.PENDING) {
                pending++;
            }
        }

        path.sort((first, second) -> this.target.compareDistance(first.contact.getId(), second.contact.getId()));

        Map<Long, Integer> networks = new HashMap<>();
        int considered = 0;
        boolean unasked = false;

        for (Query query : path) {
            if (considered >= this.resultSize) {
                break;
            }

            if (query.state == QueryState.FAILED || !this.countNetwork(networks, query.contact)) {
                continue;
            }

            considered++;

            if (query.state == QueryState.UNASKED) {
                if (pending < this.parallelism) {
                    query.state      = QueryState.PENDING;
                    query.sentMillis = nowMillis;
                    query.request    = sender.send(query);
                    pending++;
                } else {
                    unasked = true;
                }
            }
        }

        return pending > 0 || unasked;
    }

    private void finish(@NotNull Consumer<@NotNull Query> timedOut) {
        this.finished = true;

        for (Query query : this.queries.values()) {
            if (query.state == QueryState.PENDING) {
                query.state = QueryState.FAILED;
                timedOut.accept(query);
            }
        }

        if (this.listener != null) {
            this.listener.accept(this.getResult());
        }
    }

    /**
     * Counts a peer's /16 network among the considered peers.
     *
     * @param networks the considered peers per network
     * @param contact  the peer
     * @return {@code true} if the network is still below its limit (or the peer is on the loopback address), {@code false} otherwise
     */
    private boolean countNetwork(@NotNull Map<@NotNull Long, @NotNull Integer> networks, @NotNull PeerContact contact) {
        Long network = RoutingTable.findNetwork(contact.getAddress().getAddress(), true);

        if (network == null) {
            return true;
        }

        int count = networks.getOrDefault(network, 0);

        if (count >= this.perNetwork) {
            return false;
        }

        networks.put(network, count + 1);

        return true;
    }

    private void addQuery(@NotNull PeerContact contact, int path) {
        Query query = new Query(this, contact, path);

        this.queries.put(contact.getKey(), query);
        this.paths.get(path).add(query);
    }
}
