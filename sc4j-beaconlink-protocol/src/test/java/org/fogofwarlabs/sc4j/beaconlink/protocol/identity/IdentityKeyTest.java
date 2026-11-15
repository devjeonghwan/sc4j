package org.fogofwarlabs.sc4j.beaconlink.protocol.identity;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Tests the identity key: signatures of a key pair verify only with the owner's key and unchanged bytes, and bytes that are no key verify
 * nothing.
 */
final class IdentityKeyTest {
    private static final byte @NotNull [] MESSAGE = "room".getBytes(StandardCharsets.US_ASCII);

    @Test
    void signatures_verify_only_with_the_owner_key_and_unchanged_bytes() {
        SecureRandom random = new SecureRandom();
        IdentityKeyPair owner = IdentityKeyPair.generate(random);
        IdentityKeyPair other = IdentityKeyPair.generate(random);
        byte[] signature = owner.sign(MESSAGE, 0, MESSAGE.length);
        byte[] changed = MESSAGE.clone();

        changed[0] ^= 1;

        Assertions.assertTrue(owner.getPublicKey().verify(MESSAGE, 0, MESSAGE.length, signature, 0));
        Assertions.assertFalse(other.getPublicKey().verify(MESSAGE, 0, MESSAGE.length, signature, 0));
        Assertions.assertFalse(owner.getPublicKey().verify(changed, 0, changed.length, signature, 0));
        Assertions.assertEquals(owner.getPublicKey(), IdentityKey.of(owner.getPublicKey().getBytes()));
    }

    @Test
    void verify_should_return_false_for_bytes_that_are_no_key() {
        byte[] notAPoint = new byte[IdentityKey.LENGTH];

        notAPoint[0] = 2;

        IdentityKey key = IdentityKey.of(notAPoint);

        Assertions.assertFalse(key.verify(MESSAGE, 0, MESSAGE.length, new byte[IdentityKey.SIGNATURE_LENGTH], 0));
    }

    @Test
    void of_should_throw_for_a_wrong_length() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> IdentityKey.of(new byte[IdentityKey.LENGTH - 1]));
    }
}
