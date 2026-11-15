package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.beaconlink.peer.account.Account;
import org.fogofwarlabs.sc4j.beaconlink.peer.account.AccountStore;
import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.KnownNodes;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerStamp;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.BeaconLink;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Stream;

/**
 * Tests what the sessions of a run use as its command line names it: the seeds and the peers file it was given, the account's key, player
 * stamp, shared friends and shared ignored players, and a guest's kept in memory only.
 */
final class ClientSetupTest {
    private static final int STAMP_DIFFICULTY = 8;
    private static final long NOW_MILLIS = 1_800_000_000_000L;

    @Test
    void open_known_nodes_should_give_the_seeds_and_the_peers_file_it_was_given(@TempDir @NotNull Path directory) {
        List<ConfiguredNode> seeds = List.of(ConfiguredNode.parse("seed.example.org:7455", null), ConfiguredNode.parse("127.0.0.1:7456", null));
        Path peersFile = directory.resolve("shared").resolve("peers.json");
        ClientSetup setup = ClientSetup.of(null, seeds, peersFile);
        KnownNodes knownNodes = setup.openKnownNodes();

        Assertions.assertEquals(peersFile, setup.getPeersFile());
        Assertions.assertEquals(seeds, knownNodes.getSeeds());
        Assertions.assertEquals(peersFile, knownNodes.getPeersFile());
        Assertions.assertNull(ClientSetup.of(null, seeds, null).openKnownNodes().getPeersFile(), "without a peers file no learned peer is kept");
        Assertions.assertFalse(Files.exists(peersFile.getParent()), "opening the known nodes writes nothing");
    }

    @Test
    void a_guest_should_keep_its_key_friends_and_ignored_players_in_memory_and_write_nothing(@TempDir @NotNull Path directory) throws IOException {
        ClientSetup setup = ClientSetup.of(null, List.of(), directory.resolve("peers.json"));
        IdentityKey friend = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();
        IdentityKeyPair key = setup.readKey(new SecureRandom());

        Assertions.assertEquals(key.getPublicKey(), setup.readKey(new SecureRandom()).getPublicKey(), "the guest keeps its key for the run");
        Assertions.assertEquals(BeaconLink.GUEST_NAME, setup.readStamp(key, STAMP_DIFFICULTY).getName(), "a guest is named Guest");
        Assertions.assertTrue(setup.readStamp(key, STAMP_DIFFICULTY).isValid(STAMP_DIFFICULTY));
        Assertions.assertNull(setup.getAccount());
        Assertions.assertNotEquals(key.getPublicKey(), ClientSetup.of(null, List.of(), null).readKey(new SecureRandom()).getPublicKey(),
                                   "two guests are two players");

        setup.addFriend(friend, NOW_MILLIS);
        setup.addIgnored(friend, NOW_MILLIS);

        Assertions.assertEquals(List.of(friend), setup.readFriends());
        Assertions.assertEquals(List.of(friend), setup.readIgnored());

        setup.removeFriend(friend, NOW_MILLIS);
        setup.removeIgnored(friend, NOW_MILLIS);

        Assertions.assertEquals(List.of(), setup.readFriends());
        Assertions.assertEquals(List.of(), setup.readIgnored());

        try (Stream<Path> listed = Files.list(directory)) {
            Assertions.assertEquals(0L, listed.count(), "a guest writes no key, no friend and no ignored player");
        }
    }

    @Test
    void key_stamp_friends_and_ignored_players_should_be_the_account_files(@TempDir @NotNull Path directory) throws IOException {
        IdentityKeyPair keyPair = IdentityKeyPair.generate(new SecureRandom());
        Account account = AccountStore.of(directory).create(keyPair, PlayerStamp.solve(keyPair.getPublicKey(), "Alice", STAMP_DIFFICULTY),
                                                            STAMP_DIFFICULTY, 0L);
        ClientSetup setup = ClientSetup.of(account, List.of(), null);
        IdentityKeyPair read = setup.readKey(new SecureRandom());
        IdentityKey friend = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();
        PlayerStamp stamp = setup.readStamp(read, STAMP_DIFFICULTY);

        Assertions.assertEquals(keyPair.getPublicKey(), read.getPublicKey(), "the client plays with the account's key");
        Assertions.assertEquals(account.getStamp(), stamp, "the client plays with the account's stamp and name");
        Assertions.assertEquals("Alice", stamp.getName());
        Assertions.assertEquals(List.of(), setup.readFriends());

        setup.addFriend(friend, NOW_MILLIS);

        Assertions.assertEquals(List.of(friend), setup.readFriends());
        Assertions.assertEquals(List.of(friend), account.openFriends().getPlayers(), "the friends are the account's shared list");

        setup.addIgnored(friend, NOW_MILLIS);

        Assertions.assertEquals(List.of(friend), setup.readIgnored());
        Assertions.assertEquals(List.of(friend), account.openIgnored().getPlayers(), "the ignored players are the account's shared list");
        Assertions.assertTrue(Files.isRegularFile(account.getDirectory().resolve(Account.IGNORED_FILE)));
    }
}
