package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.beaconlink.protocol.community.ChatLineRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.community.PresenceRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * Holds a client's part in one community topic: the presences (the newest of every connection of a player, at most
 * {@value #MAXIMUM_CONNECTIONS} per player) and chat lines the client got for it, its own presence there, and the state of its subscription,
 * of its presence and of its sign-out at each home node. {@link ClientCommunity} owns it.
 */
final class CommunityTopic {
    /**
     * The most chat lines a client keeps of one channel.
     */
    static final int MAXIMUM_LINES = 64;
    /**
     * The request number of no request.
     */
    static final long NO_REQUEST = -1L;
    /**
     * The sequence of a presence no node took yet.
     */
    static final long NOT_PUBLISHED = -1L;
    /**
     * The most chat lines a client holds for one node that lost its registration.
     */
    static final int MAXIMUM_HELD_LINES = 8;
    /**
     * The most connections of one player whose presences the client keeps in a topic, as many as a node registers of one key.
     */
    static final int MAXIMUM_CONNECTIONS = 8;

    private static final @NotNull Comparator<@NotNull ChatLineRecord> LINE_ORDER = Comparator.comparingLong(ChatLineRecord::getTimestampMillis);

    /**
     * Lists why a client takes part in a topic, which decides whether it publishes its presence there.
     */
    enum Role {
        /**
         * The topic of the run's mod: presence, for the channel list and the players of the mod.
         */
        MOD,
        /**
         * A joined chat channel: presence and lines.
         */
        CHANNEL,
        /**
         * The client's own player topic: presence, for its friends.
         */
        OWN_PLAYER,
        /**
         * A friend's player topic: the client only follows the friend's presence.
         */
        FRIEND
    }

    /**
     * Holds the topic's state at one home node: the subscription, the client's presence and sign-out there and the client's lines the node
     * refused because it had lost the client's registration.
     */
    static final class NodeState {
        private long subscribeRequest;
        private long renewMillis;
        private int subscribedRegistration;

        private long presenceRequest;
        private long presenceSequence;
        private int presenceRegistration;

        private long signOutRequest;
        private long signOutSequence;

        private final @NotNull List<@NotNull ChatLineRecord> heldLines;
        private int heldRegistration;

        private NodeState() {
            this.subscribeRequest       = NO_REQUEST;
            this.renewMillis            = Long.MIN_VALUE;
            this.subscribedRegistration = -1;

            this.presenceRequest      = NO_REQUEST;
            this.presenceSequence     = NOT_PUBLISHED;
            this.presenceRegistration = -1;

            this.signOutRequest  = NO_REQUEST;
            this.signOutSequence = NOT_PUBLISHED;

            this.heldLines        = new ArrayList<>();
            this.heldRegistration = -1;
        }

        /**
         * Checks whether the subscription at the node needs a request: none was taken since the node registered the client, or its renewal is
         * due, and no request runs.
         *
         * @param registrationCount the node's current registration count of the client
         * @param nowMillis         the protocol clock time
         * @return {@code true} if a subscription request is due, {@code false} otherwise
         */
        boolean isSubscriptionDue(int registrationCount, long nowMillis) {
            return this.subscribeRequest == NO_REQUEST && (this.subscribedRegistration != registrationCount || nowMillis >= this.renewMillis);
        }

        /**
         * Checks whether the client's next subscription asks for the backlog: the node took none since it registered the client.
         *
         * @param registrationCount the node's current registration count of the client
         * @return {@code true} if the next subscription asks for the backlog, {@code false} otherwise
         */
        boolean needsBacklog(int registrationCount) {
            return this.subscribedRegistration != registrationCount;
        }

        /**
         * Checks whether the client's presence must go to the node: the node holds an older one, or none since it registered the client.
         *
         * @param sequence          the sequence of the client's current presence
         * @param registrationCount the node's current registration count of the client
         * @return {@code true} if a presence request is due, {@code false} otherwise
         */
        boolean isPresenceDue(long sequence, int registrationCount) {
            return this.presenceRequest == NO_REQUEST && (this.presenceSequence != sequence || this.presenceRegistration != registrationCount);
        }

        /**
         * Records a sent subscription request.
         *
         * @param request the request number
         */
        void startSubscription(long request) {
            this.subscribeRequest = request;
        }

        /**
         * Records the outcome of a subscription request.
         *
         * @param registrationCount the node's registration count of the client when it answered, or {@code -1} if it refused
         * @param renewMillis       the protocol clock time of the next renewal
         */
        void finishSubscription(int registrationCount, long renewMillis) {
            this.subscribeRequest       = NO_REQUEST;
            this.subscribedRegistration = registrationCount;
            this.renewMillis            = renewMillis;
        }

        /**
         * @return the number of the running subscription request, or {@link #NO_REQUEST}
         */
        long getSubscribeRequest() {
            return this.subscribeRequest;
        }

        /**
         * Records a sent presence request.
         *
         * @param request the request number
         */
        void startPresence(long request) {
            this.presenceRequest = request;
        }

        /**
         * Records the outcome of a presence request.
         *
         * @param sequence          the sequence the node took, or {@link #NOT_PUBLISHED}
         * @param registrationCount the node's registration count of the client when it answered
         */
        void finishPresence(long sequence, int registrationCount) {
            this.presenceRequest      = NO_REQUEST;
            this.presenceSequence     = sequence;
            this.presenceRegistration = registrationCount;
        }

        /**
         * @return the number of the running presence request, or {@link #NO_REQUEST}
         */
        long getPresenceRequest() {
            return this.presenceRequest;
        }

        /**
         * Checks whether the client's sign-out must go to the node: the node took none of its sequence yet and no request runs.
         *
         * @param sequence the sequence of the client's sign-out
         * @return {@code true} if a sign-out request is due, {@code false} otherwise
         */
        boolean isSignOutDue(long sequence) {
            return this.signOutRequest == NO_REQUEST && this.signOutSequence != sequence;
        }

        /**
         * Records a sent sign-out request.
         *
         * @param request the request number
         */
        void startSignOut(long request) {
            this.signOutRequest = request;
        }

        /**
         * Records the outcome of a sign-out request.
         *
         * @param sequence the sequence the node took, or {@link #NOT_PUBLISHED}
         */
        void finishSignOut(long sequence) {
            this.signOutRequest  = NO_REQUEST;
            this.signOutSequence = sequence;
        }

        /**
         * @return the number of the running sign-out request, or {@link #NO_REQUEST}
         */
        long getSignOutRequest() {
            return this.signOutRequest;
        }

        /**
         * Holds a line the node refused because it no longer registered the client, until the node registers the client again; the oldest
         * held line gives way at {@value #MAXIMUM_HELD_LINES}.
         *
         * @param line              the refused line
         * @param registrationCount the node's registration count of the client when the line was sent
         */
        void holdLine(@NotNull ChatLineRecord line, int registrationCount) {
            if (this.heldLines.isEmpty()) {
                this.heldRegistration = registrationCount;
            } else if (this.heldLines.size() >= MAXIMUM_HELD_LINES) {
                this.heldLines.removeFirst();
            }

            this.heldLines.add(line);
        }

        /**
         * Takes the held lines once the node registered the client again since they were sent.
         *
         * @param registrationCount the node's current registration count of the client
         * @return the held lines in sending order, empty if none are held or the node has not registered the client again
         */
        @NotNull List<@NotNull ChatLineRecord> takeHeldLines(int registrationCount) {
            if (this.heldLines.isEmpty() || registrationCount == this.heldRegistration) {
                return List.of();
            }

            List<ChatLineRecord> taken = List.copyOf(this.heldLines);

            this.heldLines.clear();

            return taken;
        }
    }


    private final @NotNull TopicKey topic;
    private final @NotNull Role role;
    private final @Nullable String channelName;

    private final @NotNull Map<@NotNull IdentityKey, @NotNull Map<@NotNull Long, @NotNull PresenceRecord>> presences;
    private final @NotNull List<@NotNull ChatLineRecord> lines;
    private final @NotNull Map<@NotNull NodeSession, @NotNull NodeState> nodes;
    private @Nullable PresenceRecord ownPresence;

    /**
     * Creates a {@link CommunityTopic} instance without records.
     *
     * @param topic       the topic
     * @param role        why the client takes part in the topic
     * @param channelName the channel name as the client joined it, or {@code null} for a topic that is no channel
     */
    CommunityTopic(@NotNull TopicKey topic, @NotNull Role role, @Nullable String channelName) {
        this.topic       = topic;
        this.role        = role;
        this.channelName = channelName;

        this.presences   = new LinkedHashMap<>();
        this.lines       = new ArrayList<>();
        this.nodes       = new LinkedHashMap<>();
        this.ownPresence = null;
    }

    /**
     * @return the topic
     */
    @NotNull TopicKey getTopic() {
        return this.topic;
    }

    /**
     * @return why the client takes part in the topic
     */
    @NotNull Role getRole() {
        return this.role;
    }

    /**
     * @return the channel name as the client joined it, or {@code null} for a topic that is no channel
     */
    @Nullable String getChannelName() {
        return this.channelName;
    }

    /**
     * @return {@code true} if the client publishes its presence in the topic, {@code false} otherwise (it only follows it)
     */
    boolean isPublishingPresence() {
        return this.role != Role.FRIEND;
    }

    /**
     * @return the client's current presence in the topic, or {@code null} if it publishes none
     */
    @Nullable PresenceRecord getOwnPresence() {
        return this.ownPresence;
    }

    /**
     * Sets the client's current presence in the topic; it is also the entry of the client's connection among the topic's presences.
     *
     * @param ownPresence the presence, or {@code null} for none
     */
    void setOwnPresence(@Nullable PresenceRecord ownPresence) {
        this.ownPresence = ownPresence;

        if (ownPresence != null) {
            this.offerPresence(ownPresence);
        }
    }

    /**
     * Lists the unexpired presences of the topic, one per connection of a player.
     *
     * @param nowMillis the protocol clock time
     * @return the presences
     */
    @NotNull List<@NotNull PresenceRecord> getPresences(long nowMillis) {
        List<PresenceRecord> listed = new ArrayList<>();

        for (Map<Long, PresenceRecord> connections : this.presences.values()) {
            for (PresenceRecord presence : connections.values()) {
                if (presence.getExpiryMillis() > nowMillis) {
                    listed.add(presence);
                }
            }
        }

        return listed;
    }

    /**
     * Lists the unexpired presences of a player, one per connection.
     *
     * @param player    the player key
     * @param nowMillis the protocol clock time
     * @return the presences, empty if the topic holds none of the player
     */
    @NotNull List<@NotNull PresenceRecord> findPresences(@NotNull IdentityKey player, long nowMillis) {
        List<PresenceRecord> listed = new ArrayList<>();

        for (PresenceRecord presence : this.presences.getOrDefault(player, Map.of()).values()) {
            if (presence.getExpiryMillis() > nowMillis) {
                listed.add(presence);
            }
        }

        return listed;
    }

    /**
     * @return the chat lines, oldest first by timestamp, at most {@value #MAXIMUM_LINES}
     */
    @NotNull List<@NotNull ChatLineRecord> getLines() {
        return List.copyOf(this.lines);
    }

    /**
     * Returns the state of the topic at a home node, created at the first use.
     *
     * @param node the home node
     * @return the state
     */
    @NotNull NodeState getNodeState(@NotNull NodeSession node) {
        return this.nodes.computeIfAbsent(node, ignored -> new NodeState());
    }


    /**
     * Takes a presence: it replaces an older presence of the same connection of the player. A presence of a further connection of a player
     * with {@value #MAXIMUM_CONNECTIONS} connections takes the place of the one that expires first, if that one expires before it.
     *
     * @param presence the checked presence
     * @return {@code true} if the presence is new, {@code false} otherwise (the topic holds the same or a newer one, or no place for it)
     */
    boolean offerPresence(@NotNull PresenceRecord presence) {
        Map<Long, PresenceRecord> connections = this.presences.computeIfAbsent(presence.getAuthor(), ignored -> new LinkedHashMap<>());
        PresenceRecord stored = connections.get(presence.getConnection());

        if (stored != null && stored.getSequence() >= presence.getSequence()) {
            return false;
        }

        if (stored == null && connections.size() >= MAXIMUM_CONNECTIONS) {
            PresenceRecord first = null;

            for (PresenceRecord other : connections.values()) {
                if (first == null || other.getExpiryMillis() < first.getExpiryMillis()) {
                    first = other;
                }
            }

            if (first == null || first.getExpiryMillis() >= presence.getExpiryMillis()) {
                return false;
            }

            connections.remove(first.getConnection());
        }

        connections.put(presence.getConnection(), presence);

        return true;
    }

    /**
     * Takes a chat line in timestamp order; the oldest line gives way when the topic holds {@value #MAXIMUM_LINES}.
     *
     * @param line the checked line
     * @return {@code true} if the line is kept, {@code false} otherwise (it is older than every kept line of a full list)
     */
    boolean offerLine(@NotNull ChatLineRecord line) {
        if (this.lines.size() >= MAXIMUM_LINES) {
            if (LINE_ORDER.compare(line, this.lines.getFirst()) <= 0) {
                return false;
            }

            this.lines.removeFirst();
        }

        int index = this.lines.size();

        while (index > 0 && LINE_ORDER.compare(this.lines.get(index - 1), line) > 0) {
            index--;
        }

        this.lines.add(index, line);

        return true;
    }

    /**
     * Removes presences of a player's connections.
     *
     * @param player  the player key
     * @param removed picks the presences to remove
     * @return {@code true} if a presence was removed, {@code false} otherwise
     */
    boolean removePresences(@NotNull IdentityKey player, @NotNull Predicate<@NotNull PresenceRecord> removed) {
        Map<Long, PresenceRecord> connections = this.presences.get(player);

        if (connections == null || !connections.values().removeIf(removed)) {
            return false;
        }

        if (connections.isEmpty()) {
            this.presences.remove(player);
        }

        return true;
    }

    /**
     * Removes the expired presences.
     *
     * @param nowMillis the protocol clock time
     * @return {@code true} if a presence was removed, {@code false} otherwise
     */
    boolean removeExpired(long nowMillis) {
        boolean removed = false;

        for (Map<Long, PresenceRecord> connections : this.presences.values()) {
            removed |= connections.values().removeIf(presence -> presence.getExpiryMillis() <= nowMillis);
        }

        this.presences.values().removeIf(Map::isEmpty);

        return removed;
    }
}
