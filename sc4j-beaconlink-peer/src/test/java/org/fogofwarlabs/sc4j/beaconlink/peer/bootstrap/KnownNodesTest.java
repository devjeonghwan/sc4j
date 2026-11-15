package org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap;

import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.NodeListFile;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;

/**
 * Tests the known nodes of a process: the one seed rule of an install, the contacts of a start, and a process without a peers file.
 */
final class KnownNodesTest {
    private static final long NOW_MILLIS = 1_800_000_000_000L;
    private static final int PORT = 7455;
    private static final int LIMIT = 16;

    @Test
    void read_seeds_should_use_the_shipped_seeds_until_the_user_saves_a_node_list(@TempDir @NotNull Path directory) throws IOException {
        Path nodesFile = directory.resolve("userdata").resolve(NodeListFile.FILE_NAME);
        Path seedsFile = directory.resolve("launcher").resolve("seeds.json");
        ConfiguredNode shipped = ConfiguredNode.of("seed.example.org", PORT, null);
        ConfiguredNode own = ConfiguredNode.of("node.example.org", PORT, null);

        Assertions.assertEquals(List.of(), KnownNodes.readSeeds(nodesFile, seedsFile), "no list holds no seed");

        Files.createDirectories(seedsFile.getParent());
        new NodeListFile(seedsFile).write(List.of(shipped));

        Assertions.assertEquals(List.of(shipped), KnownNodes.readSeeds(nodesFile, seedsFile));
        Assertions.assertEquals(List.of(), KnownNodes.readSeeds(nodesFile, null));

        Files.createDirectories(nodesFile.getParent());
        new NodeListFile(nodesFile).write(List.of(own));

        Assertions.assertEquals(List.of(own), KnownNodes.readSeeds(nodesFile, seedsFile), "the user's list replaces the shipped seeds");

        new NodeListFile(nodesFile).write(List.of());

        Assertions.assertEquals(List.of(), KnownNodes.readSeeds(nodesFile, seedsFile), "a saved empty list means no node");
    }

    @Test
    void get_contacts_should_list_the_seeds_then_the_best_learned_peers_with_their_keys(@TempDir @NotNull Path directory) {
        Path peersFile = directory.resolve(PeerStore.FILE_NAME);
        SecureRandom random = new SecureRandom();
        ConfiguredNode seed = ConfiguredNode.of("seed.example.org", PORT, null);
        NodeEndpoint older = new NodeEndpoint(IdentityKeyPair.generate(random).getPublicKey(),
                                              new InetSocketAddress(InetAddress.ofLiteral("10.0.1.1"), PORT));
        NodeEndpoint newer = new NodeEndpoint(IdentityKeyPair.generate(random).getPublicKey(),
                                              new InetSocketAddress(InetAddress.ofLiteral("10.0.2.1"), PORT));
        KnownNodes first = KnownNodes.open(List.of(seed), peersFile);

        first.recordSuccess(older, NOW_MILLIS);
        first.recordSuccess(newer, NOW_MILLIS + 1L);
        first.flush(NOW_MILLIS + 1L);

        KnownNodes second = KnownNodes.open(List.of(seed), peersFile);

        Assertions.assertEquals(List.of(seed, ConfiguredNode.of(newer), ConfiguredNode.of(older)), second.getContacts(LIMIT, List.of()));
        Assertions.assertEquals(2, second.getPeerCount());
        Assertions.assertEquals(peersFile.toAbsolutePath().normalize(), second.getPeersFile());
    }

    @Test
    void known_nodes_without_a_peers_file_keep_no_peer(@TempDir @NotNull Path directory) {
        KnownNodes nodes = KnownNodes.open(List.of(), null);
        NodeEndpoint peer = new NodeEndpoint(IdentityKeyPair.generate(new SecureRandom()).getPublicKey(),
                                             new InetSocketAddress(InetAddress.ofLiteral("10.0.1.1"), PORT));

        nodes.recordSuccess(peer, NOW_MILLIS);

        Assertions.assertFalse(nodes.learn(peer, NOW_MILLIS));

        nodes.flush(NOW_MILLIS);

        Assertions.assertEquals(List.of(), nodes.getContacts(LIMIT, List.of()));
        Assertions.assertNull(nodes.getPeersFile());
        Assertions.assertEquals(0, nodes.getPeerCount());
        Assertions.assertFalse(nodes.isFlushDue(Long.MAX_VALUE));
        Assertions.assertFalse(Files.exists(directory.resolve(PeerStore.FILE_NAME)));
    }
}
