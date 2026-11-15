package org.fogofwarlabs.sc4j.beaconlink.protocol.topic;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

/**
 * Tests the topic keys of channels and players: channel names that differ in case or white space only name one channel, invalid names are
 * refused, and the kinds keep equal names apart.
 */
final class TopicKeyTest {
    @Test
    void of_channel_should_name_one_channel_for_names_that_differ_in_case_and_white_space() {
        Assertions.assertEquals(TopicKey.ofChannel("Op  Lobby"), TopicKey.ofChannel(" op lobby\t"));
        Assertions.assertEquals("Op Lobby", TopicKey.cleanChannelName("  Op \t Lobby "));
        Assertions.assertEquals(TopicKind.CHANNEL, TopicKey.ofChannel("SC4J").getKind());
        Assertions.assertNotEquals(TopicKey.ofChannel("sc4j"), TopicKey.of(TopicKind.MOD, "sc4j"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TopicKey.ofChannel("   "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TopicKey.ofChannel("n".repeat(TopicKey.MAXIMUM_CHANNEL_NAME_LENGTH + 1)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TopicKey.ofChannel("bell\u0007"));
    }

    @Test
    void of_player_should_give_every_player_its_own_topic() {
        IdentityKeyPair first = IdentityKeyPair.generate(new SecureRandom());
        IdentityKeyPair second = IdentityKeyPair.generate(new SecureRandom());

        Assertions.assertEquals(TopicKey.ofPlayer(first.getPublicKey()), TopicKey.ofPlayer(first.getPublicKey()));
        Assertions.assertNotEquals(TopicKey.ofPlayer(first.getPublicKey()), TopicKey.ofPlayer(second.getPublicKey()));
        Assertions.assertEquals(TopicKind.PLAYER, TopicKey.ofPlayer(first.getPublicKey()).getKind());
    }
}
