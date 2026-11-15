package org.fogofwarlabs.sc4j.beaconlink.peer.limit;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps the local penalty points of other peers and graylists the ones that misbehave: the points of a sender key at a source address are
 * counted together, and when they reach the limit before they are forgotten, the peer drops the datagrams of that key from that address until
 * the graylist time ends. There is no global reputation: every peer judges only what it received itself.
 * <p>
 * A penalty names the key and the address of a datagram, and either may belong to somebody else: the source address of a datagram whose
 * address is not proved may be spoofed, and a signed datagram may be another peer's datagram resent from elsewhere (an envelope is not bound
 * to its receiver). So the key alone and the address alone are graylisted only for a penalty whose source address is proved for its key (a
 * mesh neighbour, a routing contact, a registered client); otherwise only the pair is.
 * <p>
 * An address is graylisted by its IP address; on the loopback network, where every test peer shares one IP address, by its socket address.
 * Every table is bounded: when one is full, its oldest entry goes.
 */
public final class Graylist {
    /**
     * Holds the penalty points of one key at one address.
     */
    private static final class Penalty {
        private int points;
        private long updatedMillis;

        private Penalty(long updatedMillis) {
            this.points        = 0;
            this.updatedMillis = updatedMillis;
        }
    }

    private static @NotNull InetSocketAddress normalize(@NotNull InetSocketAddress address) {
        return address.getAddress().isLoopbackAddress() ? address : new InetSocketAddress(address.getAddress(), 0);
    }

    private static <K> void putBounded(@NotNull LinkedHashMap<K, Long> map, @NotNull K key, long untilMillis, int capacity) {
        map.remove(key);

        if (map.size() >= capacity) {
            Iterator<Long> eldest = map.values().iterator();

            eldest.next();
            eldest.remove();
        }

        map.put(key, untilMillis);
    }

    private static <K> boolean isListed(@NotNull Map<K, Long> map, @NotNull K key, long nowMillis) {
        Long untilMillis = map.get(key);

        if (untilMillis == null) {
            return false;
        }

        if (untilMillis <= nowMillis) {
            map.remove(key);

            return false;
        }

        return true;
    }


    private final int capacity;
    private final int limit;
    private final long memoryMillis;
    private final long listedMillis;

    private final @NotNull LinkedHashMap<@NotNull NodeEndpoint, @NotNull Penalty> penalties;
    private final @NotNull LinkedHashMap<@NotNull IdentityKey, @NotNull Long> listedKeys;
    private final @NotNull LinkedHashMap<@NotNull InetSocketAddress, @NotNull Long> listedAddresses;
    private final @NotNull LinkedHashMap<@NotNull NodeEndpoint, @NotNull Long> listedPairs;

    /**
     * Creates a {@link Graylist} instance.
     *
     * @param capacity     the largest number of entries of each table
     * @param limit        the penalty points at which a peer is graylisted
     * @param memoryMillis the time after a peer's last penalty after which its points are forgotten
     * @param listedMillis the time a graylisted key, address or pair stays listed
     * @throws IllegalArgumentException if a value is not positive
     */
    public Graylist(int capacity, int limit, long memoryMillis, long listedMillis) {
        if (capacity <= 0 || limit <= 0 || memoryMillis <= 0L || listedMillis <= 0L) {
            throw Exceptions.newIllegalArgumentException("The capacity {0}, the limit {1} and the times {2} and {3} must be positive.",
                                                         capacity, limit, memoryMillis, listedMillis);
        }

        this.capacity     = capacity;
        this.limit        = limit;
        this.memoryMillis = memoryMillis;
        this.listedMillis = listedMillis;

        this.penalties       = new LinkedHashMap<>();
        this.listedKeys      = new LinkedHashMap<>();
        this.listedAddresses = new LinkedHashMap<>();
        this.listedPairs     = new LinkedHashMap<>();
    }


    /**
     * Checks whether a source address is graylisted, before the datagram is decoded.
     *
     * @param address   the source address
     * @param nowMillis the current time
     * @return {@code true} if the address is graylisted, {@code false} otherwise
     */
    public boolean isListed(@NotNull InetSocketAddress address, long nowMillis) {
        return !this.listedAddresses.isEmpty() && isListed(this.listedAddresses, normalize(address), nowMillis);
    }

    /**
     * Checks whether a key is graylisted.
     *
     * @param key       the key
     * @param nowMillis the current time
     * @return {@code true} if the key is graylisted, {@code false} otherwise
     */
    public boolean isListed(@NotNull IdentityKey key, long nowMillis) {
        return !this.listedKeys.isEmpty() && isListed(this.listedKeys, key, nowMillis);
    }

    /**
     * Checks whether the sender of a signed datagram is graylisted: its key, or its key at the source address.
     *
     * @param key       the sender key
     * @param address   the source address
     * @param nowMillis the current time
     * @return {@code true} if the key or the pair is graylisted, {@code false} otherwise
     */
    public boolean isListed(@NotNull IdentityKey key, @NotNull InetSocketAddress address, long nowMillis) {
        return this.isListed(key, nowMillis) ||
               !this.listedPairs.isEmpty() && isListed(this.listedPairs, new NodeEndpoint(key, address), nowMillis);
    }

    /**
     * Adds penalty points to a key at a source address; when the points reach the limit, the pair is graylisted and its points forgotten, and
     * if the address is proved for the key, the key and the address are graylisted as well.
     *
     * @param key       the sender key
     * @param address   the source address
     * @param points    the points of the misbehaviour
     * @param proved    whether the source address is proved for the key, so that both belong to the misbehaving peer
     * @param nowMillis the current time
     * @return {@code true} if the key was graylisted by these points, {@code false} otherwise (also when only the pair was)
     */
    public boolean penalize(@NotNull IdentityKey key, @NotNull InetSocketAddress address, int points, boolean proved, long nowMillis) {
        NodeEndpoint pair = new NodeEndpoint(key, address);
        Penalty penalty = this.penalties.remove(pair);

        if (penalty == null || nowMillis - penalty.updatedMillis > this.memoryMillis) {
            penalty = new Penalty(nowMillis);
        }

        penalty.points += points;
        penalty.updatedMillis = nowMillis;

        if (penalty.points < this.limit) {
            if (this.penalties.size() >= this.capacity) {
                Iterator<Penalty> eldest = this.penalties.values().iterator();

                eldest.next();
                eldest.remove();
            }

            this.penalties.put(pair, penalty);

            return false;
        }

        putBounded(this.listedPairs, pair, nowMillis + this.listedMillis, this.capacity);

        if (!proved) {
            return false;
        }

        putBounded(this.listedKeys, key, nowMillis + this.listedMillis, this.capacity);
        putBounded(this.listedAddresses, normalize(address), nowMillis + this.listedMillis, this.capacity);

        return true;
    }

    /**
     * Removes the forgotten penalties and the ended graylist entries.
     *
     * @param nowMillis the current time
     */
    public void removeExpired(long nowMillis) {
        this.penalties.values().removeIf(penalty -> nowMillis - penalty.updatedMillis > this.memoryMillis);
        this.listedKeys.values().removeIf(untilMillis -> untilMillis <= nowMillis);
        this.listedAddresses.values().removeIf(untilMillis -> untilMillis <= nowMillis);
        this.listedPairs.values().removeIf(untilMillis -> untilMillis <= nowMillis);
    }
}
