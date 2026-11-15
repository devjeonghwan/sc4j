package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Tests the sealed channel datagrams: keys derived on both sides open each other's datagrams, and tampered, truncated, replayed, too old and
 * foreign-key datagrams (another connection of the same account included), as well as invalid ephemeral keys, are refused.
 */
final class ChannelCipherTest {
    private static final long INITIATOR_NUMBER = 0x1122334455667788L;
    private static final long RESPONDER_NUMBER = 0x0102030405060708L;
    private static final long INITIATOR_CONNECTION = 0x0A0A0A0A0A0A0A0AL;
    private static final long RESPONDER_CONNECTION = 0x0B0B0B0B0B0B0B0BL;
    private static final long OTHER_CONNECTION = 0x0C0C0C0C0C0C0C0CL;
    private static final @NotNull SecureRandom RANDOM = new SecureRandom();

    private static @NotNull ChannelFrame createData(@NotNull String text) {
        return new ChannelFrame(ChannelFrameType.DATA, text.getBytes(StandardCharsets.UTF_8));
    }

    private static @NotNull String readText(@Nullable ChannelFrame frame) {
        Assertions.assertNotNull(frame, "the datagram opens");

        return new String(frame.readBody().readBytes(frame.getBodyLength()), StandardCharsets.UTF_8);
    }

    /**
     * Derives the ciphers of both sides of one channel: index 0 is the initiator's, index 1 the responder's.
     */
    private static @NotNull ChannelCipher @NotNull [] createPair() {
        IdentityKey initiator = IdentityKeyPair.generate(RANDOM).getPublicKey();
        IdentityKey responder = IdentityKeyPair.generate(RANDOM).getPublicKey();
        EphemeralKeyPair initiatorEphemeral = EphemeralKeyPair.generate(RANDOM);
        EphemeralKeyPair responderEphemeral = EphemeralKeyPair.generate(RANDOM);
        ChannelKeys initiatorKeys = ChannelKeys.derive(initiatorEphemeral, responderEphemeral.getPublicKey(), true, initiator, INITIATOR_CONNECTION,
                                                       responder, RESPONDER_CONNECTION, INITIATOR_NUMBER, RESPONDER_NUMBER);
        ChannelKeys responderKeys = ChannelKeys.derive(responderEphemeral, initiatorEphemeral.getPublicKey(), false, initiator, INITIATOR_CONNECTION,
                                                       responder, RESPONDER_CONNECTION, INITIATOR_NUMBER, RESPONDER_NUMBER);

        Assertions.assertNotNull(initiatorKeys);
        Assertions.assertNotNull(responderKeys);

        return new ChannelCipher[]{new ChannelCipher(initiatorKeys, RESPONDER_NUMBER), new ChannelCipher(responderKeys, INITIATOR_NUMBER)};
    }

    @Test
    void seal_and_open_round_trip_in_both_directions() {
        ChannelCipher[] pair = createPair();
        byte[] toResponder = pair[0].seal(createData("hello responder"));
        byte[] toInitiator = pair[1].seal(createData("hello initiator"));

        Assertions.assertNotNull(toResponder);
        Assertions.assertNotNull(toInitiator);
        Assertions.assertEquals(RESPONDER_NUMBER, ChannelDatagramUtils.readReceiveNumber(toResponder, 0));
        Assertions.assertTrue(ChannelDatagramUtils.isChannelDatagram(toResponder, 0, toResponder.length));
        Assertions.assertEquals("hello responder".length() + 1 + ChannelCipher.OVERHEAD, toResponder.length);

        ChannelFrame received = pair[1].open(toResponder, 0, toResponder.length);

        Assertions.assertNotNull(received);
        Assertions.assertEquals(ChannelFrameType.DATA, received.getType());
        Assertions.assertEquals("hello responder", readText(received));
        Assertions.assertEquals("hello initiator", readText(pair[0].open(toInitiator, 0, toInitiator.length)));
        Assertions.assertNull(pair[0].open(toResponder, 0, toResponder.length), "a side cannot open its own datagrams");
    }

    @Test
    void open_should_drop_tampered_truncated_replayed_and_foreign_key_datagrams() {
        ChannelCipher[] pair = createPair();
        ChannelCipher[] other = createPair();
        byte[] datagram = pair[0].seal(createData("payload"));

        Assertions.assertNotNull(datagram);

        for (int index = 0; index < datagram.length; index++) {
            byte[] tampered = datagram.clone();

            tampered[index] ^= 1;
            Assertions.assertNull(pair[1].open(tampered, 0, tampered.length), "a changed byte " + index + " opens");
        }

        for (int length = 0; length < datagram.length; length++) {
            Assertions.assertNull(pair[1].open(Arrays.copyOf(datagram, length), 0, length), "a datagram cut to " + length + " bytes opens");
        }

        Assertions.assertNull(other[1].open(datagram, 0, datagram.length), "another channel's key opens the datagram");
        Assertions.assertNotNull(pair[1].open(datagram, 0, datagram.length));
        Assertions.assertNull(pair[1].open(datagram, 0, datagram.length), "a replayed datagram opens again");
    }

    @Test
    void derive_should_give_other_keys_for_another_connection_of_the_same_account() {
        IdentityKey account = IdentityKeyPair.generate(RANDOM).getPublicKey();
        EphemeralKeyPair initiatorEphemeral = EphemeralKeyPair.generate(RANDOM);
        EphemeralKeyPair responderEphemeral = EphemeralKeyPair.generate(RANDOM);
        ChannelKeys initiatorKeys = ChannelKeys.derive(initiatorEphemeral, responderEphemeral.getPublicKey(), true, account, INITIATOR_CONNECTION,
                                                       account, RESPONDER_CONNECTION, INITIATOR_NUMBER, RESPONDER_NUMBER);
        ChannelKeys responderKeys = ChannelKeys.derive(responderEphemeral, initiatorEphemeral.getPublicKey(), false, account, INITIATOR_CONNECTION,
                                                       account, RESPONDER_CONNECTION, INITIATOR_NUMBER, RESPONDER_NUMBER);
        ChannelKeys otherKeys = ChannelKeys.derive(responderEphemeral, initiatorEphemeral.getPublicKey(), false, account, INITIATOR_CONNECTION,
                                                   account, OTHER_CONNECTION, INITIATOR_NUMBER, RESPONDER_NUMBER);

        Assertions.assertNotNull(initiatorKeys);
        Assertions.assertNotNull(responderKeys);
        Assertions.assertNotNull(otherKeys);

        byte[] datagram = new ChannelCipher(initiatorKeys, RESPONDER_NUMBER).seal(createData("to one device"));

        Assertions.assertNotNull(datagram);
        Assertions.assertNull(new ChannelCipher(otherKeys, INITIATOR_NUMBER).open(datagram, 0, datagram.length),
                              "keys bound to another connection of the account open the datagram");
        Assertions.assertEquals("to one device", readText(new ChannelCipher(responderKeys, INITIATOR_NUMBER).open(datagram, 0, datagram.length)));
    }

    @Test
    void open_should_accept_late_datagrams_inside_the_replay_window_only() {
        ChannelCipher[] pair = createPair();
        byte[] first = pair[0].seal(createData("first"));
        byte[] late = pair[0].seal(createData("late"));
        byte[] newest = null;

        for (int count = 0; count < ReplayWindow.SIZE - 1; count++) {
            newest = pair[0].seal(createData("next " + count));
        }

        Assertions.assertNotNull(first);
        Assertions.assertNotNull(late);
        Assertions.assertNotNull(newest);
        Assertions.assertNotNull(pair[1].open(newest, 0, newest.length));
        Assertions.assertTrue(pair[1].isLastOpenedNewest());
        Assertions.assertNull(pair[1].open(first, 0, first.length), "a datagram older than the window opens");
        Assertions.assertNotNull(pair[1].open(late, 0, late.length));
        Assertions.assertFalse(pair[1].isLastOpenedNewest());
        Assertions.assertNull(pair[1].open(late, 0, late.length), "a late datagram opens twice");
    }

    @Test
    void derive_should_refuse_an_invalid_or_low_order_ephemeral_key() {
        IdentityKey initiator = IdentityKeyPair.generate(RANDOM).getPublicKey();
        IdentityKey responder = IdentityKeyPair.generate(RANDOM).getPublicKey();
        EphemeralKeyPair own = EphemeralKeyPair.generate(RANDOM);
        byte[] lowOrder = new byte[EphemeralKeyPair.KEY_LENGTH];

        Assertions.assertNull(ChannelKeys.derive(own, lowOrder, true, initiator, INITIATOR_CONNECTION, responder, RESPONDER_CONNECTION,
                                                 INITIATOR_NUMBER, RESPONDER_NUMBER));

        lowOrder[0] = 1;

        Assertions.assertNull(ChannelKeys.derive(own, lowOrder, true, initiator, INITIATOR_CONNECTION, responder, RESPONDER_CONNECTION,
                                                 INITIATOR_NUMBER, RESPONDER_NUMBER));
        Assertions.assertNull(ChannelKeys.derive(own, new byte[EphemeralKeyPair.KEY_LENGTH - 1], true, initiator, INITIATOR_CONNECTION,
                                                 responder, RESPONDER_CONNECTION, INITIATOR_NUMBER, RESPONDER_NUMBER));
    }

    @Test
    void seal_should_refuse_a_frame_longer_than_a_datagram() {
        ChannelCipher cipher = createPair()[0];
        byte[] body = new byte[ChannelDatagramUtils.MAXIMUM_DATAGRAM_LENGTH - ChannelCipher.OVERHEAD];

        Assertions.assertThrows(IllegalArgumentException.class, () -> cipher.seal(new ChannelFrame(ChannelFrameType.DATA, body)));
        Assertions.assertNotNull(cipher.seal(new ChannelFrame(ChannelFrameType.DATA, Arrays.copyOf(body, body.length - 1))));
    }
}
