package org.fogofwarlabs.sc4j.beaconlink.peer.store;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.ModFingerprint;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomAdvertisement;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.RoomDescription;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKind;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.List;

/**
 * Tests the room store of a topic: newer sequences replace older ones and withdrawals block older advertisements, every stored advertisement
 * is found by its record ID, the store is bounded in total and per host, and expired rooms free their places.
 */
final class RoomStoreTest {
    private static final long NOW_MILLIS = 1_800_000_000_000L;
    private static final long LIFETIME_MILLIS = 60_000L;
    private static final long MAXIMUM_LIFETIME_MILLIS = 600_000L;
    private static final int BUDGET = 4096;
    private static final @NotNull ModFingerprint FINGERPRINT = ModFingerprint.of(new byte[]{6});
    private static final @NotNull TopicKey TOPIC = TopicKey.of(TopicKind.MOD, "store-mod");
    private static final long CONNECTION = 7L;
    private static final long OTHER_CONNECTION = 8L;

    private static @NotNull RoomDescription createDescription() {
        return new RoomDescription(FINGERPRINT, "Room", "Map", 1, 2);
    }

    private static @NotNull RoomAdvertisement publish(@NotNull IdentityKeyPair host, long number, long sequence, long lifetimeMillis) {
        return publish(host, CONNECTION, number, sequence, lifetimeMillis);
    }

    private static @NotNull RoomAdvertisement publish(@NotNull IdentityKeyPair host, long connection, long number, long sequence,
                                                      long lifetimeMillis) {
        return RoomAdvertisement.signPublication(host, connection, TOPIC, number, sequence, NOW_MILLIS + lifetimeMillis, createDescription(),
                                                 List.of(), null);
    }

    @Test
    void offer_should_keep_the_newest_sequence_and_block_older_ones_after_a_withdrawal() {
        IdentityKeyPair host = IdentityKeyPair.generate(new SecureRandom());
        RoomStore store = new RoomStore(8, 4, 4, MAXIMUM_LIFETIME_MILLIS);
        RoomAdvertisement first = publish(host, 1L, 1L, LIFETIME_MILLIS);
        RoomAdvertisement second = publish(host, 1L, 2L, LIFETIME_MILLIS);
        RoomAdvertisement withdrawal = RoomAdvertisement.signWithdrawal(host, CONNECTION, TOPIC, 1L, 3L, NOW_MILLIS + LIFETIME_MILLIS,
                                                                        createDescription(), List.of());

        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(first, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(second, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.STALE, store.offer(first, NOW_MILLIS));
        Assertions.assertTrue(store.isStale(first));
        Assertions.assertEquals(List.of(second), store.listPage(0, BUDGET, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(withdrawal, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.STALE, store.offer(second, NOW_MILLIS));
        Assertions.assertEquals(0, store.countListed(NOW_MILLIS));
        Assertions.assertEquals(1, store.getSize());
        Assertions.assertEquals(List.of(withdrawal.getRecordId()), store.getRecordIds());
        Assertions.assertEquals(RecordOffer.EXPIRED, store.offer(publish(host, 2L, 1L, 0L), NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.LIFETIME_TOO_LONG, store.offer(publish(host, 2L, 1L, MAXIMUM_LIFETIME_MILLIS + 1L), NOW_MILLIS));
    }

    @Test
    void find_should_return_the_stored_advertisement_of_a_record_id_until_it_is_replaced_or_expires() {
        IdentityKeyPair host = IdentityKeyPair.generate(new SecureRandom());
        RoomStore store = new RoomStore(8, 4, 4, MAXIMUM_LIFETIME_MILLIS);
        RoomAdvertisement shortRoom = publish(host, 1L, 1L, 1_000L);
        RoomAdvertisement longRoom = publish(host, 2L, 1L, LIFETIME_MILLIS);
        RoomAdvertisement updated = publish(host, 2L, 2L, LIFETIME_MILLIS);

        store.offer(shortRoom, NOW_MILLIS);
        store.offer(longRoom, NOW_MILLIS);

        Assertions.assertEquals(shortRoom, store.find(shortRoom.getRecordId()));
        Assertions.assertEquals(List.of(shortRoom, longRoom), store.getListed(NOW_MILLIS));
        Assertions.assertEquals(List.of(longRoom), store.getListed(NOW_MILLIS + 1_000L));
        Assertions.assertEquals(1, store.listPage(0, longRoom.getEncodedLength(), NOW_MILLIS).size());

        store.offer(updated, NOW_MILLIS);
        store.removeExpired(NOW_MILLIS + 1_000L);

        Assertions.assertNull(store.find(longRoom.getRecordId()));
        Assertions.assertNull(store.find(shortRoom.getRecordId()));
        Assertions.assertEquals(updated, store.find(updated.getRecordId()));
        Assertions.assertEquals(List.of(updated.getRecordId()), store.getRecordIds());
    }

    @Test
    void offer_should_respect_the_total_and_host_limits_until_rooms_expire() {
        SecureRandom random = new SecureRandom();
        IdentityKeyPair host = IdentityKeyPair.generate(random);
        IdentityKeyPair other = IdentityKeyPair.generate(random);
        RoomStore store = new RoomStore(2, 1, 1, MAXIMUM_LIFETIME_MILLIS);

        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(publish(host, 1L, 1L, 1_000L), NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.HOST_LIMIT, store.offer(publish(host, 2L, 1L, LIFETIME_MILLIS), NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(publish(other, 1L, 1L, LIFETIME_MILLIS), NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.FULL, store.offer(publish(IdentityKeyPair.generate(random), 1L, 1L, LIFETIME_MILLIS), NOW_MILLIS));

        store.removeExpired(NOW_MILLIS + 1_000L);

        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(publish(host, 2L, 1L, LIFETIME_MILLIS), NOW_MILLIS + 1_000L));
    }

    @Test
    void offer_should_hold_the_rooms_of_each_connection_of_a_host_apart_within_the_host_limit() {
        IdentityKeyPair host = IdentityKeyPair.generate(new SecureRandom());
        RoomStore store = new RoomStore(8, 3, 2, MAXIMUM_LIFETIME_MILLIS);
        RoomAdvertisement first = publish(host, CONNECTION, 1L, 1L, LIFETIME_MILLIS);
        RoomAdvertisement second = publish(host, CONNECTION, 2L, 1L, LIFETIME_MILLIS);
        RoomAdvertisement sameNumber = publish(host, OTHER_CONNECTION, 1L, 1L, LIFETIME_MILLIS);

        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(first, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(second, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.HOST_LIMIT, store.offer(publish(host, CONNECTION, 3L, 1L, LIFETIME_MILLIS), NOW_MILLIS),
                                "the connection holds the most rooms of one connection");
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(sameNumber, NOW_MILLIS), "another connection's room of the same number");
        Assertions.assertEquals(RecordOffer.HOST_LIMIT, store.offer(publish(host, OTHER_CONNECTION, 2L, 1L, LIFETIME_MILLIS), NOW_MILLIS),
                                "the key holds the most rooms of one host");
        Assertions.assertEquals(List.of(first, second, sameNumber), store.getListed(NOW_MILLIS));
    }

    @Test
    void offer_should_refuse_a_forged_advertisement() {
        SecureRandom random = new SecureRandom();
        IdentityKeyPair host = IdentityKeyPair.generate(random);
        RoomStore store = new RoomStore(8, 4, 4, MAXIMUM_LIFETIME_MILLIS);
        RoomAdvertisement genuine = publish(host, 1L, 1L, LIFETIME_MILLIS);
        WireWriter writer = new WireWriter(RoomAdvertisement.MAXIMUM_ENCODED_LENGTH);

        genuine.write(writer);

        byte[] bytes = writer.toByteArray();

        bytes[bytes.length - 1] ^= 1;

        RoomAdvertisement forged = RoomAdvertisement.read(new WireReader(bytes, 0, bytes.length));

        Assertions.assertNotNull(forged);
        Assertions.assertEquals(RecordOffer.INVALID_SIGNATURE, store.offer(forged, NOW_MILLIS));
        Assertions.assertEquals(0, store.getSize());
    }
}
