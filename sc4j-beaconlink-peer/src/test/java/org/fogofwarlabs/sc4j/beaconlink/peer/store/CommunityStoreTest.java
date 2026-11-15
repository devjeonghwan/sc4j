package org.fogofwarlabs.sc4j.beaconlink.peer.store;

import org.fogofwarlabs.sc4j.beaconlink.protocol.community.*;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerStamp;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.List;

/**
 * Tests the community store of a topic: the newest presence of a player's connection replaces the older one while the player's other
 * connections keep theirs, up to the connections per player, the newest sign-out of a player is kept, the chat backlog keeps the newest lines
 * and is bounded, and expired, too long, forged and unstamped records and records of another name than their stamp's are refused.
 */
final class CommunityStoreTest {
    private static final long NOW_MILLIS = 1_800_000_000_000L;
    private static final long LIFETIME_MILLIS = 120_000L;
    private static final long MAXIMUM_LIFETIME_MILLIS = 300_000L;
    private static final long SKEW_MILLIS = 30_000L;
    private static final int STAMP_DIFFICULTY = 16;
    private static final int PRESENCE_CAPACITY = 2;
    private static final int CONNECTIONS_PER_PLAYER = 2;
    private static final long CONNECTION = 0x1111L;
    private static final long OTHER_CONNECTION = 0x2222L;
    private static final long THIRD_CONNECTION = 0x3333L;
    private static final int BACKLOG = 4;
    private static final int LINES = 10;
    private static final int DIGEST_LENGTH = PresenceContent.IDENTITY_DIGEST_LENGTH;
    private static final @NotNull TopicKey CHANNEL = TopicKey.ofChannel("General");

    private static @NotNull CommunityStore createStore() {
        return createStore(PRESENCE_CAPACITY);
    }

    private static @NotNull CommunityStore createStore(int presenceCapacity) {
        return new CommunityStore(presenceCapacity, CONNECTIONS_PER_PLAYER, BACKLOG, MAXIMUM_LIFETIME_MILLIS, SKEW_MILLIS, STAMP_DIFFICULTY);
    }

    private static @NotNull IdentityKeyPair createPlayer() {
        return IdentityKeyPair.generate(new SecureRandom());
    }

    private static @NotNull PresenceRecord presence(@NotNull IdentityKeyPair player, long sequence, long expiryMillis) {
        return presence(player, CONNECTION, sequence, expiryMillis);
    }

    private static @NotNull PresenceRecord presence(@NotNull IdentityKeyPair player, long connection, long sequence, long expiryMillis) {
        PresenceContent content = new PresenceContent("Player", "test-mod", "1.0.0", new byte[DIGEST_LENGTH], PresenceStatus.AVAILABLE, "General",
                                                      new byte[PresenceContent.WHISPER_KEY_LENGTH], List.of(), null);

        return PresenceRecord.sign(player, PlayerStamp.solve(player.getPublicKey(), "Player", STAMP_DIFFICULTY), CHANNEL, connection, sequence,
                                   expiryMillis, content);
    }

    private static @NotNull ChatLineRecord line(@NotNull IdentityKeyPair player, long sequence, long timestampMillis) {
        return ChatLineRecord.sign(player, PlayerStamp.solve(player.getPublicKey(), "Player", STAMP_DIFFICULTY), CHANNEL, sequence, timestampMillis,
                                   false, "line " + sequence);
    }

    private static @NotNull PresenceRecord forge(@NotNull PresenceRecord genuine) {
        WireWriter writer = new WireWriter(PresenceRecord.MAXIMUM_ENCODED_LENGTH);

        genuine.write(writer);

        byte[] bytes = writer.toByteArray();

        bytes[bytes.length - 1] ^= 1;

        PresenceRecord forged = PresenceRecord.read(new WireReader(bytes, 0, bytes.length));

        Assertions.assertNotNull(forged);

        return forged;
    }

    @Test
    void offer_should_keep_the_newest_presence_of_a_player() {
        CommunityStore store = createStore();
        IdentityKeyPair player = createPlayer();
        PresenceRecord first = presence(player, 1L, NOW_MILLIS + LIFETIME_MILLIS);
        PresenceRecord second = presence(player, 2L, NOW_MILLIS + LIFETIME_MILLIS);

        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(first, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(second, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.STALE, store.offer(first, NOW_MILLIS));
        Assertions.assertEquals(List.of(second), store.getPresences(NOW_MILLIS));
        Assertions.assertEquals(second, store.find(second.getRecordId()));
        Assertions.assertNull(store.find(first.getRecordId()));
    }

    @Test
    void offer_should_keep_the_newest_presence_of_each_player_connection_up_to_the_connections_per_player() {
        CommunityStore store = createStore(PRESENCE_CAPACITY * 4);
        IdentityKeyPair player = createPlayer();
        PresenceRecord first = presence(player, CONNECTION, 1L, NOW_MILLIS + LIFETIME_MILLIS);
        PresenceRecord other = presence(player, OTHER_CONNECTION, 1L, NOW_MILLIS + LIFETIME_MILLIS + 1L);
        PresenceRecord renewed = presence(player, CONNECTION, 2L, NOW_MILLIS + LIFETIME_MILLIS + 2L);
        PresenceRecord stranger = presence(createPlayer(), CONNECTION, 1L, NOW_MILLIS + 1L);
        PresenceRecord third = presence(player, THIRD_CONNECTION, 1L, NOW_MILLIS + LIFETIME_MILLIS + 3L);

        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(first, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(other, NOW_MILLIS), "another connection of the player stands beside the first");
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(renewed, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.STALE, store.offer(first, NOW_MILLIS), "a connection's older presence is stale");
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(stranger, NOW_MILLIS));
        Assertions.assertEquals(List.of(other, renewed, stranger), store.getPresences(NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(third, NOW_MILLIS),
                                "a further connection takes the place of the player's connection that expires first");
        Assertions.assertEquals(List.of(renewed, stranger, third), store.getPresences(NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.FULL, store.offer(presence(player, OTHER_CONNECTION, 2L, NOW_MILLIS + 1L), NOW_MILLIS),
                                "a connection that expires before every connection of the player finds no place");
    }

    @Test
    void offer_should_keep_the_newest_sign_out_of_a_player() {
        CommunityStore store = createStore();
        IdentityKeyPair player = createPlayer();
        SignOutRecord first = SignOutRecord.sign(player, CONNECTION, 1L, NOW_MILLIS, NOW_MILLIS + LIFETIME_MILLIS);
        SignOutRecord second = SignOutRecord.sign(player, OTHER_CONNECTION, 2L, NOW_MILLIS, NOW_MILLIS + LIFETIME_MILLIS);
        SignOutRecord tooLong = SignOutRecord.sign(player, CONNECTION, 3L, NOW_MILLIS, NOW_MILLIS + MAXIMUM_LIFETIME_MILLIS + 1L);
        SignOutRecord dated = SignOutRecord.sign(player, CONNECTION, 3L, NOW_MILLIS + SKEW_MILLIS + 1L, NOW_MILLIS + LIFETIME_MILLIS);

        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(first, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(second, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.STALE, store.offer(first, NOW_MILLIS));
        Assertions.assertEquals(second, store.findSignOut(player.getPublicKey(), NOW_MILLIS));
        Assertions.assertEquals(second, store.find(second.getRecordId()));
        Assertions.assertTrue(store.getRecordIds().contains(second.getRecordId()));
        Assertions.assertEquals(RecordOffer.LIFETIME_TOO_LONG, store.offer(tooLong, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.LIFETIME_TOO_LONG, store.offer(dated, NOW_MILLIS));

        store.removeExpired(NOW_MILLIS + LIFETIME_MILLIS);

        Assertions.assertNull(store.findSignOut(player.getPublicKey(), NOW_MILLIS + LIFETIME_MILLIS));
    }

    @Test
    void offer_should_refuse_expired_too_long_forged_unstamped_and_renamed_records() {
        CommunityStore store = createStore();
        IdentityKeyPair player = createPlayer();
        PresenceRecord genuine = presence(player, 1L, NOW_MILLIS + LIFETIME_MILLIS);
        long invalidNonce = 0L;

        while (new PlayerStamp(player.getPublicKey(), "Player", invalidNonce).isValid(STAMP_DIFFICULTY)) {
            invalidNonce++;
        }

        PresenceRecord unstamped = PresenceRecord.sign(player, new PlayerStamp(player.getPublicKey(), "Player", invalidNonce), CHANNEL, CONNECTION,
                                                       1L, NOW_MILLIS + LIFETIME_MILLIS, genuine.getContent());
        PresenceContent renamedContent = new PresenceContent("Kerrigan", "test-mod", "1.0.0", new byte[DIGEST_LENGTH], PresenceStatus.AVAILABLE,
                                                             "General", new byte[PresenceContent.WHISPER_KEY_LENGTH], List.of(), null);
        PresenceRecord renamed = PresenceRecord.sign(player, new PlayerStamp(player.getPublicKey(), "Kerrigan", genuine.getStamp().getNonce()),
                                                     CHANNEL, CONNECTION, 1L, NOW_MILLIS + LIFETIME_MILLIS, renamedContent);
        ChatLineRecord renamedLine = ChatLineRecord.sign(player, new PlayerStamp(player.getPublicKey(), "Kerrigan", genuine.getStamp().getNonce()),
                                                         CHANNEL, 1L, NOW_MILLIS, false, "line");

        Assertions.assertEquals(RecordOffer.EXPIRED, store.offer(presence(player, 1L, NOW_MILLIS), NOW_MILLIS));

        PresenceRecord tooLong = presence(player, 1L, NOW_MILLIS + MAXIMUM_LIFETIME_MILLIS + 1L);

        Assertions.assertEquals(RecordOffer.LIFETIME_TOO_LONG, store.offer(tooLong, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.LIFETIME_TOO_LONG, store.offer(line(player, 1L, NOW_MILLIS + SKEW_MILLIS + 1L), NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.EXPIRED, store.offer(line(player, 1L, NOW_MILLIS - ChatLineRecord.LIFETIME_MILLIS), NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.INVALID_SIGNATURE, store.offer(forge(genuine), NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.INVALID_STAMP, store.offer(unstamped, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.INVALID_STAMP, store.offer(renamed, NOW_MILLIS), "another name with the key's stamp nonce");
        Assertions.assertEquals(RecordOffer.INVALID_STAMP, store.offer(renamedLine, NOW_MILLIS), "another name with the key's stamp nonce");
        Assertions.assertTrue(store.getPresences(NOW_MILLIS).isEmpty());
        Assertions.assertTrue(store.getLines().isEmpty());
    }

    @Test
    void offer_should_keep_the_newest_lines_of_a_bounded_backlog() {
        CommunityStore store = createStore();
        IdentityKeyPair player = createPlayer();

        for (int index = 0; index < LINES; index++) {
            Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(line(player, index, NOW_MILLIS + index), NOW_MILLIS + index));
        }

        List<ChatLineRecord> lines = store.getLines();

        Assertions.assertEquals(BACKLOG, lines.size());
        Assertions.assertEquals(LINES - BACKLOG, lines.getFirst().getSequence());
        Assertions.assertEquals(LINES - 1, lines.getLast().getSequence());
        Assertions.assertEquals(RecordOffer.FULL, store.offer(line(player, 0L, NOW_MILLIS), NOW_MILLIS + LINES));

        store.removeExpired(NOW_MILLIS + ChatLineRecord.LIFETIME_MILLIS + LINES);

        Assertions.assertTrue(store.getLines().isEmpty());
    }

    @Test
    void offer_should_give_the_place_of_the_first_expiring_presence_to_a_later_one() {
        CommunityStore store = createStore();
        PresenceRecord early = presence(createPlayer(), 1L, NOW_MILLIS + 1_000L);
        PresenceRecord middle = presence(createPlayer(), 1L, NOW_MILLIS + 2_000L);
        PresenceRecord late = presence(createPlayer(), 1L, NOW_MILLIS + 3_000L);

        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(middle, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(late, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.FULL, store.offer(early, NOW_MILLIS));
        Assertions.assertEquals(RecordOffer.ACCEPTED, store.offer(presence(createPlayer(), 1L, NOW_MILLIS + 4_000L), NOW_MILLIS));
        Assertions.assertFalse(store.getPresences(NOW_MILLIS).contains(middle));
    }
}
