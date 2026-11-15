package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.channel.ChannelOffer;
import org.fogofwarlabs.sc4j.beaconlink.channel.EphemeralKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.channel.JoinProofUtils;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.*;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomAdvertisement;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomDescription;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Publishes one room of a client at every registered node. The room is signed with a sequence that rises across restarts (at least the epoch second),
 * an expiry one room lifetime ahead and the client's home nodes, and signed again after the refresh time or when the home nodes change; a node that
 * registers the client again (it may have lost its rooms) receives the room again. A withdrawal is a signed advertisement with the withdrawn flag,
 * sent to the nodes that hold the room.
 * <p>
 * The client checks the joins of the room ({@link BeaconLinkClient}); the room hands out the channels of the joined participants once they
 * opened. A room with a password is private: it publishes the public half of a join key pair made for it, and a joiner must prove the
 * password with it ({@link JoinProofUtils}); the password itself never leaves the host.
 */
final class ClientHostedRoom implements HostedRoom, RequestOwner {
    private static final long NO_REQUEST = -1L;
    private static final long NOT_PUBLISHED = -1L;
    private static final long MILLIS_PER_SECOND = 1000L;

    /**
     * Holds the room's state at one node.
     */
    private static final class Publication {
        private long request;
        private long requestSequence;
        private boolean requestWithdraws;

        private long publishedSequence;
        private int registrationCount;
        private boolean refused;
        private boolean withdrawn;

        private Publication() {
            this.request          = NO_REQUEST;
            this.requestSequence  = NOT_PUBLISHED;
            this.requestWithdraws = false;

            this.publishedSequence = NOT_PUBLISHED;
            this.registrationCount = 0;
            this.refused           = false;
            this.withdrawn         = false;
        }

        private boolean isPublished() {
            return this.publishedSequence != NOT_PUBLISHED;
        }
    }

    private static long nextSequence(long sequence, long nowMillis) {
        return Math.min(RoomAdvertisement.MAXIMUM_SEQUENCE, Math.max(sequence + 1L, nowMillis / MILLIS_PER_SECOND));
    }


    private final @NotNull BeaconLinkClient client;
    private final long number;
    private final @NotNull Map<@NotNull NodeSession, @NotNull Publication> publications;
    private final @NotNull Deque<@NotNull PeerChannel> joinedChannels;
    private final @NotNull String password;
    private final @Nullable EphemeralKeyPair joinKeyPair;

    private @NotNull RoomSettings settings;
    private @NotNull RoomDescription description;
    private long sequence;
    private @NotNull List<@NotNull NodeEndpoint> homePeers;
    private @NotNull RoomAdvertisement advertisement;
    private @Nullable RoomAdvertisement withdrawal;
    private long refreshMillis;

    /**
     * Creates a {@link ClientHostedRoom} instance and signs its first advertisement; the next maintenance sends it.
     *
     * @param client    the client
     * @param number    the room number, an unsigned 32-bit value
     * @param settings  the room's settings
     * @param password  the password a joiner must prove, empty for a public room
     * @param joinKey   the room's join key pair when it has a password, otherwise {@code null}
     * @param nowMillis the protocol clock time
     * @throws IllegalArgumentException if a name is longer than the room advertisement allows
     */
    ClientHostedRoom(@NotNull BeaconLinkClient client, long number, @NotNull RoomSettings settings, @NotNull String password,
                     @Nullable EphemeralKeyPair joinKey, long nowMillis) {
        this.client         = client;
        this.number         = number;
        this.publications   = new LinkedHashMap<>();
        this.joinedChannels = new ArrayDeque<>();
        this.password       = password;
        this.joinKeyPair    = joinKey;

        this.settings      = settings;
        this.description   = client.createDescription(settings, this.getJoinKey());
        this.sequence      = nextSequence(0L, nowMillis);
        this.homePeers     = client.getHomeEndpoints();
        this.advertisement = this.signPublication(nowMillis);
        this.withdrawal    = null;
        this.refreshMillis = nowMillis + client.getConfiguration().getRoomRefreshMillis();
    }

    /**
     * @return the room number, an unsigned 32-bit value
     */
    long getNumber() {
        return this.number;
    }

    /**
     * @return the public half of the private room's join key pair, or {@code null} for a public room
     */
    private byte @Nullable [] getJoinKey() {
        EphemeralKeyPair keyPair = this.joinKeyPair;

        return keyPair == null ? null : keyPair.getPublicKey();
    }

    /**
     * Checks the password proof of a join offer.
     *
     * @param joiner the joiner's key
     * @param offer  the joiner's offer
     * @return {@code true} if the room is public or the offer proves its password, {@code false} otherwise
     */
    boolean acceptsPassword(@NotNull IdentityKey joiner, @NotNull ChannelOffer offer) {
        EphemeralKeyPair keyPair = this.joinKeyPair;

        return keyPair == null || JoinProofUtils.verify(keyPair, offer.getEphemeralKey(), this.client.getKey(), this.number, joiner, this.password,
                                                        offer.getProof());
    }

    /**
     * @return {@code true} if the room was withdrawn, {@code false} otherwise (while it is published)
     */
    boolean isWithdrawn() {
        return this.withdrawal != null;
    }

    /**
     * @return the room's current settings
     */
    @Override
    public @NotNull RoomSettings getSettings() {
        return this.settings;
    }

    /**
     * @return the publication state
     */
    @Override
    public @NotNull HostedRoomState getState() {
        if (this.withdrawal != null) {
            return HostedRoomState.WITHDRAWN;
        }

        if (this.isPublished()) {
            return HostedRoomState.PUBLISHED;
        }

        return this.getFailure() != null ? HostedRoomState.FAILED : HostedRoomState.PUBLISHING;
    }

    /**
     * @return why the room is not published, or {@code null} unless the state is {@link HostedRoomState#FAILED}
     */
    @Override
    public @Nullable LinkFailure getFailure() {
        if (this.withdrawal != null || this.isPublished()) {
            return null;
        }

        LinkFailure failure = this.client.getFailure();

        if (failure != null) {
            return failure;
        }

        boolean registered = false;

        for (NodeSession node : this.client.getNodeSessions()) {
            if (!node.isSettled()) {
                return null;
            }

            if (node.isRegistered()) {
                Publication publication = this.publications.get(node);

                if (publication == null || !publication.refused) {
                    return null;
                }

                registered = true;
            }
        }

        return registered ? LinkFailure.ROOM_REFUSED : null;
    }

    /**
     * @return {@code true} if the room was withdrawn and no node waits for its withdrawal any more, {@code false} otherwise
     */
    boolean isFinished() {
        if (this.withdrawal == null) {
            return false;
        }

        for (Map.Entry<NodeSession, Publication> entry : this.publications.entrySet()) {
            Publication publication = entry.getValue();

            if (publication.request != NO_REQUEST || publication.isPublished() && !publication.withdrawn && entry.getKey().isRegistered()) {
                return false;
            }
        }

        return true;
    }

    private boolean isPublished() {
        for (Map.Entry<NodeSession, Publication> entry : this.publications.entrySet()) {
            if (entry.getValue().isPublished() && entry.getKey().isRegistered()) {
                return true;
            }
        }

        return false;
    }


    /**
     * Replaces the room's settings, for example its player count; the nodes receive the new settings with the next poll.
     *
     * @param settings the new settings
     * @throws IllegalArgumentException if a name is longer than the room advertisement allows
     * @throws IllegalStateException    if the room was withdrawn
     */
    @Override
    public void update(@NotNull RoomSettings settings) {
        if (this.withdrawal != null) {
            throw Exceptions.newIllegalStateException("The room {0} was withdrawn.", this.settings);
        }

        long nowMillis = this.client.getEpochMillis();

        this.description = this.client.createDescription(settings, this.getJoinKey());
        this.settings    = settings;
        this.renew(nowMillis);
    }

    /**
     * Takes the channel of the next participant that joined the room: it passed the join check (the engine version and the mod identity are
     * the room's) and its channel opened. Returns at once when none waits.
     *
     * @return the channel, or {@code null} if no joined participant waits
     */
    @Override
    public @Nullable ParticipantChannel acceptChannel() {
        return this.joinedChannels.pollFirst();
    }

    /**
     * Hands out the channel of a joined participant that opened.
     *
     * @param channel the channel
     */
    void queueChannel(@NotNull PeerChannel channel) {
        this.joinedChannels.addLast(channel);
    }

    /**
     * Withdraws the room: the nodes that hold it receive a signed withdrawal with the next poll. Calling it again does nothing.
     */
    @Override
    public void withdraw() {
        if (this.withdrawal != null) {
            return;
        }

        long nowMillis = this.client.getEpochMillis();

        this.sequence   = nextSequence(this.sequence, nowMillis);
        this.withdrawal = RoomAdvertisement.signWithdrawal(this.client.getKeyPair(), this.client.getConnection(), this.client.getTopic(),
                                                           this.number, this.sequence,
                                                           nowMillis + this.client.getConfiguration().getRoomLifetimeMillis(), this.description,
                                                           this.homePeers);
        this.client.cancelRequests(this);

        for (Publication publication : this.publications.values()) {
            publication.request = NO_REQUEST;
        }
    }

    /**
     * Sends what is due: the current advertisement to every registered node that does not hold it, a new advertisement after the refresh
     * time, or the withdrawal to the nodes that hold the room.
     *
     * @param nowMillis the protocol clock time
     */
    void maintain(long nowMillis) {
        RoomAdvertisement sent = this.withdrawal;

        if (sent == null && (nowMillis >= this.refreshMillis || !this.homePeers.equals(this.client.getHomeEndpoints()))) {
            this.renew(nowMillis);
        }

        for (NodeSession node : this.client.getNodeSessions()) {
            if (!node.isRegistered()) {
                continue;
            }

            Publication publication = this.publications.computeIfAbsent(node, ignored -> new Publication());

            if (publication.request != NO_REQUEST) {
                continue;
            }

            if (sent != null) {
                if (publication.isPublished() && !publication.withdrawn) {
                    publication.request          = this.client.sendRequest(node, new WithdrawRoomMessage(sent), false, this);
                    publication.requestWithdraws = true;
                }
            } else if (!publication.refused && (publication.publishedSequence != this.sequence ||
                                                publication.registrationCount != node.getRegistrationCount())) {
                publication.request          = this.client.sendRequest(node, new PublishRoomMessage(this.advertisement), false, this);
                publication.requestSequence  = this.sequence;
                publication.requestWithdraws = false;
            }
        }
    }

    /**
     * Sends the withdrawal once to every registered node that holds the room, without waiting for answers: the session closes.
     */
    void withdrawAtClose() {
        this.withdraw();

        RoomAdvertisement sent = this.withdrawal;

        if (sent == null) {
            return;
        }

        for (Map.Entry<NodeSession, Publication> entry : this.publications.entrySet()) {
            Publication publication = entry.getValue();

            if (publication.isPublished() && !publication.withdrawn && entry.getKey().isRegistered()) {
                this.client.sendOnce(entry.getKey(), new WithdrawRoomMessage(sent), false);
            }
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
        Publication publication = this.publications.get(node);

        if (publication == null || publication.request != reply.getRequestNumber()) {
            return;
        }

        publication.request = NO_REQUEST;

        if (!(reply.getBody() instanceof ResultMessage result)) {
            return;
        }

        switch (result.getStatus()) {
            case ACCEPTED, STALE -> {
                if (publication.requestWithdraws) {
                    publication.withdrawn = true;
                } else {
                    publication.publishedSequence = publication.requestSequence;
                    publication.registrationCount = node.getRegistrationCount();
                }
            }
            case NOT_REGISTERED -> node.markRegistrationLost(nowMillis);
            case INVALID, LIMIT_REACHED, DELIVERED, UNKNOWN_TARGET -> {
                if (publication.requestWithdraws) {
                    publication.withdrawn = true;
                } else {
                    publication.refused = true;
                }
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
        Publication publication = this.publications.get(node);

        if (publication != null && publication.request == requestNumber) {
            publication.request = NO_REQUEST;
        }
    }

    private void renew(long nowMillis) {
        this.sequence      = nextSequence(this.sequence, nowMillis);
        this.homePeers     = this.client.getHomeEndpoints();
        this.advertisement = this.signPublication(nowMillis);
        this.refreshMillis = nowMillis + this.client.getConfiguration().getRoomRefreshMillis();

        for (Publication publication : this.publications.values()) {
            publication.refused = false;
        }
    }

    private @NotNull RoomAdvertisement signPublication(long nowMillis) {
        return RoomAdvertisement.signPublication(this.client.getKeyPair(), this.client.getConnection(), this.client.getTopic(),
                                                 this.number, this.sequence,
                                                 nowMillis + this.client.getConfiguration().getRoomLifetimeMillis(), this.description,
                                                 this.homePeers, this.client.getCoordinates().getSettledCoordinate());
    }

    /**
     * @return the room number, the sequence and the settings, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.number + "#" + this.sequence + " " + this.settings;
    }
}
