package org.fogofwarlabs.sc4j.beaconlink.peer.account;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Tests a player list several processes share: a removal is kept while another process adds a player, the users follow each other's changes,
 * the oldest additions give way beyond the most players, and a removal leaves the file after its time.
 */
final class PlayerListStoreTest {
    private static final long NOW_MILLIS = 1_800_000_000_000L;
    private static final long SECOND_MILLIS = 1_000L;
    private static final int MAXIMUM_PLAYERS = 4;
    private static final @NotNull String FILE_NAME = "friends.json";

    private final @NotNull SecureRandom random;

    PlayerListStoreTest() {
        this.random = new SecureRandom();
    }

    private @NotNull IdentityKey createKey() {
        return IdentityKeyPair.generate(this.random).getPublicKey();
    }

    @Test
    void a_removal_stays_while_another_user_adds_a_player(@TempDir @NotNull Path directory) {
        Path file = directory.resolve(FILE_NAME);
        IdentityKey firstFriend = this.createKey();
        IdentityKey secondFriend = this.createKey();
        PlayerListStore setup = PlayerListStore.open(file, MAXIMUM_PLAYERS);

        setup.add(firstFriend, NOW_MILLIS);

        PlayerListStore launcher = PlayerListStore.open(file, MAXIMUM_PLAYERS);
        PlayerListStore game = PlayerListStore.open(file, MAXIMUM_PLAYERS);

        launcher.remove(firstFriend, NOW_MILLIS + SECOND_MILLIS);
        game.add(secondFriend, NOW_MILLIS + 2 * SECOND_MILLIS);

        Assertions.assertEquals(List.of(secondFriend), game.getPlayers(), "the game's merge keeps the launcher's removal");
        Assertions.assertEquals(List.of(secondFriend), PlayerListStore.open(file, MAXIMUM_PLAYERS).getPlayers());
        Assertions.assertFalse(launcher.reloadWhenDue(NOW_MILLIS), "the first call only schedules a read");
        Assertions.assertTrue(launcher.reloadWhenDue(NOW_MILLIS + PlayerListStore.RELOAD_INTERVAL_MILLIS), "the launcher follows the game");
        Assertions.assertEquals(List.of(secondFriend), launcher.getPlayers());
    }

    @Test
    void add_should_let_the_oldest_additions_give_way_beyond_the_most_players(@TempDir @NotNull Path directory) {
        PlayerListStore store = PlayerListStore.open(directory.resolve(FILE_NAME), MAXIMUM_PLAYERS);
        List<IdentityKey> added = new ArrayList<>();

        for (int index = 0; index <= MAXIMUM_PLAYERS; index++) {
            IdentityKey player = this.createKey();

            added.add(player);
            store.add(player, NOW_MILLIS + index);
        }

        Assertions.assertEquals(added.subList(1, added.size()), store.getPlayers());
        Assertions.assertEquals(added.subList(1, added.size()), PlayerListStore.open(store.getFile(), MAXIMUM_PLAYERS).getPlayers());
    }

    @Test
    void a_removal_leaves_the_file_after_its_time(@TempDir @NotNull Path directory) throws IOException {
        PlayerListStore store = PlayerListStore.open(directory.resolve(FILE_NAME), MAXIMUM_PLAYERS);
        IdentityKey removed = this.createKey();
        IdentityKey kept = this.createKey();
        String removedHex = HexFormat.of().formatHex(removed.getBytes());

        store.add(removed, NOW_MILLIS);
        store.remove(removed, NOW_MILLIS);

        Assertions.assertTrue(Files.readString(store.getFile(), StandardCharsets.UTF_8).contains(removedHex), "the removal is kept");
        Assertions.assertFalse(store.contains(removed));

        store.add(kept, NOW_MILLIS + PlayerListStore.REMOVED_ENTRY_MILLIS + SECOND_MILLIS);

        Assertions.assertFalse(Files.readString(store.getFile(), StandardCharsets.UTF_8).contains(removedHex), "an old removal is dropped");
        Assertions.assertEquals(List.of(kept), store.getPlayers());
    }

    @Test
    void open_should_list_no_player_for_a_damaged_file_and_the_next_change_writes_it_again(@TempDir @NotNull Path directory) throws IOException {
        Path file = directory.resolve(FILE_NAME);
        IdentityKey player = this.createKey();

        Files.writeString(file, "{\"format\": 1, \"players\": [{", StandardCharsets.UTF_8);

        PlayerListStore store = PlayerListStore.open(file, MAXIMUM_PLAYERS);

        Assertions.assertEquals(List.of(), store.getPlayers());

        store.add(player, NOW_MILLIS);

        Assertions.assertEquals(List.of(player), PlayerListStore.open(file, MAXIMUM_PLAYERS).getPlayers());
    }
}
