package org.fogofwarlabs.sc4j.beaconlink.peer.limit;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;

/**
 * Tests the address tokens: a token is valid only for its address, port and key, in its time bucket and the next, and only under the secret
 * that created it.
 */
final class AddressTokensTest {
    private static final long NOW_MILLIS = 1_800_000_000_000L;
    private static final int PORT = 40500;

    @Test
    void token_is_valid_only_for_its_address_key_secret_and_two_time_buckets() {
        SecureRandom random = new SecureRandom();
        byte[] secret = new byte[AddressTokens.SECRET_LENGTH];
        byte[] otherSecret = new byte[AddressTokens.SECRET_LENGTH];

        random.nextBytes(secret);
        random.nextBytes(otherSecret);

        AddressTokens tokens = new AddressTokens(secret);
        IdentityKey key = IdentityKeyPair.generate(random).getPublicKey();
        IdentityKey otherKey = IdentityKeyPair.generate(random).getPublicKey();
        InetSocketAddress address = new InetSocketAddress(InetAddress.ofLiteral("127.0.0.1"), PORT);
        InetSocketAddress otherPort = new InetSocketAddress(InetAddress.ofLiteral("127.0.0.1"), PORT + 1);
        InetSocketAddress otherHost = new InetSocketAddress(InetAddress.ofLiteral("127.0.0.2"), PORT);
        byte[] token = tokens.create(address, key, NOW_MILLIS);

        Assertions.assertEquals(AddressTokens.TOKEN_LENGTH, token.length);
        Assertions.assertTrue(tokens.isValid(token, address, key, NOW_MILLIS));
        Assertions.assertTrue(tokens.isValid(token, address, key, NOW_MILLIS + AddressTokens.BUCKET_MILLIS));
        Assertions.assertFalse(tokens.isValid(token, address, key, NOW_MILLIS + 2 * AddressTokens.BUCKET_MILLIS));
        Assertions.assertFalse(tokens.isValid(token, otherPort, key, NOW_MILLIS));
        Assertions.assertFalse(tokens.isValid(token, otherHost, key, NOW_MILLIS));
        Assertions.assertFalse(tokens.isValid(token, address, otherKey, NOW_MILLIS));
        Assertions.assertFalse(new AddressTokens(otherSecret).isValid(token, address, key, NOW_MILLIS));
        Assertions.assertFalse(tokens.isValid(new byte[0], address, key, NOW_MILLIS));
    }
}
