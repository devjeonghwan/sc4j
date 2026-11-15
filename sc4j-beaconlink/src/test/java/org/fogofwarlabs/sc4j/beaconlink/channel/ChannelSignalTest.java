package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.fogofwarlabs.sc4j.beaconlink.protocol.message.SignalMessage;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.ModFingerprint;
import org.fogofwarlabs.sc4j.engine.net.lobby.JoinRefusal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Tests the signal payloads that open a channel: offers (with and without a join), answers and refusals read back field by field, the
 * largest payload fits a node signal, and malformed payloads are refused without an exception.
 */
final class ChannelSignalTest {
    private static final long RANDOM_SEED = 20261108L;
    private static final int RANDOM_CHANGES = 2000;
    private static final long SENDER_CONNECTION = 0x5E5E5E5E5E5E5E5EL;
    private static final @NotNull List<@NotNull InetSocketAddress> CANDIDATES = List.of(
            new InetSocketAddress(InetAddress.ofLiteral("127.0.0.1"), 40_001),
            new InetSocketAddress(InetAddress.ofLiteral("::1"), 40_002)
    );

    private static @NotNull List<@NotNull InetSocketAddress> createLargestCandidates() {
        List<InetSocketAddress> candidates = new ArrayList<>();

        for (int index = 0; index < ChannelSignal.MAXIMUM_CANDIDATES; index++) {
            candidates.add(new InetSocketAddress(InetAddress.ofLiteral("2001:db8::" + (index + 1)), 50_000 + index));
        }

        return candidates;
    }

    @Test
    void offer_and_answer_read_back() {
        byte[] key = EphemeralKeyPair.generate(new SecureRandom()).getPublicKey();
        ChannelSignal offer = ChannelSignal.read(new ChannelOffer(SENDER_CONNECTION, 7L, key, CANDIDATES).write());
        ChannelSignal answer = ChannelSignal.read(new ChannelAnswer(SENDER_CONNECTION, 7L, 9L, key, CANDIDATES).write());

        Assertions.assertInstanceOf(ChannelOffer.class, offer);
        Assertions.assertInstanceOf(ChannelAnswer.class, answer);
        Assertions.assertEquals(7L, ((ChannelOffer) offer).getReceiveNumber());
        Assertions.assertArrayEquals(key, ((ChannelOffer) offer).getEphemeralKey());
        Assertions.assertEquals(CANDIDATES, ((ChannelOffer) offer).getCandidates());
        Assertions.assertEquals(7L, ((ChannelAnswer) answer).getOfferNumber());
        Assertions.assertEquals(9L, ((ChannelAnswer) answer).getReceiveNumber());
        Assertions.assertArrayEquals(key, ((ChannelAnswer) answer).getEphemeralKey());
        Assertions.assertEquals(CANDIDATES, ((ChannelAnswer) answer).getCandidates());
        Assertions.assertEquals(SENDER_CONNECTION, offer.getSenderConnection());
        Assertions.assertEquals(SENDER_CONNECTION, answer.getSenderConnection());
    }

    @Test
    void largest_answer_fits_a_node_signal() {
        byte[] payload = new ChannelAnswer(SENDER_CONNECTION, 1L, 2L, new byte[EphemeralKeyPair.KEY_LENGTH], createLargestCandidates()).write();

        Assertions.assertTrue(payload.length <= SignalMessage.MAXIMUM_PAYLOAD_LENGTH);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            List<InetSocketAddress> tooMany = new ArrayList<>(createLargestCandidates());

            tooMany.add(CANDIDATES.getFirst());
            new ChannelOffer(SENDER_CONNECTION, 1L, new byte[EphemeralKeyPair.KEY_LENGTH], tooMany);
        });
    }

    @Test
    void read_should_refuse_malformed_payloads_without_an_exception() {
        byte[] payload = new ChannelAnswer(SENDER_CONNECTION, 1L, 2L, new byte[EphemeralKeyPair.KEY_LENGTH], CANDIDATES).write();
        byte[] trailing = Arrays.copyOf(payload, payload.length + 1);
        byte[] unknownKind = payload.clone();
        byte[] tooManyCandidates = new ChannelOffer(SENDER_CONNECTION, 1L, new byte[EphemeralKeyPair.KEY_LENGTH], List.of()).write();

        unknownKind[0]                                  = (byte) 0x7f;
        tooManyCandidates[tooManyCandidates.length - 1] = (byte) (ChannelSignal.MAXIMUM_CANDIDATES + 1);

        Assertions.assertNull(ChannelSignal.read(trailing));
        Assertions.assertNull(ChannelSignal.read(unknownKind));
        Assertions.assertNull(ChannelSignal.read(tooManyCandidates));

        for (int length = 0; length < payload.length; length++) {
            Assertions.assertNull(ChannelSignal.read(Arrays.copyOf(payload, length)), "a payload cut to " + length + " bytes reads");
        }

        SplittableRandom random = new SplittableRandom(RANDOM_SEED);

        for (int count = 0; count < RANDOM_CHANGES; count++) {
            byte[] changed = payload.clone();

            changed[random.nextInt(changed.length)] = (byte) random.nextInt();
            ChannelSignal.read(changed);
        }
    }

    @Test
    void join_offer_and_refusal_read_back() {
        byte[] key = new byte[EphemeralKeyPair.KEY_LENGTH];
        ModFingerprint identity = ModFingerprint.of(new byte[ModFingerprint.MAXIMUM_LENGTH]);
        byte[] proof = new byte[JoinProofUtils.PROOF_LENGTH];

        Arrays.fill(proof, (byte) 7);

        byte[] largest = new ChannelOffer(SENDER_CONNECTION, 3L, key, 0xffffffffL, identity, proof, createLargestCandidates()).write();
        byte[] withoutProofBytes = new ChannelOffer(SENDER_CONNECTION, 3L, key, 1L, identity, CANDIDATES).write();
        byte[] withoutRoomBytes = new ChannelOffer(SENDER_CONNECTION, 3L, key, CANDIDATES).write();
        byte[] refusalBytes = new ChannelRefusal(SENDER_CONNECTION, 3L, JoinRefusal.ENGINE_VERSION).write();
        byte[] missingBytes = new ChannelRefusal(SENDER_CONNECTION, 3L, null).write();
        ChannelOffer offer = Assertions.assertInstanceOf(ChannelOffer.class, ChannelSignal.read(largest));
        ChannelOffer withoutProof = Assertions.assertInstanceOf(ChannelOffer.class, ChannelSignal.read(withoutProofBytes));
        ChannelOffer withoutRoom = Assertions.assertInstanceOf(ChannelOffer.class, ChannelSignal.read(withoutRoomBytes));
        ChannelRefusal refusal = Assertions.assertInstanceOf(ChannelRefusal.class, ChannelSignal.read(refusalBytes));
        ChannelRefusal missing = Assertions.assertInstanceOf(ChannelRefusal.class, ChannelSignal.read(missingBytes));

        Assertions.assertTrue(largest.length <= SignalMessage.MAXIMUM_PAYLOAD_LENGTH);
        Assertions.assertTrue(offer.isJoin());
        Assertions.assertEquals(0xffffffffL, offer.getRoomNumber());
        Assertions.assertEquals(identity, offer.getIdentity());
        Assertions.assertArrayEquals(proof, offer.getProof());
        Assertions.assertNull(withoutProof.getProof());
        Assertions.assertFalse(withoutRoom.isJoin());
        Assertions.assertEquals(JoinRefusal.ENGINE_VERSION, refusal.getJoinRefusal());
        Assertions.assertEquals(3L, refusal.getOfferNumber());
        Assertions.assertNull(missing.getJoinRefusal());
        Assertions.assertEquals(SENDER_CONNECTION, refusal.getSenderConnection());
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new ChannelOffer(SENDER_CONNECTION, 3L, key, ChannelOffer.NO_ROOM, identity, CANDIDATES));

        byte[] unknownReason = new ChannelRefusal(SENDER_CONNECTION, 3L, null).write();

        unknownReason[unknownReason.length - 1] = (byte) 0x7f;
        Assertions.assertNull(ChannelSignal.read(unknownReason));
    }
}
