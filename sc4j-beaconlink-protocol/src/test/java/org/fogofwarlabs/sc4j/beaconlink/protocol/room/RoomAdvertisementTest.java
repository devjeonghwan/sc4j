package org.fogofwarlabs.sc4j.beaconlink.protocol.room;

import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.NetworkCoordinate;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKind;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.List;

/**
 * Tests the signed room advertisement: it reads back unchanged with a valid host signature, a private and started room keeps its flags and
 * its join key, any changed byte breaks the signature or the decoding, the largest advertisement has the documented length, and invalid
 * descriptions are refused.
 */
final class RoomAdvertisementTest {
    private static final int HOME_PEER_PORT = 7455;
    private static final long ROOM_NUMBER = 7L;
    private static final long SEQUENCE = 3L;
    private static final long EXPIRY_MILLIS = 1_800_000_600_000L;
    private static final int CAPACITY = 4;
    private static final @NotNull TopicKey TOPIC = TopicKey.of(TopicKind.MOD, "duel-mod");
    private static final long CONNECTION = 0x0A0B0C0D0E0F1011L;
    private static final byte @NotNull [] MOD_DATA = {1, 2, 0, 6, 64};

    private static @NotNull RoomAdvertisement read(byte @NotNull [] bytes) {
        WireReader reader = new WireReader(bytes, 0, bytes.length);
        RoomAdvertisement advertisement = RoomAdvertisement.read(reader);

        Assertions.assertNotNull(advertisement);
        Assertions.assertTrue(reader.isComplete());

        return advertisement;
    }

    private static byte @NotNull [] write(@NotNull RoomAdvertisement advertisement) {
        WireWriter writer = new WireWriter(RoomAdvertisement.MAXIMUM_ENCODED_LENGTH);

        advertisement.write(writer);

        return writer.toByteArray();
    }

    @Test
    void advertisement_reads_back_unchanged_with_a_valid_host_signature() {
        IdentityKeyPair host = IdentityKeyPair.generate(new SecureRandom());
        RoomDescription description = new RoomDescription(ModFingerprint.of(new byte[]{1, 2, 3}),
                                                          "Duel at noon", "Ashen Ford",
                                                          1, CAPACITY,
                                                          MOD_DATA);
        NetworkCoordinate coordinate = new NetworkCoordinate(new double[]{-3.0, 15.0, 120.0}, 4.0);
        RoomAdvertisement advertisement = RoomAdvertisement.signPublication(host, CONNECTION, TOPIC, ROOM_NUMBER, SEQUENCE, EXPIRY_MILLIS,
                                                                            description, List.of(), coordinate);
        byte[] bytes = write(advertisement);
        RoomAdvertisement read = read(bytes);

        Assertions.assertEquals(advertisement, read);
        Assertions.assertEquals(bytes.length, read.getEncodedLength());
        Assertions.assertTrue(read.isSignatureValid());
        Assertions.assertFalse(read.isWithdrawn());
        Assertions.assertEquals(TOPIC, read.getTopic());
        Assertions.assertEquals(new RoomKey(host.getPublicKey(), CONNECTION, ROOM_NUMBER), read.getKey());
        Assertions.assertEquals(SEQUENCE, read.getSequence());
        Assertions.assertEquals(EXPIRY_MILLIS, read.getExpiryMillis());
        Assertions.assertEquals("Duel at noon", read.getDescription().getRoomName());
        Assertions.assertEquals("Ashen Ford", read.getDescription().getMapName());
        Assertions.assertEquals(CAPACITY, read.getDescription().getCapacity());
        Assertions.assertArrayEquals(MOD_DATA, read.getDescription().getModData());
        Assertions.assertEquals(coordinate, read.getCoordinate());

        RoomAdvertisement withdrawal = RoomAdvertisement.signWithdrawal(host, CONNECTION, TOPIC, ROOM_NUMBER, SEQUENCE + 1, EXPIRY_MILLIS,
                                                                        description, List.of());

        Assertions.assertTrue(read(write(withdrawal)).isWithdrawn());
        Assertions.assertNull(read(write(withdrawal)).getCoordinate());
        Assertions.assertTrue(read(write(withdrawal)).isSignatureValid());
        Assertions.assertEquals(advertisement.getRecordId(), read.getRecordId());
        Assertions.assertNotEquals(advertisement.getRecordId(), withdrawal.getRecordId());
    }

    @Test
    void private_started_room_reads_back_with_its_join_key() {
        IdentityKeyPair host = IdentityKeyPair.generate(new SecureRandom());
        byte[] joinKey = new byte[RoomDescription.JOIN_KEY_LENGTH];

        joinKey[0] = 5;

        RoomDescription description = new RoomDescription(ModFingerprint.of(new byte[]{4}),
                                                          "Private duel", "Ashen Ford",
                                                          1, CAPACITY,
                                                          true, joinKey,
                                                          MOD_DATA);
        byte[] bytes = write(RoomAdvertisement.signPublication(host, CONNECTION, TOPIC, ROOM_NUMBER, SEQUENCE, EXPIRY_MILLIS,
                                                               description, List.of(), null));
        RoomDescription read = read(bytes).getDescription();
        RoomDescription open = new RoomDescription(ModFingerprint.of(new byte[]{4}), "Open duel", "Ashen Ford", 1, CAPACITY, MOD_DATA);

        Assertions.assertTrue(read.isPrivate());
        Assertions.assertTrue(read.isStarted());
        Assertions.assertArrayEquals(joinKey, read.getJoinKey());
        Assertions.assertArrayEquals(MOD_DATA, read.getModData());
        Assertions.assertEquals(bytes.length, read(bytes).getEncodedLength());
        Assertions.assertFalse(open.isPrivate());
        Assertions.assertFalse(open.isStarted());
        Assertions.assertNull(open.getJoinKey());
    }

    @Test
    void any_changed_byte_breaks_the_signature_or_the_decoding() {
        IdentityKeyPair host = IdentityKeyPair.generate(new SecureRandom());
        RoomDescription description = new RoomDescription(ModFingerprint.of(new byte[]{9}), "Room", "Map", 0, 2);
        byte[] bytes = write(RoomAdvertisement.signPublication(host, CONNECTION, TOPIC, ROOM_NUMBER, SEQUENCE, EXPIRY_MILLIS,
                                                               description, List.of(), NetworkCoordinate.ORIGIN));

        for (int index = 0; index < bytes.length; index++) {
            byte[] changed = bytes.clone();

            changed[index] ^= 0x10;

            WireReader reader = new WireReader(changed, 0, changed.length);
            RoomAdvertisement advertisement = RoomAdvertisement.read(reader);

            Assertions.assertTrue(advertisement == null || !reader.isComplete() || !advertisement.isSignatureValid(), "byte " + index);
        }

        for (int length = 0; length < bytes.length; length++) {
            WireReader reader = new WireReader(bytes, 0, length);

            Assertions.assertNull(RoomAdvertisement.read(reader), "length " + length);
        }
    }

    @Test
    void largest_advertisement_has_the_documented_length() {
        IdentityKeyPair host = IdentityKeyPair.generate(new SecureRandom());
        String name = "n".repeat(RoomDescription.MAXIMUM_NAME_LENGTH);
        RoomDescription description = new RoomDescription(ModFingerprint.of(new byte[ModFingerprint.MAXIMUM_LENGTH]),
                                                          name, name,
                                                          RoomDescription.MAXIMUM_CAPACITY, RoomDescription.MAXIMUM_CAPACITY,
                                                          true, new byte[RoomDescription.JOIN_KEY_LENGTH],
                                                          new byte[RoomDescription.MAXIMUM_MOD_DATA_LENGTH]);
        NodeEndpoint homePeer = new NodeEndpoint(host.getPublicKey(), new InetSocketAddress(InetAddress.ofLiteral("2001:db8::1"), HOME_PEER_PORT));
        RoomAdvertisement advertisement = RoomAdvertisement.signPublication(host, CONNECTION, TOPIC,
                                                                            RoomKey.MAXIMUM_NUMBER, RoomAdvertisement.MAXIMUM_SEQUENCE,
                                                                            Long.MAX_VALUE, description, List.of(homePeer, homePeer, homePeer),
                                                                            NetworkCoordinate.ORIGIN);

        Assertions.assertEquals(RoomAdvertisement.MAXIMUM_ENCODED_LENGTH, advertisement.getEncodedLength());
        Assertions.assertEquals(advertisement, read(write(advertisement)));
    }

    @Test
    void description_should_refuse_values_out_of_range() {
        ModFingerprint fingerprint = ModFingerprint.of(new byte[]{1});
        String longName = "n".repeat(RoomDescription.MAXIMUM_NAME_LENGTH + 1);

        Assertions.assertThrows(IllegalArgumentException.class, () -> new RoomDescription(fingerprint, "", "Map", 0, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RoomDescription(fingerprint, longName, "Map", 0, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RoomDescription(fingerprint, "Room", "Map", 3, 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RoomDescription(fingerprint, "Room", "Map", 0, 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new RoomDescription(fingerprint, "Room", "Map", 0, 1, new byte[RoomDescription.MAXIMUM_MOD_DATA_LENGTH + 1]));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new RoomDescription(fingerprint, "Room", "Map", 0, 1, false, new byte[1], new byte[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ModFingerprint.of(new byte[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ModFingerprint.of(new byte[ModFingerprint.MAXIMUM_LENGTH + 1]));
    }
}
