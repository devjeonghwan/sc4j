package org.fogofwarlabs.sc4j.beaconlink.peer.account;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerNameUtils;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerStamp;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;

/**
 * Tests the accounts of an install: nothing exists before an account is created, an account keeps its key, name, stamp and friends in its own
 * folder named by its id, the list shows the most recently used first, the name is fixed by the stamp (a changed name makes the folder no
 * account), a key is added once, a name is checked, an account folder is read wherever it lies, and any number of users may use an account
 * at once while it cannot be deleted.
 */
final class AccountStoreTest {
    private static final int STAMP_DIFFICULTY = 16;
    private static final long FIRST_USE_MILLIS = 1_000L;
    private static final long SECOND_USE_MILLIS = 2_000L;
    private static final long THIRD_USE_MILLIS = 3_000L;

    private static @NotNull Account create(@NotNull AccountStore store, @NotNull IdentityKeyPair keyPair, @NotNull String name, long nowMillis) throws IOException {
        return store.create(keyPair, PlayerStamp.solve(keyPair.getPublicKey(), name, STAMP_DIFFICULTY), STAMP_DIFFICULTY, nowMillis);
    }

    @Test
    void create_should_write_one_folder_per_account_named_by_its_id(@TempDir @NotNull Path directory) throws IOException {
        AccountStore store = AccountStore.of(directory);
        IdentityKeyPair keyPair = IdentityKeyPair.generate(new SecureRandom());

        Assertions.assertEquals(List.of(), store.list());
        Assertions.assertFalse(Files.exists(store.getDirectory()), "no account is made on its own");

        Account account = create(store, keyPair, "Alice", FIRST_USE_MILLIS);
        IdentityKey friend = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();

        Assertions.assertEquals("Alice", account.getName());
        Assertions.assertEquals(keyPair.getPublicKey().getTag(), account.getTag());
        Assertions.assertEquals("Alice#" + account.getTag(), account.getTaggedName());
        Assertions.assertTrue(account.getId().startsWith(account.getTag()), "the tag starts the id");
        Assertions.assertEquals(store.getDirectory().resolve(account.getId()), account.getDirectory());
        Assertions.assertEquals(keyPair.getPublicKey(), account.readKeyPair().getPublicKey());
        Assertions.assertEquals("Alice", account.getStamp().getName());
        Assertions.assertTrue(account.getStamp().isValid(STAMP_DIFFICULTY));

        account.openFriends().add(friend, FIRST_USE_MILLIS);

        Account found = store.find(account.getId());

        Assertions.assertNotNull(found);
        Assertions.assertEquals("Alice", found.getName());
        Assertions.assertEquals(FIRST_USE_MILLIS, found.getLastUsedMillis());
        Assertions.assertEquals(List.of(friend), found.openFriends().getPlayers());
        Assertions.assertEquals(account.getStamp(), found.getStamp());
        Assertions.assertEquals(STAMP_DIFFICULTY, found.getStampDifficulty());
        Assertions.assertNull(store.find("0000000000000000"));
        Assertions.assertNull(store.find(".."), "an id never leaves the accounts folder");

        AccountException again = Assertions.assertThrows(AccountException.class, () -> create(store, keyPair, "Bob", FIRST_USE_MILLIS));

        Assertions.assertEquals(AccountProblem.ALREADY_ADDED, again.getProblem());
    }

    @Test
    void list_should_show_the_most_recently_used_account_first(@TempDir @NotNull Path directory) throws IOException {
        AccountStore store = AccountStore.of(directory);
        IdentityKeyPair firstKey = IdentityKeyPair.generate(new SecureRandom());
        IdentityKeyPair secondKey = IdentityKeyPair.generate(new SecureRandom());
        Account first = create(store, firstKey, "First", FIRST_USE_MILLIS);
        Account second = create(store, secondKey, "Second", SECOND_USE_MILLIS);

        Assertions.assertEquals(List.of(second.getId(), first.getId()), store.list().stream().map(Account::getId).toList());

        store.markUsed(first, THIRD_USE_MILLIS);

        Assertions.assertEquals(List.of(first.getId(), second.getId()), store.list().stream().map(Account::getId).toList());

        Files.createDirectories(store.getDirectory().resolve("not-an-account"));
        Files.writeString(store.getDirectory().resolve(first.getId()).resolve(Account.ACCOUNT_FILE), "broken", StandardCharsets.UTF_8);

        Assertions.assertEquals(List.of(second.getId()), store.list().stream().map(Account::getId).toList(), "unreadable folders are left out");
    }

    @Test
    void read_should_refuse_an_account_whose_name_was_changed_in_its_file(@TempDir @NotNull Path directory) throws IOException {
        AccountStore store = AccountStore.of(directory);
        IdentityKeyPair keyPair = IdentityKeyPair.generate(new SecureRandom());
        Account account = create(store, keyPair, "Alice", FIRST_USE_MILLIS);
        Path accountFile = account.getDirectory().resolve(Account.ACCOUNT_FILE);
        String text = Files.readString(accountFile, StandardCharsets.UTF_8);

        Assertions.assertEquals("Alice", store.list().getFirst().getName());

        Files.writeString(accountFile, text.replace("\"Alice\"", "\"Taylor\""), StandardCharsets.UTF_8);

        Assertions.assertEquals(List.of(), store.list(), "a name the stamp does not bind makes the folder no account");
        Assertions.assertThrows(IOException.class, () -> store.find(account.getId()));
    }

    @Test
    void create_should_refuse_a_name_outside_the_rules(@TempDir @NotNull Path directory) {
        AccountStore store = AccountStore.of(directory);
        IdentityKeyPair keyPair = IdentityKeyPair.generate(new SecureRandom());

        for (String name : List.of("", "   ", "with#tag", "a".repeat(PlayerNameUtils.MAXIMUM_LENGTH + 1), " Alice")) {
            PlayerStamp stamp = new PlayerStamp(keyPair.getPublicKey(), name, 0L);
            AccountException refused = Assertions.assertThrows(AccountException.class, () -> store.create(keyPair, stamp, 0, FIRST_USE_MILLIS), name);

            Assertions.assertEquals(AccountProblem.NAME_INVALID, refused.getProblem());
        }

        Assertions.assertFalse(Files.exists(store.getDirectory()));
    }

    @Test
    void delete_should_remove_the_account_unless_a_process_uses_it(@TempDir @NotNull Path directory) throws IOException {
        AccountStore store = AccountStore.of(directory);
        IdentityKeyPair keyPair = IdentityKeyPair.generate(new SecureRandom());
        Account account = create(store, keyPair, "Alice", FIRST_USE_MILLIS);

        try (AccountLock lock = AccountLock.acquire(account)) {
            Assertions.assertEquals(account.getId(), lock.getAccount().getId());
            Assertions.assertTrue(store.isInUse(account));

            AccountException refused = Assertions.assertThrows(AccountException.class, () -> store.delete(account));

            Assertions.assertEquals(AccountProblem.IN_USE, refused.getProblem());
            Assertions.assertTrue(Files.isDirectory(account.getDirectory()));
        }

        Assertions.assertFalse(store.isInUse(account));

        store.delete(account);

        Assertions.assertFalse(Files.exists(account.getDirectory()));
        Assertions.assertEquals(List.of(), store.list());
    }

    @Test
    void acquire_should_let_every_user_use_one_account_until_the_last_closes(@TempDir @NotNull Path directory) throws IOException {
        AccountStore store = AccountStore.of(directory);
        Account account = create(store, IdentityKeyPair.generate(new SecureRandom()), "Alice", FIRST_USE_MILLIS);
        AccountLock first = AccountLock.acquire(account);
        AccountLock second = AccountLock.acquire(account);

        first.close();
        first.close();

        Assertions.assertTrue(store.isInUse(account), "the second user still uses the account");
        Assertions.assertThrows(AccountException.class, () -> store.delete(account));

        second.close();

        Assertions.assertFalse(store.isInUse(account));

        try (AccountLock again = AccountLock.acquire(account)) {
            Assertions.assertEquals(account.getId(), again.getAccount().getId());
            Assertions.assertTrue(store.isInUse(account), "a released account can be used again");
        }
    }

    @Test
    void read_should_read_an_account_folder_wherever_it_lies(@TempDir @NotNull Path directory) throws IOException {
        Account account = create(AccountStore.of(directory.resolve("install")), IdentityKeyPair.generate(new SecureRandom()),
                                 "Alice", FIRST_USE_MILLIS);
        Path moved = Files.move(account.getDirectory(), Files.createDirectories(directory.resolve("elsewhere")).resolve(account.getId()));
        Account read = AccountStore.read(moved);

        Assertions.assertEquals(account.getId(), read.getId());
        Assertions.assertEquals(account.getTaggedName(), read.getTaggedName());
        Assertions.assertEquals(moved, read.getDirectory());
        Assertions.assertThrows(IOException.class, () -> AccountStore.read(directory.resolve("elsewhere")), "a folder that holds no account");
    }
}
