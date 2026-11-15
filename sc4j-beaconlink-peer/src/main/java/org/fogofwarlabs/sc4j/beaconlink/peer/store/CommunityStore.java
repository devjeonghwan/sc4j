package org.fogofwarlabs.sc4j.beaconlink.peer.store;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.community.ChatLineRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.community.PresenceRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.community.SignOutRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerStamp;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.RecordId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Holds the community records of one topic at a peer: the newest presence of every connection of every player (bounded in total and per
 * player; when full, a presence that expires later takes the place of the one that expires first, of the same player while the player has
 * the most connections), the backlog of chat lines (the newest lines by timestamp, bounded) and the newest sign-out of every player (only a
 * player topic holds one, its player's). The store checks the expiry, the lifetime, the sequence, the author's signature and the author's
 * stamp ({@link PlayerStamp}; a sign-out has none), never the content. Every stored record is found by its {@link RecordId} as well.
 */
public final class CommunityStore {
    private static final @NotNull Comparator<@NotNull ChatLineRecord> LINE_ORDER = Comparator.comparingLong(ChatLineRecord::getTimestampMillis);
    private static final int HASH_MULTIPLIER = 31;

    /**
     * Names the presence of one connection of a player.
     */
    private static final class ConnectionKey {
        private static @NotNull ConnectionKey of(@NotNull PresenceRecord presence) {
            return new ConnectionKey(presence.getAuthor(), presence.getConnection());
        }


        private final @NotNull IdentityKey player;
        private final long connection;

        private ConnectionKey(@NotNull IdentityKey player, long connection) {
            this.player     = player;
            this.connection = connection;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof ConnectionKey key && this.connection == key.connection && this.player.equals(key.player);
        }

        @Override
        public int hashCode() {
            return this.player.hashCode() * HASH_MULTIPLIER + Long.hashCode(this.connection);
        }
    }


    private final int presenceCapacity;
    private final int connectionsPerPlayer;
    private final int backlogCapacity;
    private final long maximumPresenceLifetimeMillis;
    private final long clockSkewMillis;
    private final int stampDifficulty;

    private final @NotNull LinkedHashMap<@NotNull ConnectionKey, @NotNull PresenceRecord> presences;
    private final @NotNull LinkedHashMap<@NotNull RecordId, @NotNull ChatLineRecord> lines;
    private final @NotNull LinkedHashMap<@NotNull IdentityKey, @NotNull SignOutRecord> signOuts;

    /**
     * Creates a {@link CommunityStore} instance.
     *
     * @param presenceCapacity              the largest number of presences
     * @param connectionsPerPlayer          the largest number of presences of one player, one per connection
     * @param backlogCapacity               the largest number of chat lines
     * @param maximumPresenceLifetimeMillis the longest time a presence or a sign-out may expire after the moment it is offered
     * @param clockSkewMillis               the longest time a chat line or a sign-out may be dated after the moment it is offered
     * @param stampDifficulty               the number of leading zero bits an author's stamp hash needs
     * @throws IllegalArgumentException if a capacity or a time is not positive, or the difficulty is negative
     */
    public CommunityStore(int presenceCapacity, int connectionsPerPlayer, int backlogCapacity, long maximumPresenceLifetimeMillis,
                          long clockSkewMillis, int stampDifficulty) {
        if (presenceCapacity <= 0 || connectionsPerPlayer <= 0 || backlogCapacity <= 0 || maximumPresenceLifetimeMillis <= 0L ||
            clockSkewMillis <= 0L || stampDifficulty < 0) {
            throw Exceptions.newIllegalArgumentException("The presence capacity {0}, the connections per player {1}, the backlog {2}, the " +
                                                         "lifetime {3}, the skew {4} and the stamp difficulty {5} are out of range.",
                                                         presenceCapacity, connectionsPerPlayer, backlogCapacity, maximumPresenceLifetimeMillis,
                                                         clockSkewMillis, stampDifficulty);
        }

        this.presenceCapacity              = presenceCapacity;
        this.connectionsPerPlayer          = connectionsPerPlayer;
        this.backlogCapacity               = backlogCapacity;
        this.maximumPresenceLifetimeMillis = maximumPresenceLifetimeMillis;
        this.clockSkewMillis               = clockSkewMillis;
        this.stampDifficulty               = stampDifficulty;

        this.presences = new LinkedHashMap<>();
        this.lines     = new LinkedHashMap<>();
        this.signOuts  = new LinkedHashMap<>();
    }

    /**
     * @return the IDs of every stored presence, chat line and sign-out
     */
    public @NotNull List<@NotNull RecordId> getRecordIds() {
        List<RecordId> ids = new ArrayList<>(this.presences.size() + this.lines.size() + this.signOuts.size());

        for (PresenceRecord presence : this.presences.values()) {
            ids.add(presence.getRecordId());
        }

        ids.addAll(this.lines.keySet());

        for (SignOutRecord signOut : this.signOuts.values()) {
            ids.add(signOut.getRecordId());
        }

        return ids;
    }

    /**
     * Lists the unexpired presences, one per connection of a player.
     *
     * @param nowMillis the current time
     * @return the presences, oldest stored first
     */
    public @NotNull List<@NotNull PresenceRecord> getPresences(long nowMillis) {
        List<PresenceRecord> listed = new ArrayList<>();

        for (PresenceRecord presence : this.presences.values()) {
            if (presence.getExpiryMillis() > nowMillis) {
                listed.add(presence);
            }
        }

        return listed;
    }

    /**
     * @return the backlog of chat lines, oldest first by timestamp
     */
    public @NotNull List<@NotNull ChatLineRecord> getLines() {
        List<ChatLineRecord> listed = new ArrayList<>(this.lines.values());

        listed.sort(LINE_ORDER);

        return listed;
    }

    /**
     * Lists the unexpired sign-outs.
     *
     * @param nowMillis the current time
     * @return the sign-outs, at most one per player
     */
    public @NotNull List<@NotNull SignOutRecord> getSignOuts(long nowMillis) {
        List<SignOutRecord> listed = new ArrayList<>();

        for (SignOutRecord signOut : this.signOuts.values()) {
            if (signOut.getExpiryMillis() > nowMillis) {
                listed.add(signOut);
            }
        }

        return listed;
    }

    /**
     * Finds the unexpired sign-out of a player.
     *
     * @param player    the player's key
     * @param nowMillis the current time
     * @return the sign-out, or {@code null} if the store holds none of the player
     */
    public @Nullable SignOutRecord findSignOut(@NotNull IdentityKey player, long nowMillis) {
        SignOutRecord signOut = this.signOuts.get(player);

        return signOut == null || signOut.getExpiryMillis() <= nowMillis ? null : signOut;
    }

    /**
     * Checks whether a presence or a sign-out is stale: the store holds a presence of the same connection of the player, or a sign-out of the
     * same player, with the same or a higher sequence. A chat line is never stale; the members drop a repeated line by its ID.
     *
     * @param record the presence, chat line or sign-out
     * @return {@code true} if the record is stale, {@code false} otherwise
     */
    public boolean isStale(@NotNull TopicRecord record) {
        TopicRecord stored = switch (record) {
            case PresenceRecord presence -> this.presences.get(ConnectionKey.of(presence));
            case SignOutRecord signOut -> this.signOuts.get(signOut.getAuthor());
            default -> null;
        };

        return stored != null && stored.getSequence() >= record.getSequence();
    }


    /**
     * Offers a presence, a chat line or a sign-out: it is stored if it is unexpired, within its lifetime, newer than the stored presence of its
     * connection or the stored sign-out of its player, validly signed and stamped, and there is room for it.
     *
     * @param record    the presence, chat line or sign-out
     * @param nowMillis the current time
     * @return the outcome
     */
    public @NotNull RecordOffer offer(@NotNull TopicRecord record, long nowMillis) {
        if (record.getExpiryMillis() <= nowMillis) {
            return RecordOffer.EXPIRED;
        }

        RecordOffer timing = switch (record) {
            case PresenceRecord presence -> presence.getExpiryMillis() - nowMillis > this.maximumPresenceLifetimeMillis
                                            ? RecordOffer.LIFETIME_TOO_LONG
                                            : null;
            case ChatLineRecord line -> line.getTimestampMillis() - nowMillis > this.clockSkewMillis ? RecordOffer.LIFETIME_TOO_LONG : null;
            case SignOutRecord signOut -> signOut.getExpiryMillis() - nowMillis > this.maximumPresenceLifetimeMillis ||
                                          signOut.getTimestampMillis() - nowMillis > this.clockSkewMillis ? RecordOffer.LIFETIME_TOO_LONG : null;
            default -> RecordOffer.WRONG_TOPIC;
        };

        if (timing != null) {
            return timing;
        }

        if (this.isStale(record)) {
            return RecordOffer.STALE;
        }

        if (!record.isSignatureValid()) {
            return RecordOffer.INVALID_SIGNATURE;
        }

        return switch (record) {
            case PresenceRecord presence -> presence.getStamp().isValid(this.stampDifficulty) ? this.storePresence(presence)
                                                                                              : RecordOffer.INVALID_STAMP;
            case ChatLineRecord line -> line.getStamp().isValid(this.stampDifficulty) ? this.storeLine(line) : RecordOffer.INVALID_STAMP;
            case SignOutRecord signOut -> {
                this.signOuts.remove(signOut.getAuthor());
                this.signOuts.put(signOut.getAuthor(), signOut);

                yield RecordOffer.ACCEPTED;
            }
            default -> RecordOffer.WRONG_TOPIC;
        };
    }

    /**
     * Stores a presence in the place of the stored presence of its connection. A presence of a new connection takes the place of the
     * player's presence that expires first while the player has the most connections, else of the presence that expires first while the
     * store is full; in both cases only if that one expires before the new one.
     */
    private @NotNull RecordOffer storePresence(@NotNull PresenceRecord presence) {
        ConnectionKey key = ConnectionKey.of(presence);

        if (!this.presences.containsKey(key)) {
            PresenceRecord playerFirst = null;
            PresenceRecord first = null;
            int playerCount = 0;

            for (PresenceRecord stored : this.presences.values()) {
                if (stored.getAuthor().equals(presence.getAuthor())) {
                    playerCount++;

                    if (playerFirst == null || stored.getExpiryMillis() < playerFirst.getExpiryMillis()) {
                        playerFirst = stored;
                    }
                }

                if (first == null || stored.getExpiryMillis() < first.getExpiryMillis()) {
                    first = stored;
                }
            }

            PresenceRecord replaced = null;

            if (playerCount >= this.connectionsPerPlayer) {
                replaced = playerFirst;
            } else if (this.presences.size() >= this.presenceCapacity) {
                replaced = first;
            }

            if (replaced != null) {
                if (replaced.getExpiryMillis() >= presence.getExpiryMillis()) {
                    return RecordOffer.FULL;
                }

                this.presences.remove(ConnectionKey.of(replaced));
            }
        }

        this.presences.remove(key);
        this.presences.put(key, presence);

        return RecordOffer.ACCEPTED;
    }

    private @NotNull RecordOffer storeLine(@NotNull ChatLineRecord line) {
        if (this.lines.size() >= this.backlogCapacity) {
            ChatLineRecord oldest = null;

            for (ChatLineRecord stored : this.lines.values()) {
                if (oldest == null || LINE_ORDER.compare(stored, oldest) < 0) {
                    oldest = stored;
                }
            }

            if (oldest == null || LINE_ORDER.compare(line, oldest) <= 0) {
                return RecordOffer.FULL;
            }

            this.lines.remove(oldest.getRecordId());
        }

        this.lines.put(line.getRecordId(), line);

        return RecordOffer.ACCEPTED;
    }

    /**
     * Finds a stored presence, chat line or sign-out by its ID.
     *
     * @param id the ID
     * @return the record, or {@code null} if none is stored with the ID
     */
    public @Nullable TopicRecord find(@NotNull RecordId id) {
        ChatLineRecord line = this.lines.get(id);

        if (line != null) {
            return line;
        }

        for (PresenceRecord presence : this.presences.values()) {
            if (presence.getRecordId().equals(id)) {
                return presence;
            }
        }

        for (SignOutRecord signOut : this.signOuts.values()) {
            if (signOut.getRecordId().equals(id)) {
                return signOut;
            }
        }

        return null;
    }

    /**
     * Removes the expired presences, chat lines and sign-outs.
     *
     * @param nowMillis the current time
     */
    public void removeExpired(long nowMillis) {
        this.presences.values().removeIf(presence -> presence.getExpiryMillis() <= nowMillis);
        this.lines.values().removeIf(line -> line.getExpiryMillis() <= nowMillis);
        this.signOuts.values().removeIf(signOut -> signOut.getExpiryMillis() <= nowMillis);
    }
}
