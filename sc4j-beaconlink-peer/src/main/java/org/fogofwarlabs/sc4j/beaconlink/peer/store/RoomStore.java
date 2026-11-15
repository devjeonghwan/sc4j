package org.fogofwarlabs.sc4j.beaconlink.peer.store;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomAdvertisement;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.RecordId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Holds the room advertisements of one topic at a peer: published and withdrawn rooms from the peer's clients and from the topic's other
 * members. The store checks only the expiry, the sequence and the host's signature, never the content; it is bounded in total, per host key
 * and per connection of a host.
 * Withdrawals stay until their expiry so that an older advertisement of the room is refused as stale. Rooms are listed in the stable order of
 * their {@link RoomKey}; every stored advertisement is found by its {@link RecordId} as well.
 */
public final class RoomStore {
    private static boolean isListed(@NotNull RoomAdvertisement advertisement, long nowMillis) {
        return !advertisement.isWithdrawn() && advertisement.getExpiryMillis() > nowMillis;
    }


    private final int capacity;
    private final int capacityPerHost;
    private final int capacityPerConnection;
    private final long maximumLifetimeMillis;

    private final @NotNull TreeMap<@NotNull RoomKey, @NotNull RoomAdvertisement> rooms;
    private final @NotNull HashMap<@NotNull RecordId, @NotNull RoomAdvertisement> records;
    private final @NotNull HashMap<@NotNull IdentityKey, @NotNull Integer> roomsPerHost;

    /**
     * Creates a {@link RoomStore} instance.
     *
     * @param capacity              the largest number of rooms, withdrawals included
     * @param capacityPerHost       the largest number of rooms of one host key, withdrawals included
     * @param capacityPerConnection the largest number of rooms of one connection of a host, withdrawals included
     * @param maximumLifetimeMillis the longest time an advertisement may expire after the moment it is offered
     * @throws IllegalArgumentException if a value is not positive
     */
    public RoomStore(int capacity, int capacityPerHost, int capacityPerConnection, long maximumLifetimeMillis) {
        if (capacity <= 0 || capacityPerHost <= 0 || capacityPerConnection <= 0 || maximumLifetimeMillis <= 0L) {
            throw Exceptions.newIllegalArgumentException("The room capacity {0}, the host capacity {1}, the connection capacity {2} and the " +
                                                         "lifetime {3} must be positive.",
                                                         capacity, capacityPerHost, capacityPerConnection, maximumLifetimeMillis);
        }

        this.capacity              = capacity;
        this.capacityPerHost       = capacityPerHost;
        this.capacityPerConnection = capacityPerConnection;
        this.maximumLifetimeMillis = maximumLifetimeMillis;

        this.rooms        = new TreeMap<>();
        this.records      = new HashMap<>();
        this.roomsPerHost = new HashMap<>();
    }

    /**
     * @return the number of stored advertisements, withdrawals and expired ones included until they are removed
     */
    public int getSize() {
        return this.rooms.size();
    }

    /**
     * @return the IDs of every stored advertisement, withdrawals included, in room key order
     */
    public @NotNull List<@NotNull RecordId> getRecordIds() {
        List<RecordId> ids = new ArrayList<>(this.rooms.size());

        for (RoomAdvertisement advertisement : this.rooms.values()) {
            ids.add(advertisement.getRecordId());
        }

        return ids;
    }


    /**
     * Checks whether an advertisement is not newer than the stored one of its room, without checking its signature.
     *
     * @param advertisement the advertisement
     * @return {@code true} if the store holds the room with the same or a higher sequence, {@code false} otherwise
     */
    public boolean isStale(@NotNull RoomAdvertisement advertisement) {
        RoomAdvertisement stored = this.rooms.get(advertisement.getKey());

        return stored != null && stored.getSequence() >= advertisement.getSequence();
    }

    /**
     * Offers an advertisement: stores it if it is valid and newer than the stored one of its room.
     *
     * @param advertisement the advertisement, from a host or another member of the topic
     * @param nowMillis     the current time
     * @return the outcome
     */
    public @NotNull RecordOffer offer(@NotNull RoomAdvertisement advertisement, long nowMillis) {
        if (advertisement.getExpiryMillis() <= nowMillis) {
            return RecordOffer.EXPIRED;
        }

        if (advertisement.getExpiryMillis() - nowMillis > this.maximumLifetimeMillis) {
            return RecordOffer.LIFETIME_TOO_LONG;
        }

        if (this.isStale(advertisement)) {
            return RecordOffer.STALE;
        }

        if (!advertisement.isSignatureValid()) {
            return RecordOffer.INVALID_SIGNATURE;
        }

        RoomKey key = advertisement.getKey();
        RoomAdvertisement stored = this.rooms.get(key);

        if (stored == null) {
            if (this.rooms.size() >= this.capacity) {
                this.removeExpired(nowMillis);

                if (this.rooms.size() >= this.capacity) {
                    return RecordOffer.FULL;
                }
            }

            if (this.roomsPerHost.getOrDefault(key.getHost(), 0) >= this.capacityPerHost ||
                this.countConnectionRooms(key) >= this.capacityPerConnection) {
                return RecordOffer.HOST_LIMIT;
            }

            this.roomsPerHost.merge(key.getHost(), 1, Integer::sum);
        } else {
            this.records.remove(stored.getRecordId());
        }

        this.rooms.put(key, advertisement);
        this.records.put(advertisement.getRecordId(), advertisement);

        return RecordOffer.ACCEPTED;
    }

    /**
     * Counts the stored rooms of the connection that holds a room: the rooms of one host connection are neighbours in room key order.
     */
    private int countConnectionRooms(@NotNull RoomKey key) {
        return this.rooms.subMap(new RoomKey(key.getHost(), key.getConnection(), 0L), true,
                                 new RoomKey(key.getHost(), key.getConnection(), RoomKey.MAXIMUM_NUMBER), true).size();
    }

    /**
     * Finds a stored advertisement by its record ID.
     *
     * @param id the record ID
     * @return the advertisement, or {@code null} if the store holds none with the ID
     */
    public @Nullable RoomAdvertisement find(@NotNull RecordId id) {
        return this.records.get(id);
    }

    /**
     * Counts the listed rooms: published, not withdrawn and not expired.
     *
     * @param nowMillis the current time
     * @return the number of listed rooms
     */
    public int countListed(long nowMillis) {
        int count = 0;

        for (RoomAdvertisement advertisement : this.rooms.values()) {
            if (isListed(advertisement, nowMillis)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Lists the listed rooms in room key order.
     *
     * @param nowMillis the current time
     * @return the advertisements of the listed rooms
     */
    public @NotNull List<@NotNull RoomAdvertisement> getListed(long nowMillis) {
        List<RoomAdvertisement> listed = new ArrayList<>();

        for (RoomAdvertisement advertisement : this.rooms.values()) {
            if (isListed(advertisement, nowMillis)) {
                listed.add(advertisement);
            }
        }

        return listed;
    }

    /**
     * Lists one page of the listed rooms in room key order.
     *
     * @param offset       the number of listed rooms to skip
     * @param maximumBytes the largest total wire length of the page's advertisements
     * @param nowMillis    the current time
     * @return the page's advertisements
     */
    public @NotNull List<@NotNull RoomAdvertisement> listPage(int offset, int maximumBytes, long nowMillis) {
        List<RoomAdvertisement> page = new ArrayList<>();
        int skipped = 0;
        int bytes = 0;

        for (RoomAdvertisement advertisement : this.rooms.values()) {
            if (!isListed(advertisement, nowMillis)) {
                continue;
            }

            if (skipped < offset) {
                skipped++;

                continue;
            }

            if (bytes + advertisement.getEncodedLength() > maximumBytes) {
                break;
            }

            page.add(advertisement);
            bytes += advertisement.getEncodedLength();
        }

        return page;
    }

    /**
     * Removes the expired advertisements, withdrawals included.
     *
     * @param nowMillis the current time
     */
    public void removeExpired(long nowMillis) {
        Iterator<RoomAdvertisement> iterator = this.rooms.values().iterator();

        while (iterator.hasNext()) {
            RoomAdvertisement advertisement = iterator.next();

            if (advertisement.getExpiryMillis() <= nowMillis) {
                iterator.remove();
                this.records.remove(advertisement.getRecordId());
                this.roomsPerHost.computeIfPresent(advertisement.getKey().getHost(), (host, count) -> count > 1 ? count - 1 : null);
            }
        }
    }
}
