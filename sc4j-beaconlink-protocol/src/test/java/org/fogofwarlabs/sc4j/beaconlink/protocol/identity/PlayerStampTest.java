package org.fogofwarlabs.sc4j.beaconlink.protocol.identity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Tests the player stamp: a solved stamp is valid for its key, its name and its difficulty and the same at every solve, and it is no stamp of
 * another key or another name.
 */
final class PlayerStampTest {
    private static final int DIFFICULTY = 12;
    private static final long KEY_SEED = 20261111L;

    @Test
    void solve_should_find_the_same_stamp_valid_for_its_key_and_name_only() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        random.setSeed(KEY_SEED);

        IdentityKey key = IdentityKeyPair.generate(random).getPublicKey();
        IdentityKey other = IdentityKeyPair.generate(random).getPublicKey();
        PlayerStamp stamp = PlayerStamp.solve(key, "Morgan", DIFFICULTY);

        Assertions.assertTrue(stamp.isValid(DIFFICULTY));
        Assertions.assertEquals(stamp, PlayerStamp.solve(key, "Morgan", DIFFICULTY));
        Assertions.assertFalse(new PlayerStamp(other, "Morgan", stamp.getNonce()).isValid(DIFFICULTY));
        Assertions.assertThrows(IllegalArgumentException.class, () -> PlayerStamp.solve(key, "Morgan", -1));
    }

    @Test
    void is_valid_should_fail_for_another_name_of_the_same_key() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        random.setSeed(KEY_SEED);

        IdentityKey key = IdentityKeyPair.generate(random).getPublicKey();
        PlayerStamp stamp = PlayerStamp.solve(key, "Morgan", DIFFICULTY);

        for (String name : new String[]{"Taylor", "morgan", "Morgan ", "Morgan2", "Morgan#1"}) {
            Assertions.assertFalse(new PlayerStamp(key, name, stamp.getNonce()).isValid(DIFFICULTY), name);
        }

        Assertions.assertThrows(IllegalArgumentException.class, () -> PlayerStamp.solve(key, " Morgan", DIFFICULTY));
        Assertions.assertThrows(IllegalArgumentException.class, () -> PlayerStamp.solve(key, "", DIFFICULTY));
    }

    @Test
    void is_valid_should_refuse_an_invalid_name_at_any_difficulty() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        random.setSeed(KEY_SEED);

        IdentityKey key = IdentityKeyPair.generate(random).getPublicKey();

        Assertions.assertTrue(new PlayerStamp(key, "Morgan", 0L).isValid(0));
        Assertions.assertFalse(new PlayerStamp(key, "with#tag", 0L).isValid(0));
        Assertions.assertFalse(new PlayerStamp(key, " Morgan", 0L).isValid(0));
    }
}
