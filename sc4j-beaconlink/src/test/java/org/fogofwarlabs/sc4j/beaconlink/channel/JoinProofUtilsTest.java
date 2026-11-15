package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

/**
 * Tests the password proof of a private room's join: the host accepts the proof of its password from the offer's ephemeral key and refuses
 * another password, another joiner, another room, another offer's key and a missing proof.
 */
final class JoinProofUtilsTest {
    private static final long ROOM_NUMBER = 42L;
    private static final @NotNull String PASSWORD = "open sesame";

    @Test
    void verify_should_accept_only_the_proof_of_the_room_password_by_its_joiner() {
        SecureRandom random = new SecureRandom();
        EphemeralKeyPair joinKey = EphemeralKeyPair.generate(random);
        EphemeralKeyPair offerKey = EphemeralKeyPair.generate(random);
        IdentityKey host = IdentityKeyPair.generate(random).getPublicKey();
        IdentityKey joiner = IdentityKeyPair.generate(random).getPublicKey();
        IdentityKey stranger = IdentityKeyPair.generate(random).getPublicKey();
        byte[] proof = JoinProofUtils.create(offerKey, joinKey.getPublicKey(), host, ROOM_NUMBER, joiner, PASSWORD);
        byte[] guess = JoinProofUtils.create(offerKey, joinKey.getPublicKey(), host, ROOM_NUMBER, joiner, "open barley");

        Assertions.assertNotNull(proof);
        Assertions.assertEquals(JoinProofUtils.PROOF_LENGTH, proof.length);
        Assertions.assertTrue(JoinProofUtils.verify(joinKey, offerKey.getPublicKey(), host, ROOM_NUMBER, joiner, PASSWORD, proof));
        Assertions.assertFalse(JoinProofUtils.verify(joinKey, offerKey.getPublicKey(), host, ROOM_NUMBER, joiner, PASSWORD, guess));
        Assertions.assertFalse(JoinProofUtils.verify(joinKey, offerKey.getPublicKey(), host, ROOM_NUMBER, stranger, PASSWORD, proof));
        Assertions.assertFalse(JoinProofUtils.verify(joinKey, offerKey.getPublicKey(), host, ROOM_NUMBER + 1, joiner, PASSWORD, proof));
        Assertions.assertFalse(JoinProofUtils.verify(joinKey, EphemeralKeyPair.generate(random).getPublicKey(), host, ROOM_NUMBER, joiner,
                                                     PASSWORD, proof));
        Assertions.assertFalse(JoinProofUtils.verify(joinKey, offerKey.getPublicKey(), host, ROOM_NUMBER, joiner, PASSWORD, null));
        Assertions.assertNull(JoinProofUtils.create(offerKey, new byte[1], host, ROOM_NUMBER, joiner, PASSWORD));
    }
}
