package org.fogofwarlabs.sc4j.engine.lockstep;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the in-memory lockstep transport and the turn exchange over it: the all-turns barrier, the execution order of the participants, the
 * turns kept in transit, and a departure that the others stop waiting for once its turns were received.
 */
final class InMemoryLockstepNetworkTest {
    private static final int LATENCY_TURNS = 2;

    private static @NotNull String describe(@NotNull List<@NotNull PeerTurn> turns) {
        StringBuilder text = new StringBuilder();

        for (PeerTurn turn : turns) {
            text.append(turn.getPeer().getNumber()).append('=').append(turn.getTurn().length == 0 ? "-" : String.valueOf(turn.getTurn()[0]))
                .append(' ');
        }

        return text.toString().trim();
    }

    @Test
    void exchange_should_keep_the_latency_in_transit_and_wait_for_every_participant() {
        LockstepPeer first = new LockstepPeer(4);
        LockstepPeer second = new LockstepPeer(1);
        InMemoryLockstepNetwork network = new InMemoryLockstepNetwork(List.of(first, second));
        byte[] next = {10};
        TurnExchange firstExchange = new TurnExchange(network.getTransport(first), () -> new byte[]{next[0]++});
        TurnExchange secondExchange = new TurnExchange(network.getTransport(second), () -> new byte[]{(byte) -next[0]});

        Assertions.assertNull(firstExchange.exchange(LATENCY_TURNS));
        Assertions.assertEquals(LATENCY_TURNS, network.getTransport(first).getTurnsInTransit());
        Assertions.assertNull(firstExchange.exchange(LATENCY_TURNS));

        List<PeerTurn> secondTurns = secondExchange.exchange(LATENCY_TURNS);
        List<PeerTurn> firstTurns = firstExchange.exchange(LATENCY_TURNS);

        Assertions.assertNotNull(secondTurns);
        Assertions.assertNotNull(firstTurns);
        Assertions.assertEquals("4=10 1=-12", describe(firstTurns));
        Assertions.assertEquals(describe(firstTurns), describe(secondTurns));
        Assertions.assertEquals(LATENCY_TURNS, network.getTransport(first).getTurnsInTransit());
        Assertions.assertEquals(LATENCY_TURNS, network.getTransport(second).getTurnsInTransit());
    }

    @Test
    void receive_turns_should_stop_waiting_for_a_participant_once_its_turns_after_leaving_were_received() {
        LockstepPeer first = new LockstepPeer(0);
        LockstepPeer second = new LockstepPeer(1);
        InMemoryLockstepNetwork network = new InMemoryLockstepNetwork(List.of(first, second));
        LockstepTransport leaving = network.getTransport(second);
        LockstepTransport staying = network.getTransport(first);

        leaving.sendTurn(new byte[]{1});
        leaving.leave(0);
        staying.sendTurn(new byte[]{2});
        staying.sendTurn(new byte[]{3});

        List<PeerTurn> withDeparted = staying.receiveTurns();
        List<PeerTurn> alone = staying.receiveTurns();

        Assertions.assertNotNull(withDeparted);
        Assertions.assertNotNull(alone);
        Assertions.assertEquals("0=2 1=1", describe(withDeparted));
        Assertions.assertEquals("0=3", describe(alone));
        Assertions.assertThrows(IllegalStateException.class, () -> leaving.sendTurn(new byte[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new InMemoryLockstepNetwork(List.of(first, new LockstepPeer(0))));
    }
}
