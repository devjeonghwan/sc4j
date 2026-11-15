package org.fogofwarlabs.sc4j.beaconlink.peer.routing;

import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the full peers a peer routes through: Kademlia k-buckets by the length of the prefix a peer's ID shares with the own ID, each with at
 * most {@link PeerConfiguration#getBucketSize()} peers and a small replacement cache of proved peers that found the bucket full.
 * <p>
 * A full bucket keeps the peers it has (long-lived peers stay; Kademlia's preference): a new peer waits in the replacement cache until a peer
 * of the bucket leaves, and the newest cached peer that keeps the limits takes its place. The table limits the share of one network, so that
 * many IDs at the addresses of one operator cannot fill it: per bucket and per table for an IPv4 /24 (IPv6 /48) network, and per table for an
 * IPv4 /16 (IPv6 /32) network. Loopback addresses (peers of one machine) are not limited.
 * <p>
 * The table only holds peers whose address and key are proved and whose proof was checked; {@link PeerRouting} decides who enters.
 */
final class RoutingTable {
    private static final int NETWORK_BYTES_IPV4 = 3;
    private static final int WIDE_NETWORK_BYTES_IPV4 = 2;
    private static final int NETWORK_BYTES_IPV6 = 6;
    private static final int WIDE_NETWORK_BYTES_IPV6 = 4;
    private static final int IPV6_TAG = 0x40;
    private static final int TAG_SHIFT = Long.SIZE - Byte.SIZE;
    private static final int BYTE_MASK = 0xff;

    /**
     * Holds one k-bucket: its peers in the order they entered, and the replacement cache, the newest last.
     */
    private static final class Bucket {
        private final @NotNull List<@NotNull RoutingContact> entries;
        private final @NotNull List<@NotNull RoutingContact> replacements;

        private Bucket() {
            this.entries      = new ArrayList<>();
            this.replacements = new ArrayList<>();
        }
    }

    /**
     * Computes the key of an address's network: the first bytes of the address with its family, or {@code null} for a loopback address,
     * which no network limit counts.
     *
     * @param address the IP address
     * @param wide    {@code true} for the IPv4 /16 (IPv6 /32) network, {@code false} for the IPv4 /24 (IPv6 /48) network
     * @return the network key, or {@code null} for a loopback address
     */
    static @Nullable Long findNetwork(@NotNull InetAddress address, boolean wide) {
        if (address.isLoopbackAddress()) {
            return null;
        }

        byte[] bytes = address.getAddress();
        boolean ipv4 = address instanceof Inet4Address;
        int length = ipv4 ? (wide ? WIDE_NETWORK_BYTES_IPV4 : NETWORK_BYTES_IPV4) : (wide ? WIDE_NETWORK_BYTES_IPV6 : NETWORK_BYTES_IPV6);
        long network = 0L;

        for (int index = 0; index < length; index++) {
            network = (network << Byte.SIZE) | (bytes[index] & BYTE_MASK);
        }

        return network | (long) (ipv4 ? length : length | IPV6_TAG) << TAG_SHIFT;
    }


    private final @NotNull PeerId ownId;
    private final @NotNull PeerConfiguration configuration;

    private final @Nullable Bucket @NotNull [] buckets;
    private final @NotNull Map<@NotNull IdentityKey, @NotNull RoutingContact> contacts;
    private final @NotNull Map<@NotNull Long, @NotNull Integer> networkCounts;
    private final @NotNull Map<@NotNull Long, @NotNull Integer> wideNetworkCounts;

    /**
     * Creates an empty {@link RoutingTable} instance.
     *
     * @param ownId         the ID of the table's peer
     * @param configuration the bucket size, the replacement cache size and the network limits
     */
    RoutingTable(@NotNull PeerId ownId, @NotNull PeerConfiguration configuration) {
        this.ownId         = ownId;
        this.configuration = configuration;

        this.buckets           = new Bucket[PeerId.BIT_LENGTH];
        this.contacts          = new HashMap<>();
        this.networkCounts     = new HashMap<>();
        this.wideNetworkCounts = new HashMap<>();
    }

    /**
     * @return the number of peers in the table
     */
    int getSize() {
        return this.contacts.size();
    }

    /**
     * Finds a peer of the table.
     *
     * @param key the peer's key
     * @return the peer, or {@code null} if it is not in the table
     */
    @Nullable RoutingContact find(@NotNull IdentityKey key) {
        return this.contacts.get(key);
    }

    /**
     * @return every peer of the table, bucket by bucket from the farthest
     */
    @NotNull List<@NotNull RoutingContact> getContacts() {
        List<RoutingContact> all = new ArrayList<>(this.contacts.size());

        for (Bucket bucket : this.buckets) {
            if (bucket != null) {
                all.addAll(bucket.entries);
            }
        }

        return all;
    }

    /**
     * @return one more than the index of the deepest bucket that holds a peer, {@code 0} for an empty table: the buckets a refresh covers
     */
    int getUsedBucketCount() {
        for (int index = this.buckets.length - 1; index >= 0; index--) {
            Bucket bucket = this.buckets[index];

            if (bucket != null && !bucket.entries.isEmpty()) {
                return index + 1;
            }
        }

        return 0;
    }

    /**
     * Checks whether a peer would enter the table now: its bucket has room and its network is within the limits.
     *
     * @param id      the peer's ID
     * @param address the peer's IP address
     * @return {@code true} if the peer would enter, {@code false} otherwise (it would wait in the replacement cache or is the own ID)
     */
    boolean hasRoom(@NotNull PeerId id, @NotNull InetAddress address) {
        int index = this.ownId.getCommonPrefixLength(id);

        if (index >= PeerId.BIT_LENGTH) {
            return false;
        }

        Bucket bucket = this.buckets[index];

        return bucket == null || bucket.entries.size() < this.configuration.getBucketSize() && this.isWithinLimits(bucket, address);
    }


    /**
     * Adds a proved full peer: into its bucket when the bucket has room and the network limits allow it, otherwise into the bucket's
     * replacement cache (the oldest cached peer gives way).
     *
     * @param contact the peer, not in the table yet
     * @return {@code true} if the peer entered the table, {@code false} otherwise (it waits in the replacement cache or is the own ID)
     */
    boolean add(@NotNull RoutingContact contact) {
        int index = this.ownId.getCommonPrefixLength(contact.getId());

        if (index >= PeerId.BIT_LENGTH || this.contacts.containsKey(contact.getKey())) {
            return false;
        }

        Bucket bucket = this.buckets[index];

        if (bucket == null) {
            bucket              = new Bucket();
            this.buckets[index] = bucket;
        }

        bucket.replacements.removeIf(replacement -> replacement.getKey().equals(contact.getKey()));

        if (bucket.entries.size() < this.configuration.getBucketSize() && this.isWithinLimits(bucket, contact.getAddress().getAddress())) {
            this.insert(bucket, contact);

            return true;
        }

        if (bucket.replacements.size() >= this.configuration.getReplacementCacheSize()) {
            bucket.replacements.removeFirst();
        }

        bucket.replacements.add(contact);

        return false;
    }

    /**
     * Removes a peer from the table; the newest cached peer of its bucket that keeps the network limits takes its place.
     *
     * @param contact the peer
     * @return the peer that took the place, or {@code null} if none did
     */
    @Nullable RoutingContact remove(@NotNull RoutingContact contact) {
        if (this.contacts.get(contact.getKey()) != contact) {
            return null;
        }

        Bucket bucket = this.buckets[this.ownId.getCommonPrefixLength(contact.getId())];

        if (bucket == null) {
            return null;
        }

        bucket.entries.remove(contact);
        this.contacts.remove(contact.getKey());
        this.changeCounts(contact.getAddress().getAddress(), -1);

        for (int index = bucket.replacements.size() - 1; index >= 0; index--) {
            RoutingContact replacement = bucket.replacements.get(index);

            if (this.isWithinLimits(bucket, replacement.getAddress().getAddress())) {
                bucket.replacements.remove(index);
                this.insert(bucket, replacement);

                return replacement;
            }
        }

        return null;
    }

    /**
     * Finds the peers of the table closest to a target.
     *
     * @param target   the target ID
     * @param count    the largest number of peers
     * @param excluded the key of a peer to leave out (the asking peer), or {@code null}
     * @return the peers, closest first
     */
    @NotNull List<@NotNull RoutingContact> findClosest(@NotNull PeerId target, int count, @Nullable IdentityKey excluded) {
        List<RoutingContact> all = new ArrayList<>(this.contacts.size());

        for (RoutingContact contact : this.contacts.values()) {
            if (!contact.getKey().equals(excluded)) {
                all.add(contact);
            }
        }

        all.sort((first, second) -> target.compareDistance(first.getId(), second.getId()));

        return all.size() <= count ? all : new ArrayList<>(all.subList(0, count));
    }

    private void insert(@NotNull Bucket bucket, @NotNull RoutingContact contact) {
        bucket.entries.add(contact);
        this.contacts.put(contact.getKey(), contact);
        this.changeCounts(contact.getAddress().getAddress(), 1);
    }

    private boolean isWithinLimits(@NotNull Bucket bucket, @NotNull InetAddress address) {
        Long network = findNetwork(address, false);
        Long wideNetwork = findNetwork(address, true);

        if (network == null || wideNetwork == null) {
            return true;
        }

        int inBucket = 0;

        for (RoutingContact entry : bucket.entries) {
            if (network.equals(findNetwork(entry.getAddress().getAddress(), false))) {
                inBucket++;
            }
        }

        return inBucket < this.configuration.getMaximumPerNetworkInBucket() &&
               this.networkCounts.getOrDefault(network, 0) < this.configuration.getMaximumPerNetworkInTable() &&
               this.wideNetworkCounts.getOrDefault(wideNetwork, 0) < this.configuration.getMaximumPerWideNetworkInTable();
    }

    private void changeCounts(@NotNull InetAddress address, int change) {
        Long network = findNetwork(address, false);
        Long wideNetwork = findNetwork(address, true);

        if (network == null || wideNetwork == null) {
            return;
        }

        this.networkCounts.merge(network, change, (count, delta) -> count + delta == 0 ? null : count + delta);
        this.wideNetworkCounts.merge(wideNetwork, change, (count, delta) -> count + delta == 0 ? null : count + delta);
    }
}
