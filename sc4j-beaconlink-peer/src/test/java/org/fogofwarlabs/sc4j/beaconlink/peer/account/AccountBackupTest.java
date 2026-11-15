package org.fogofwarlabs.sc4j.beaconlink.peer.account;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyFileUtils;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
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
 * Tests the account backup file: an export restores on another install with its name, key, stamp, friends and ignored players, with and
 * without a password; a protected backup needs its password and refuses a wrong one or a changed byte; a backup whose name was changed, a key
 * file alone and other files are refused.
 */
final class AccountBackupTest {
    private static final @NotNull String PASSWORD = "correct horse";
    private static final int STAMP_DIFFICULTY = 16;

    private static @NotNull Account createAccount(@NotNull Path directory) throws IOException {
        IdentityKeyPair keyPair = IdentityKeyPair.generate(new SecureRandom());
        Account account = AccountStore.of(directory).create(keyPair, PlayerStamp.solve(keyPair.getPublicKey(), "Alice", STAMP_DIFFICULTY),
                                                            STAMP_DIFFICULTY, 1L);
        IdentityKey friend = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();
        IdentityKey blocked = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();

        account.openFriends().add(friend, 1L);
        account.openIgnored().add(blocked, 1L);

        return account;
    }

    private static void assertRestored(@NotNull Account exported, @NotNull Path otherInstall, @NotNull AccountBackup backup) throws IOException {
        AccountStore other = AccountStore.of(otherInstall);

        Assertions.assertEquals("Alice", backup.getName());
        Assertions.assertEquals(exported.getId(), backup.getId());
        Assertions.assertEquals(exported.getStamp(), backup.getStamp(), "the backup carries the stamp, so that no new one is needed");
        Assertions.assertEquals(STAMP_DIFFICULTY, backup.getStampDifficulty());

        Account restored = other.restore(backup, 2L);

        Assertions.assertEquals("Alice", restored.getName(), "the import keeps the name");
        Assertions.assertEquals(exported.getTaggedName(), restored.getTaggedName());
        Assertions.assertEquals(exported.getKey(), restored.readKeyPair().getPublicKey());
        Assertions.assertEquals(exported.getStamp(), restored.getStamp());
        Assertions.assertEquals(exported.getStamp(), other.list().getFirst().getStamp());
        Assertions.assertEquals(exported.openFriends().getPlayers(), restored.openFriends().getPlayers());
        Assertions.assertEquals(exported.openIgnored().getPlayers(), restored.openIgnored().getPlayers());
    }

    private static int indexOf(byte @NotNull [] data, byte @NotNull [] part) {
        for (int index = 0; index + part.length <= data.length; index++) {
            boolean found = true;

            for (int offset = 0; offset < part.length && found; offset++) {
                found = data[index + offset] == part[offset];
            }

            if (found) {
                return index;
            }
        }

        return -1;
    }

    @Test
    void create_and_read_should_carry_an_account_without_a_password(@TempDir @NotNull Path directory) throws IOException {
        Account account = createAccount(directory.resolve("first"));
        byte[] backup = AccountBackup.create(account, null, new SecureRandom());

        Assertions.assertFalse(AccountBackup.isProtected(backup));
        assertRestored(account, directory.resolve("second"), AccountBackup.read(backup, null));
    }

    @Test
    void create_and_read_should_carry_an_account_protected_by_a_password(@TempDir @NotNull Path directory) throws IOException {
        Account account = createAccount(directory.resolve("first"));
        byte[] backup = AccountBackup.create(account, PASSWORD.toCharArray(), new SecureRandom());

        Assertions.assertTrue(AccountBackup.isProtected(backup));
        Assertions.assertEquals(-1, indexOf(backup, "Alice".getBytes(StandardCharsets.UTF_8)), "the name is encrypted with the rest");
        Assertions.assertEquals(AccountProblem.PASSWORD_REQUIRED,
                                Assertions.assertThrows(AccountException.class, () -> AccountBackup.read(backup, null)).getProblem());
        Assertions.assertEquals(AccountProblem.WRONG_PASSWORD,
                                Assertions.assertThrows(AccountException.class,
                                                        () -> AccountBackup.read(backup, "wrong".toCharArray())).getProblem());

        byte[] changed = backup.clone();

        changed[changed.length - 1] ^= 1;

        Assertions.assertEquals(AccountProblem.WRONG_PASSWORD,
                                Assertions.assertThrows(AccountException.class,
                                                        () -> AccountBackup.read(changed, PASSWORD.toCharArray())).getProblem(),
                                "a changed byte is detected");

        assertRestored(account, directory.resolve("second"), AccountBackup.read(backup, PASSWORD.toCharArray()));
    }

    @Test
    void read_should_refuse_a_changed_name(@TempDir @NotNull Path directory) throws IOException {
        Account account = createAccount(directory.resolve("first"));
        byte[] backup = AccountBackup.create(account, null, new SecureRandom());
        int name = indexOf(backup, "Alice".getBytes(StandardCharsets.UTF_8));
        byte[] changed = backup.clone();

        changed[name + "Alice".length() - 1] = 'a';

        Assertions.assertEquals(AccountProblem.NOT_AN_ACCOUNT_FILE,
                                Assertions.assertThrows(AccountException.class, () -> AccountBackup.read(changed, null)).getProblem(),
                                "a name the stamp does not bind is refused");
        Assertions.assertEquals("Alice", AccountBackup.read(backup, null).getName());
    }

    @Test
    void read_should_refuse_a_key_file_and_other_files(@TempDir @NotNull Path directory) throws IOException {
        IdentityKeyPair keyPair = IdentityKeyPair.generate(new SecureRandom());
        Path keyFile = directory.resolve("identity.key");

        IdentityKeyFileUtils.write(keyFile, keyPair);

        for (byte[] other : List.of(Files.readAllBytes(keyFile), new byte[0], "not an account".getBytes(StandardCharsets.UTF_8),
                                    new byte[]{'B', 'L', 'K', 'A', 2, 0}, new byte[]{'B', 'L', 'K', 'A', 2, 0, 5, 'A'})) {
            Assertions.assertEquals(AccountProblem.NOT_AN_ACCOUNT_FILE,
                                    Assertions.assertThrows(AccountException.class, () -> AccountBackup.read(other, null)).getProblem());
        }
    }
}
