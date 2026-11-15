package org.fogofwarlabs.sc4j.beaconlink.peer.routing;

import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.PeerContact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Tests the k-bucket routing table: a full bucket keeps its peers and caches newcomers, a leaving peer gives its place to the newest cached
 * one, the network limits per bucket and per table hold (loopback aside), and the closest peers come back closest first.
 */
final class RoutingTableTest {
    private static final long RANDOM_SEED = 20261109L;
    private static final int PORT = 7455;
    private static final int BUCKET_SIZE = 8;
    private static final int REPLACEMENTS = 4;
    private static final int PER_NETWORK_IN_BUCKET = 2;
    private static final int PER_NETWORK_IN_TABLE = 8;
    private static final int PER_WIDE_NETWORK_IN_TABLE = 16;
    private static final int MANY = 2000;
    private static final int FIRST_OCTET = 11;
    private static final int OCTET_COUNT = 250;

    private static @NotNull InetAddress createAddress(int first, int second, int third, int fourth) {
        return InetAddress.ofLiteral(first + "." + second + "." + third + "." + fourth);
    }

    private final @NotNull SplittableRandom random;
    private final @NotNull PeerId ownId;
    private int nextHost;

    RoutingTableTest() {
        this.random   = new SplittableRandom(RANDOM_SEED);
        this.ownId    = PeerId.createRandom(this.random);
        this.nextHost = 0;
    }

    private @NotNull RoutingContact createContact(@NotNull InetAddress address) {
        byte[] key = new byte[IdentityKey.LENGTH];

        this.random.nextBytes(key);

        return new RoutingContact(new PeerContact(IdentityKey.of(key), 0L, new InetSocketAddress(address, PORT)), true, 0L);
    }

    /**
     * Creates a contact in bucket 0 (the half of the ID space farthest from the own ID) at a fresh /16 network.
     */
    private @NotNull RoutingContact createFarContact() {
        return this.createFarContact(null);
    }

    private @NotNull RoutingContact createFarContact(@Nullable InetAddress address) {
        while (true) {
            int host = this.nextHost++;
            InetAddress chosen = address != null ? address : createAddress(FIRST_OCTET + host / OCTET_COUNT, host % OCTET_COUNT, 1, 1);
            RoutingContact contact = this.createContact(chosen);

            if (this.ownId.getCommonPrefixLength(contact.getId()) == 0) {
                return contact;
            }
        }
    }

    private @NotNull RoutingTable createTable() {
        PeerConfiguration configuration = new PeerConfiguration.Builder().setBucketSize(BUCKET_SIZE)
                                                                         .setReplacementCacheSize(REPLACEMENTS)
                                                                         .setMaximumPerNetworkInBucket(PER_NETWORK_IN_BUCKET)
                                                                         .setMaximumPerNetworkInTable(PER_NETWORK_IN_TABLE)
                                                                         .setMaximumPerWideNetworkInTable(PER_WIDE_NETWORK_IN_TABLE)
                                                                         .build();

        return new RoutingTable(this.ownId, configuration);
    }

    @Test
    void full_bucket_keeps_its_peers_and_a_leaving_peer_gives_way_to_the_newest_cached_one() {
        RoutingTable table = this.createTable();
        List<RoutingContact> entered = new ArrayList<>();
        List<RoutingContact> cached = new ArrayList<>();

        for (int index = 0; index < BUCKET_SIZE; index++) {
            RoutingContact contact = this.createFarContact();

            Assertions.assertTrue(table.add(contact));
            entered.add(contact);
        }

        for (int index = 0; index < REPLACEMENTS + 1; index++) {
            RoutingContact contact = this.createFarContact();

            Assertions.assertFalse(table.add(contact), "the full bucket keeps its long-lived peers");
            cached.add(contact);
        }

        Assertions.assertEquals(BUCKET_SIZE, table.getSize());
        Assertions.assertEquals(cached.getLast(), table.remove(entered.getFirst()));
        Assertions.assertSame(cached.getLast(), table.find(cached.getLast().getKey()));
        Assertions.assertNull(table.find(entered.getFirst().getKey()));
        Assertions.assertEquals(cached.get(cached.size() - 2), table.remove(entered.get(1)));
        Assertions.assertEquals(BUCKET_SIZE, table.getSize());
    }

    @Test
    void network_limits_hold_per_bucket_and_per_table_except_for_loopback() {
        RoutingTable table = this.createTable();
        InetAddress sameNetwork = createAddress(FIRST_OCTET, 1, 1, 1);
        int sameNetworkEntered = 0;
        int wideEntered = 0;
        int loopbackEntered = 0;

        for (int index = 0; index < BUCKET_SIZE; index++) {
            if (table.add(this.createFarContact(sameNetwork))) {
                sameNetworkEntered++;
            }
        }

        Assertions.assertEquals(PER_NETWORK_IN_BUCKET, sameNetworkEntered, "the peers of one /24 network per bucket");

        for (int index = 0; index < MANY; index++) {
            RoutingContact contact = this.createContact(createAddress(FIRST_OCTET + 1, 1, index % OCTET_COUNT, 1 + index / OCTET_COUNT));

            if (table.add(contact)) {
                wideEntered++;
            }
        }

        Assertions.assertEquals(PER_WIDE_NETWORK_IN_TABLE, wideEntered, "the peers of one /16 network in the table");

        RoutingTable loopbackTable = this.createTable();

        for (int index = 0; index < BUCKET_SIZE; index++) {
            if (loopbackTable.add(this.createFarContact(InetAddress.getLoopbackAddress()))) {
                loopbackEntered++;
            }
        }

        Assertions.assertEquals(BUCKET_SIZE, loopbackEntered, "peers of one machine are not limited");
    }

    @Test
    void find_closest_should_return_the_peers_closest_to_the_target_first() {
        RoutingTable table = this.createTable();
        List<RoutingContact> all = new ArrayList<>();

        for (int index = 0; index < MANY; index++) {
            RoutingContact contact = this.createContact(createAddress(FIRST_OCTET + index / OCTET_COUNT, index % OCTET_COUNT, 1, 1));

            if (table.add(contact)) {
                all.add(contact);
            }
        }

        PeerId target = PeerId.createRandom(this.random);
        List<RoutingContact> closest = table.findClosest(target, BUCKET_SIZE, null);

        all.sort((first, second) -> target.compareDistance(first.getId(), second.getId()));

        Assertions.assertEquals(all.subList(0, BUCKET_SIZE), closest);
        Assertions.assertFalse(table.findClosest(target, BUCKET_SIZE, closest.getFirst().getKey()).contains(closest.getFirst()));
        Assertions.assertTrue(table.getUsedBucketCount() > 1);
    }
}
