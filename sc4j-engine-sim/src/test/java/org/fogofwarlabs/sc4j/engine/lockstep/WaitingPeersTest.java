package org.fogofwarlabs.sc4j.engine.lockstep;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the waiting state of the all-turns barrier: awaited participants keep the time they were first awaited, become overdue, count down
 * to their drop, a person can drop an overdue participant early, and the waiting dialog shows the seconds to the drop or those of the
 * transport's own countdown.
 */
final class WaitingPeersTest {
    private static final long OVERDUE_MILLIS = 100L;
    private static final long DROP_MILLIS = 500L;
    private static final int DIALOG_SECONDS = 45;

    @Test
    void awaited_peers_become_overdue_and_are_dropped_after_the_drop_time() {
        LockstepPeer first = new LockstepPeer(0);
        LockstepPeer second = new LockstepPeer(1);
        WaitingPeers waiting = new WaitingPeers(OVERDUE_MILLIS, DROP_MILLIS);

        waiting.update(List.of(first), 1000L);
        waiting.update(List.of(first, second), 1050L);

        Assertions.assertEquals(List.of(first, second), waiting.getAwaitedPeers());
        Assertions.assertEquals(List.of(), waiting.getOverduePeers());
        Assertions.assertEquals(-1L, waiting.getCountdownMillis());

        waiting.update(List.of(first, second), 1120L);

        Assertions.assertEquals(List.of(first), waiting.getOverduePeers());
        Assertions.assertEquals(380L, waiting.getCountdownMillis());
        Assertions.assertEquals(List.of(), waiting.takeDueDrops());

        waiting.update(List.of(first, second), 1500L);

        Assertions.assertEquals(List.of(first), waiting.takeDueDrops());
        Assertions.assertEquals(List.of(second), waiting.getAwaitedPeers());
        Assertions.assertEquals(50L, waiting.getCountdownMillis());

        waiting.clear();

        Assertions.assertFalse(waiting.isWaiting());
    }

    @Test
    void request_drop_should_drop_only_an_overdue_peer_at_the_next_take() {
        LockstepPeer first = new LockstepPeer(0);
        LockstepPeer second = new LockstepPeer(1);
        WaitingPeers waiting = new WaitingPeers(OVERDUE_MILLIS, DROP_MILLIS);

        waiting.update(List.of(first), 0L);
        waiting.update(List.of(first, second), OVERDUE_MILLIS);

        Assertions.assertTrue(waiting.requestDrop(first));
        Assertions.assertFalse(waiting.requestDrop(second));
        Assertions.assertEquals(List.of(first), waiting.takeDueDrops());

        waiting.update(List.of(), OVERDUE_MILLIS + 1L);

        Assertions.assertFalse(waiting.isWaiting());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new WaitingPeers(DROP_MILLIS, OVERDUE_MILLIS));
    }

    @Test
    void update_should_start_new_awaited_peers_at_the_wait_start() {
        LockstepPeer first = new LockstepPeer(0);
        LockstepPeer second = new LockstepPeer(1);
        WaitingPeers waiting = new WaitingPeers(OVERDUE_MILLIS, DROP_MILLIS);

        waiting.update(List.of(first), 0L, OVERDUE_MILLIS - 1L);

        Assertions.assertEquals(List.of(), waiting.getOverduePeers());

        waiting.update(List.of(first, second), OVERDUE_MILLIS - 1L, OVERDUE_MILLIS);

        Assertions.assertEquals(List.of(first), waiting.getOverduePeers());
        Assertions.assertEquals(DROP_MILLIS - OVERDUE_MILLIS, waiting.getCountdownMillis());
    }

    @Test
    void get_countdown_seconds_should_round_the_drop_time_up_or_show_the_dialog_countdown() {
        LockstepPeer peer = new LockstepPeer(0);
        WaitingPeers waiting = new WaitingPeers(OVERDUE_MILLIS, DROP_MILLIS + 2000L);
        WaitCountdown countdown = new WaitCountdown(DIALOG_SECONDS, DROP_MILLIS, 0L, 0);
        WaitingPeers dialog = new WaitingPeers(OVERDUE_MILLIS, DROP_MILLIS, countdown);

        waiting.update(List.of(peer), 0L, OVERDUE_MILLIS - 1L);

        Assertions.assertEquals(WaitCountdown.CLOSED, waiting.getCountdownSeconds());

        waiting.update(List.of(peer), 0L, OVERDUE_MILLIS);

        // 2400 milliseconds left show as 3 seconds.
        Assertions.assertEquals(3, waiting.getCountdownSeconds());

        dialog.update(List.of(peer), 0L, OVERDUE_MILLIS);

        Assertions.assertEquals(WaitCountdown.CLOSED, dialog.getCountdownSeconds());

        countdown.advance(OVERDUE_MILLIS);

        Assertions.assertEquals(DIALOG_SECONDS, dialog.getCountdownSeconds());
    }
}
