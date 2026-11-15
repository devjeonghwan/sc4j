package org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;

/**
 * Tests the learned peers of a shared peers file: the bound, the choice of the best peers across networks, the drop rules (failed starts in a
 * row, another key at a peer's address, age), the merge rules between users of one file, and reading of damaged files.
 */
final class PeerStoreTest {
    private static final long START_MILLIS = 1_800_000_000_000L;
    private static final long MINUTE_MILLIS = 60_000L;
    private static final int PORT = 7455;
    private static final int OCTET_RANGE = 256;
    private static final int START_PEERS = 32;
    private static final @NotNull String PRIVATE_NETWORK_PREFIX = "10.";
    private static final char OCTET_SEPARATOR = '.';

    /**
     * Creates the address of a host in the /24 network {@code 10.<network / 256>.<network % 256>.0}.
     */
    private static @NotNull InetSocketAddress createAddress(int network, int host) {
        String literal = PRIVATE_NETWORK_PREFIX + network / OCTET_RANGE + OCTET_SEPARATOR + network % OCTET_RANGE + OCTET_SEPARATOR + host;

        return new InetSocketAddress(InetAddress.ofLiteral(literal), PORT);
    }

    private static @NotNull List<@NotNull NodeEndpoint> toEndpoints(@NotNull PeerStore store) {
        return store.getPeers().stream().map(LearnedPeer::getEndpoint).toList();
    }

    private static @NotNull LearnedPeer requirePeer(@NotNull PeerStore store, @NotNull IdentityKey key) {
        return store.getPeers().stream().filter(peer -> peer.getKey().equals(key)).findFirst()
                    .orElseGet(() -> Assertions.fail("the store knows no peer of the key " + key));
    }

    private final @NotNull SecureRandom random;

    PeerStoreTest() {
        this.random = new SecureRandom();
    }

    private @NotNull IdentityKey createKey() {
        return IdentityKeyPair.generate(this.random).getPublicKey();
    }

    private @NotNull NodeEndpoint createPeer(int network, int host) {
        return new NodeEndpoint(this.createKey(), createAddress(network, host));
    }

    @Test
    void record_success_should_keep_at_most_the_maximum_peers_and_drop_the_least_recent(@TempDir @NotNull Path directory) {
        PeerStore store = PeerStore.open(directory.resolve(PeerStore.FILE_NAME));
        List<NodeEndpoint> first = new ArrayList<>();
        List<NodeEndpoint> later = new ArrayList<>();
        int extra = 20;

        for (int index = 0; index < extra; index++) {
            first.add(this.createPeer(index, 1));
        }

        for (int index = 0; index < PeerStore.MAXIMUM_PEERS; index++) {
            later.add(this.createPeer(extra + index, 1));
        }

        store.recordSuccess(first, START_MILLIS);
        store.recordSuccess(later, START_MILLIS + MINUTE_MILLIS);
        store.merge(START_MILLIS + MINUTE_MILLIS);

        Set<NodeEndpoint> kept = new HashSet<>(toEndpoints(PeerStore.open(store.getFile())));

        Assertions.assertEquals(PeerStore.MAXIMUM_PEERS, kept.size());
        Assertions.assertEquals(new HashSet<>(later), kept, "the peers that answered least recently give way");
    }

    @Test
    void select_peers_should_prefer_recent_answers_and_spread_over_networks(@TempDir @NotNull Path directory) {
        PeerStore store = PeerStore.open(directory.resolve(PeerStore.FILE_NAME));
        List<NodeEndpoint> crowded = new ArrayList<>();
        List<NodeEndpoint> spread = new ArrayList<>();

        for (int host = 1; host <= START_PEERS; host++) {
            crowded.add(this.createPeer(0, host));
        }

        for (int network = 1; network <= START_PEERS; network++) {
            spread.add(this.createPeer(network, 1));
        }

        store.recordSuccess(spread, START_MILLIS);
        store.recordSuccess(crowded, START_MILLIS + MINUTE_MILLIS);

        List<NodeEndpoint> chosen = store.selectPeers(START_PEERS, List.of(crowded.getLast().getKey()));

        Assertions.assertEquals(START_PEERS, chosen.size());
        Assertions.assertEquals(PeerStore.PEERS_PER_NETWORK, chosen.stream().filter(crowded::contains).count(),
                                "one network gets only its share while other networks have peers");
        Assertions.assertTrue(crowded.contains(chosen.getFirst()), "the most recent answer comes first");
        Assertions.assertFalse(chosen.contains(crowded.getLast()), "an excluded key is never chosen");
    }

    @Test
    void merge_should_drop_a_peer_after_three_failed_starts_in_a_row(@TempDir @NotNull Path directory) {
        Path file = directory.resolve(PeerStore.FILE_NAME);
        PeerStore first = PeerStore.open(file);
        NodeEndpoint quiet = this.createPeer(1, 1);
        NodeEndpoint answering = this.createPeer(2, 1);

        first.recordSuccess(List.of(quiet, answering), START_MILLIS);
        first.merge(START_MILLIS);

        for (int start = 1; start <= PeerStore.MAXIMUM_FAILED_STARTS; start++) {
            PeerStore store = PeerStore.open(file);
            LearnedPeer known = requirePeer(store, quiet.getKey());

            Assertions.assertEquals(start - 1, known.getFailedStarts());

            store.selectPeers(START_PEERS, List.of());
            store.recordSuccess(List.of(answering), START_MILLIS + start * MINUTE_MILLIS);
            store.merge(START_MILLIS + start * MINUTE_MILLIS);
            store.merge(START_MILLIS + start * MINUTE_MILLIS + 1L);
        }

        PeerStore last = PeerStore.open(file);

        Assertions.assertEquals(List.of(answering), toEndpoints(last));
        Assertions.assertEquals(0, last.getPeers().getFirst().getFailedStarts());
        Assertions.assertEquals(START_MILLIS + PeerStore.MAXIMUM_FAILED_STARTS * MINUTE_MILLIS, last.getPeers().getFirst().getLastSuccessMillis());
        Assertions.assertEquals(START_MILLIS, last.getPeers().getFirst().getFirstSeenMillis());
    }

    @Test
    void record_success_should_drop_a_peer_whose_address_answers_with_another_key(@TempDir @NotNull Path directory) {
        PeerStore store = PeerStore.open(directory.resolve(PeerStore.FILE_NAME));
        NodeEndpoint old = this.createPeer(1, 1);
        NodeEndpoint successor = new NodeEndpoint(this.createKey(), old.getAddress());
        NodeEndpoint moved = new NodeEndpoint(successor.getKey(), createAddress(1, 2));

        store.recordSuccess(List.of(old), START_MILLIS);
        store.recordSuccess(List.of(successor), START_MILLIS + MINUTE_MILLIS);

        Assertions.assertEquals(List.of(successor), toEndpoints(store));

        store.recordSuccess(List.of(moved), START_MILLIS + 2 * MINUTE_MILLIS);

        Assertions.assertEquals(List.of(moved), toEndpoints(store));
        Assertions.assertEquals(START_MILLIS + MINUTE_MILLIS, store.getPeers().getFirst().getFirstSeenMillis(), "a moved peer keeps its age");
    }

    @Test
    void learn_should_keep_a_reported_peer_as_never_answered_without_displacing_a_known_one(@TempDir @NotNull Path directory) {
        PeerStore store = PeerStore.open(directory.resolve(PeerStore.FILE_NAME));
        NodeEndpoint known = this.createPeer(1, 1);
        NodeEndpoint reported = this.createPeer(2, 1);

        store.recordSuccess(List.of(known), START_MILLIS);

        Assertions.assertFalse(store.learn(new NodeEndpoint(known.getKey(), createAddress(3, 1)), START_MILLIS), "a known key does not move");
        Assertions.assertFalse(store.learn(new NodeEndpoint(this.createKey(), known.getAddress()), START_MILLIS), "a known address keeps its key");
        Assertions.assertTrue(store.learn(reported, START_MILLIS + MINUTE_MILLIS));

        store.merge(START_MILLIS + MINUTE_MILLIS);

        PeerStore reopened = PeerStore.open(store.getFile());
        LearnedPeer learned = requirePeer(reopened, reported.getKey());

        Assertions.assertEquals(List.of(known, reported), toEndpoints(reopened), "a peer that answered comes before a reported one");
        Assertions.assertEquals(LearnedPeer.NEVER_ANSWERED, learned.getLastSuccessMillis());
        Assertions.assertEquals(START_MILLIS + MINUTE_MILLIS, learned.getFirstSeenMillis());
    }

    @Test
    void merge_should_join_two_users_by_key_with_the_newest_success_and_the_most_failed_starts(@TempDir @NotNull Path directory) {
        Path file = directory.resolve(PeerStore.FILE_NAME);
        PeerStore first = PeerStore.open(file);
        PeerStore second = PeerStore.open(file);
        NodeEndpoint mover = this.createPeer(1, 1);
        NodeEndpoint moved = new NodeEndpoint(mover.getKey(), createAddress(1, 2));
        NodeEndpoint shared = this.createPeer(2, 1);
        NodeEndpoint oldHolder = this.createPeer(3, 1);
        NodeEndpoint newHolder = new NodeEndpoint(this.createKey(), oldHolder.getAddress());

        first.recordSuccess(List.of(mover, shared, oldHolder), START_MILLIS);
        first.merge(START_MILLIS);
        second.recordSuccess(List.of(moved, newHolder), START_MILLIS + MINUTE_MILLIS);
        second.merge(START_MILLIS + MINUTE_MILLIS);
        first.merge(START_MILLIS + 2 * MINUTE_MILLIS);

        Assertions.assertEquals(Set.of(moved, shared, newHolder), new HashSet<>(toEndpoints(first)),
                                "the newer success gives the address, and of two keys at one address the newer success stays");
        Assertions.assertEquals(START_MILLIS, requirePeer(first, mover.getKey()).getFirstSeenMillis(), "the first seen time is the earlier one");

        // Both users try the shared peer in their starts and it never answers: each user's failed start counts.
        first.selectPeers(START_PEERS, List.of(mover.getKey(), newHolder.getKey()));
        first.merge(START_MILLIS + 3 * MINUTE_MILLIS);
        second.merge(START_MILLIS + 3 * MINUTE_MILLIS);
        second.selectPeers(START_PEERS, List.of(mover.getKey(), newHolder.getKey()));
        second.merge(START_MILLIS + 4 * MINUTE_MILLIS);

        Assertions.assertEquals(2, requirePeer(PeerStore.open(file), shared.getKey()).getFailedStarts(),
                                "on an equal success more failed starts win");
    }

    @Test
    void merge_should_not_bring_back_a_peer_another_user_dropped(@TempDir @NotNull Path directory) {
        Path file = directory.resolve(PeerStore.FILE_NAME);
        PeerStore setup = PeerStore.open(file);
        NodeEndpoint quiet = this.createPeer(1, 1);
        NodeEndpoint other = this.createPeer(2, 1);

        setup.recordSuccess(List.of(quiet), START_MILLIS);
        setup.merge(START_MILLIS);

        for (int start = 1; start < PeerStore.MAXIMUM_FAILED_STARTS; start++) {
            PeerStore store = PeerStore.open(file);

            store.selectPeers(START_PEERS, List.of());
            store.merge(START_MILLIS + start * MINUTE_MILLIS);
        }

        PeerStore stale = PeerStore.open(file);
        PeerStore dropping = PeerStore.open(file);

        dropping.selectPeers(START_PEERS, List.of());
        dropping.merge(START_MILLIS + PeerStore.MAXIMUM_FAILED_STARTS * MINUTE_MILLIS);
        stale.recordSuccess(List.of(other), START_MILLIS + PeerStore.MAXIMUM_FAILED_STARTS * MINUTE_MILLIS);
        stale.merge(START_MILLIS + PeerStore.MAXIMUM_FAILED_STARTS * MINUTE_MILLIS);

        Assertions.assertEquals(List.of(other), toEndpoints(PeerStore.open(file)));
        Assertions.assertEquals(List.of(other), toEndpoints(stale), "the stale user takes the file's entries as its view");
    }

    @Test
    void merge_should_drop_peers_older_than_the_age_limit(@TempDir @NotNull Path directory) {
        PeerStore store = PeerStore.open(directory.resolve(PeerStore.FILE_NAME));
        NodeEndpoint old = this.createPeer(1, 1);
        NodeEndpoint reported = this.createPeer(2, 1);
        NodeEndpoint recent = this.createPeer(3, 1);
        long laterMillis = START_MILLIS + PeerStore.MAXIMUM_AGE_MILLIS + 1L;

        store.recordSuccess(List.of(old), START_MILLIS);
        store.learn(reported, START_MILLIS);
        store.recordSuccess(List.of(recent), laterMillis);
        store.merge(laterMillis);

        Assertions.assertEquals(List.of(recent), toEndpoints(PeerStore.open(store.getFile())));
    }

    @Test
    void open_should_hold_no_peer_for_a_missing_or_damaged_file_and_skip_invalid_entries(@TempDir @NotNull Path directory) throws IOException {
        Path file = directory.resolve(PeerStore.FILE_NAME);

        Assertions.assertTrue(PeerStore.open(file).getPeers().isEmpty());

        Files.writeString(file, "{\"format\": 1, \"peers\": [", StandardCharsets.UTF_8);

        Assertions.assertTrue(PeerStore.open(file).getPeers().isEmpty());

        Files.writeString(file, "{\"format\": 2, \"peers\": []}", StandardCharsets.UTF_8);

        Assertions.assertTrue(PeerStore.open(file).getPeers().isEmpty());

        NodeEndpoint valid = this.createPeer(1, 1);
        String validEntry = "{\"key\": \"" + HexFormat.of().formatHex(valid.getKey().getBytes()) + "\", \"address\": \"10.0.1.1\", \"port\": " + PORT +
                            ", \"firstSeen\": 1, \"lastSuccess\": 2, \"failures\": 0}";

        Files.writeString(file, "{\"format\": 1, \"peers\": [{\"key\": \"zz\"}, 7, " + validEntry + "]}", StandardCharsets.UTF_8);

        Assertions.assertEquals(List.of(valid), toEndpoints(PeerStore.open(file)));
    }

    @Test
    void merge_should_write_a_damaged_file_again_from_the_users_view(@TempDir @NotNull Path directory) throws IOException {
        Path file = directory.resolve(PeerStore.FILE_NAME);
        PeerStore store = PeerStore.open(file);
        NodeEndpoint peer = this.createPeer(1, 1);

        store.recordSuccess(List.of(peer), START_MILLIS);
        store.merge(START_MILLIS);
        Files.writeString(file, "{\"format\": 1, \"pe", StandardCharsets.UTF_8);
        store.merge(START_MILLIS + MINUTE_MILLIS);

        Assertions.assertEquals(List.of(peer), toEndpoints(PeerStore.open(file)), "a merge without changes rebuilds the damaged file");
    }
}
