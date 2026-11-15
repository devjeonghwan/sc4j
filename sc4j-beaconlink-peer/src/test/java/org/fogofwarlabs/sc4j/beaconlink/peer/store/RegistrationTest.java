package org.fogofwarlabs.sc4j.beaconlink.peer.store;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.ModFingerprint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;

/**
 * Tests a client registration: the values it holds and when it expires.
 */
final class RegistrationTest {
    private static final int PORT = 7455;
    private static final long CONNECTION = 3L;
    private static final long CONNECTION_START_MILLIS = 1_800_000_000_000L;
    private static final long EXPIRY_MILLIS = CONNECTION_START_MILLIS + 120_000L;

    @Test
    void registration_keeps_its_values_and_expires_at_its_expiry() {
        IdentityKey key = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();
        InetSocketAddress address = new InetSocketAddress(InetAddress.ofLiteral("127.0.0.1"), PORT);
        ModFingerprint fingerprint = ModFingerprint.of(new byte[]{1, 2});
        Registration registration = new Registration(key, address, CONNECTION, CONNECTION_START_MILLIS, "Alice", fingerprint, EXPIRY_MILLIS);

        Assertions.assertEquals(key, registration.getKey());
        Assertions.assertEquals(address, registration.getAddress());
        Assertions.assertEquals(CONNECTION, registration.getConnection());
        Assertions.assertEquals(CONNECTION_START_MILLIS, registration.getConnectionStartMillis());
        Assertions.assertEquals("Alice", registration.getDisplayName());
        Assertions.assertEquals(fingerprint, registration.getFingerprint());
        Assertions.assertEquals(EXPIRY_MILLIS, registration.getExpiryMillis());
        Assertions.assertFalse(registration.isExpired(EXPIRY_MILLIS - 1L));
        Assertions.assertTrue(registration.isExpired(EXPIRY_MILLIS));
    }
}
