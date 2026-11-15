package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.security.SecureRandom;

/**
 * Tests the replay guard: a nonce is accepted once per sender inside the window, timestamps outside the window are refused, a full cache
 * refuses new messages instead of forgetting, one source cannot take more than its share, and entries leave the cache two windows after
 * they were accepted.
 */
final class ReplayGuardTest {
    private static final long WINDOW_MILLIS = 30_000L;
    private static final long NOW_MILLIS = 1_800_000_000_000L;
    private static final int CAPACITY = 2;
    private static final int SHARED_CAPACITY = 8;
    private static final int SOURCE_CAPACITY = 3;
    private static final @NotNull InetAddress SOURCE = InetAddress.ofLiteral("127.0.0.1");
    private static final @NotNull InetAddress OTHER_SOURCE = InetAddress.ofLiteral("192.0.2.7");

    @Test
    void check_should_accept_a_nonce_once_per_sender_inside_the_window() {
        SecureRandom random = new SecureRandom();
        IdentityKey sender = IdentityKeyPair.generate(random).getPublicKey();
        IdentityKey other = IdentityKeyPair.generate(random).getPublicKey();
        ReplayGuard<InetAddress> guard = new ReplayGuard<>(WINDOW_MILLIS, CAPACITY);

        Assertions.assertEquals(ReplayCheck.ACCEPTED, guard.check(SOURCE, sender, 1L, NOW_MILLIS, NOW_MILLIS));
        Assertions.assertEquals(ReplayCheck.REPEATED, guard.check(SOURCE, sender, 1L, NOW_MILLIS, NOW_MILLIS + 1L));
        Assertions.assertEquals(ReplayCheck.REPEATED, guard.check(OTHER_SOURCE, sender, 1L, NOW_MILLIS, NOW_MILLIS + 1L));
        Assertions.assertEquals(ReplayCheck.ACCEPTED, guard.check(SOURCE, other, 1L, NOW_MILLIS, NOW_MILLIS));
        Assertions.assertEquals(ReplayCheck.OUTSIDE_WINDOW, guard.check(SOURCE, sender, 2L, NOW_MILLIS - WINDOW_MILLIS - 1L, NOW_MILLIS));
        Assertions.assertEquals(ReplayCheck.OUTSIDE_WINDOW, guard.check(SOURCE, sender, 2L, NOW_MILLIS + WINDOW_MILLIS + 1L, NOW_MILLIS));
        Assertions.assertTrue(guard.isInWindow(NOW_MILLIS + WINDOW_MILLIS, NOW_MILLIS));
    }

    @Test
    void check_should_refuse_new_messages_while_the_cache_is_full_and_forget_after_two_windows() {
        IdentityKey sender = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();
        ReplayGuard<InetAddress> guard = new ReplayGuard<>(WINDOW_MILLIS, CAPACITY);

        Assertions.assertEquals(ReplayCheck.ACCEPTED, guard.check(SOURCE, sender, 1L, NOW_MILLIS, NOW_MILLIS));
        Assertions.assertEquals(ReplayCheck.ACCEPTED, guard.check(SOURCE, sender, 2L, NOW_MILLIS, NOW_MILLIS));
        Assertions.assertEquals(ReplayCheck.SOURCE_FULL, guard.check(SOURCE, sender, 3L, NOW_MILLIS, NOW_MILLIS));
        Assertions.assertEquals(ReplayCheck.FULL, guard.check(OTHER_SOURCE, sender, 3L, NOW_MILLIS, NOW_MILLIS));

        long later = NOW_MILLIS + 2 * WINDOW_MILLIS;

        Assertions.assertEquals(ReplayCheck.ACCEPTED, guard.check(SOURCE, sender, 3L, later, later));
        Assertions.assertEquals(1, guard.getSize());
        Assertions.assertEquals(ReplayCheck.ACCEPTED, guard.check(SOURCE, sender, 1L, later, later));
    }

    @Test
    void check_should_keep_room_for_other_sources_when_one_source_floods() {
        SecureRandom random = new SecureRandom();
        ReplayGuard<InetAddress> guard = new ReplayGuard<>(WINDOW_MILLIS, SHARED_CAPACITY, SOURCE_CAPACITY);
        IdentityKey victim = IdentityKeyPair.generate(random).getPublicKey();

        // One source signs with a fresh key per message, so no per-key limit applies; only its share of the cache bounds it.
        for (int count = 0; count < SHARED_CAPACITY; count++) {
            ReplayCheck expected = count < SOURCE_CAPACITY ? ReplayCheck.ACCEPTED : ReplayCheck.SOURCE_FULL;

            Assertions.assertEquals(expected, guard.check(SOURCE, IdentityKeyPair.generate(random).getPublicKey(), count, NOW_MILLIS, NOW_MILLIS));
        }

        Assertions.assertEquals(SOURCE_CAPACITY, guard.getSize(SOURCE));
        Assertions.assertEquals(ReplayCheck.ACCEPTED, guard.check(OTHER_SOURCE, victim, 1L, NOW_MILLIS, NOW_MILLIS));
        Assertions.assertEquals(1, guard.getSize(OTHER_SOURCE));

        long later = NOW_MILLIS + 2 * WINDOW_MILLIS;

        Assertions.assertEquals(ReplayCheck.ACCEPTED, guard.check(OTHER_SOURCE, victim, 2L, later, later));
        Assertions.assertEquals(0, guard.getSize(SOURCE));
        Assertions.assertEquals(1, guard.getSize());
    }
}
