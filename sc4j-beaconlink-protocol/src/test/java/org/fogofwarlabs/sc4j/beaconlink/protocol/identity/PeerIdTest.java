package org.fogofwarlabs.sc4j.beaconlink.protocol.identity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.SplittableRandom;

/**
 * Tests the peer ID: a key solves its smallest valid proof nonce, the ID follows from the key and the nonce, and the XOR distance, the
 * common prefix and the random IDs of a bucket behave as the k-buckets need.
 */
final class PeerIdTest {
    private static final int DIFFICULTY = 12;
    private static final long RANDOM_SEED = 20261109L;
    private static final int BUCKET_SAMPLES = 64;

    @Test
    void solve_proof_should_find_the_smallest_valid_nonce_of_its_key() {
        SecureRandom random = new SecureRandom();
        IdentityKey key = IdentityKeyPair.generate(random).getPublicKey();
        IdentityKey other = IdentityKeyPair.generate(random).getPublicKey();
        long nonce = PeerId.solveProof(key, DIFFICULTY);

        Assertions.assertTrue(PeerId.isProofValid(key, nonce, DIFFICULTY));
        Assertions.assertEquals(nonce, PeerId.solveProof(key, DIFFICULTY), "the same key finds the same nonce again");

        for (long candidate = 0L; candidate < nonce; candidate++) {
            Assertions.assertFalse(PeerId.isProofValid(key, candidate, DIFFICULTY), "the smallest valid nonce");
        }

        Assertions.assertTrue(PeerId.isProofValid(key, nonce, 0));
        Assertions.assertEquals(PeerId.of(key, nonce), PeerId.of(key, nonce));
        Assertions.assertNotEquals(PeerId.of(key, nonce), PeerId.of(other, nonce));
        Assertions.assertNotEquals(PeerId.of(key, nonce), PeerId.of(key, nonce + 1L));
    }

    @Test
    void compare_distance_and_common_prefix_follow_the_xor_metric() {
        byte[] zero = new byte[PeerId.LENGTH];
        byte[] near = new byte[PeerId.LENGTH];
        byte[] far = new byte[PeerId.LENGTH];

        near[PeerId.LENGTH - 1] = 1;
        far[0]                  = (byte) 0x80;

        PeerId origin = PeerId.of(zero);

        Assertions.assertTrue(origin.compareDistance(PeerId.of(near), PeerId.of(far)) < 0);
        Assertions.assertTrue(origin.compareDistance(PeerId.of(far), PeerId.of(near)) > 0);
        Assertions.assertEquals(0, origin.compareDistance(PeerId.of(near), PeerId.of(near)));
        Assertions.assertEquals(PeerId.BIT_LENGTH - 1, origin.getCommonPrefixLength(PeerId.of(near)));
        Assertions.assertEquals(0, origin.getCommonPrefixLength(PeerId.of(far)));
        Assertions.assertEquals(PeerId.BIT_LENGTH, origin.getCommonPrefixLength(origin));
    }

    @Test
    void create_random_in_bucket_should_share_exactly_the_prefix_of_the_bucket() {
        SplittableRandom random = new SplittableRandom(RANDOM_SEED);
        PeerId own = PeerId.createRandom(random);

        for (int sample = 0; sample < BUCKET_SAMPLES; sample++) {
            int prefixLength = random.nextInt(PeerId.BIT_LENGTH);

            Assertions.assertEquals(prefixLength, own.getCommonPrefixLength(own.createRandomInBucket(prefixLength, random)));
        }

        Assertions.assertThrows(IllegalArgumentException.class, () -> own.createRandomInBucket(PeerId.BIT_LENGTH, random));
    }
}
