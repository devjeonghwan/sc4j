package org.fogofwarlabs.sc4j.beaconlink.protocol.file;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.List;

/**
 * Tests the text form of node list entries: host names, IPv4 and bracketed IPv6 addresses, the port range and the pinned key.
 */
final class ConfiguredNodeTest {
    @Test
    void parse_should_read_host_names_ipv4_and_bracketed_ipv6_addresses() {
        ConfiguredNode ipv6 = ConfiguredNode.parse(" [2001:db8::1]:7000 ", null);

        Assertions.assertEquals("2001:db8::1", ipv6.getHost());
        Assertions.assertEquals(7000, ipv6.getPort());
        Assertions.assertEquals("[2001:db8::1]:7000", ipv6.toString());
        Assertions.assertEquals(ConfiguredNode.parse("Beacon.Example.org:1", null), ConfiguredNode.parse("beacon.example.org:1", null));
        Assertions.assertEquals("192.0.2.5:65535", ConfiguredNode.of("192.0.2.5", 65535, null).toString());
    }

    @Test
    void parse_should_reject_invalid_addresses() {
        for (String text : List.of("host", "host:", ":7000", "host:0", "host:65536", "host:7a", "host:+7", "2001:db8::1:7000", "[2001:db8::1:7000",
                                   "[]:7000", "two words:7000")) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> ConfiguredNode.parse(text, null), text);
        }
    }

    @Test
    void equals_should_compare_the_pinned_key_and_has_same_address_should_not() {
        IdentityKey key = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();
        ConfiguredNode pinned = ConfiguredNode.of("beacon.example.org", 7455, key);
        ConfiguredNode plain = ConfiguredNode.of("BEACON.example.org", 7455, null);

        Assertions.assertNotEquals(pinned, plain);
        Assertions.assertTrue(pinned.hasSameAddress(plain));
        Assertions.assertEquals(pinned, ConfiguredNode.parse("beacon.example.org:7455", key));
        Assertions.assertEquals(pinned.hashCode(), ConfiguredNode.parse("Beacon.Example.Org:7455", key).hashCode());
        Assertions.assertFalse(pinned.hasSameAddress(ConfiguredNode.of("beacon.example.org", 7456, key)));
    }

    @Test
    void parse_port_should_accept_decimal_digits_in_range_only() {
        Assertions.assertEquals(1, ConfiguredNode.parsePort("1"));
        Assertions.assertEquals(65535, ConfiguredNode.parsePort("65535"));

        for (String text : List.of("", "0", "65536", "-1", "+1", "7 0", "99999999999")) {
            Assertions.assertEquals(-1, ConfiguredNode.parsePort(text), text);
        }
    }
}
