package org.fogofwarlabs.sc4j.beaconlink.node.state;

import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.PeerStore;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;

/**
 * Tests the node state folder: the key survives a restart and the learned peers round trip through their file.
 */
final class NodeStateFolderTest {
    private static final int PORT = 7455;
    private static final long NOW_MILLIS = 1_800_000_000_000L;

    @Test
    void key_and_learned_peers_survive_a_restart(@TempDir @NotNull Path directory) throws IOException {
        SecureRandom random = new SecureRandom();
        NodeStateFolder folder = NodeStateFolder.open(directory.resolve("node"));
        IdentityKeyPair key = folder.readOrCreateKey(random);
        NodeEndpoint peer = new NodeEndpoint(IdentityKeyPair.generate(random).getPublicKey(),
                                             new InetSocketAddress(InetAddress.ofLiteral("127.0.0.1"), PORT));
        PeerStore learned = PeerStore.open(folder.getPeersFile());

        Assertions.assertEquals(0, learned.getCount());

        learned.recordSuccess(List.of(peer), NOW_MILLIS);
        learned.merge(NOW_MILLIS);

        NodeStateFolder reopened = NodeStateFolder.open(directory.resolve("node"));

        Assertions.assertEquals(key.getPublicKey(), reopened.readOrCreateKey(random).getPublicKey());
        Assertions.assertEquals(List.of(peer), PeerStore.open(reopened.getPeersFile()).selectPeers(1, List.of()));
        Assertions.assertEquals(directory.resolve("node").toAbsolutePath().normalize(), reopened.getDirectory());
        Assertions.assertEquals(directory.resolve("node").resolve(PeerStore.FILE_NAME).toAbsolutePath().normalize(), reopened.getPeersFile());
        Assertions.assertEquals(directory.resolve("node").resolve(NodeStateFolder.LOG_DIRECTORY).toAbsolutePath().normalize(),
                                reopened.getLogDirectory());
    }
}
