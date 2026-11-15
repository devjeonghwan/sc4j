package org.fogofwarlabs.sc4j.engine.lockstep;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

/**
 * Tests the engine lockstep session over the in-memory transport: two engine peers playing the skirmish game run the same commands at the same
 * game loop and reach the same state hash every frame, a peer waits at the all-turns barrier, a peer whose game differs is reported as a
 * desync by every participant, and a peer that leaves is reported at the same game loop by every other participant.
 */
final class LockstepSessionTest {
    private static final int FRAME_COUNT = 1500;
    private static final int PATROL_FRAME = 40;
    private static final int MOVE_FRAME = 600;
    private static final int THIRD_ORDER_FRAME = 900;
    private static final int LEAVE_FRAME = 700;
    private static final int LEAVE_REASON = 7;
    // The departure comes with the leaver's last turns in transit; frames count from 1.
    private static final int DEPARTURE_FRAME = LEAVE_FRAME + LockstepGameScenario.LATENCY_TURNS * LockstepGameScenario.FRAMES_PER_TURN + 1;

    private final @NotNull LockstepGameScenario scenario;

    LockstepSessionTest() {
        this.scenario = new LockstepGameScenario();
    }

    @Test
    void two_peers_run_the_same_commands_and_reach_the_same_state_hash_every_frame() throws IOException {
        List<LockstepGameScenario.Machine> machines = this.createMachines(LockstepGameScenario.SEED, LockstepGameScenario.SEED);

        LockstepGameScenario.run(machines, FRAME_COUNT, () -> {
        });

        Assertions.assertEquals(FRAME_COUNT, machines.get(0).getFrameHashes().size());
        Assertions.assertEquals(machines.get(0).getFrameHashes(), machines.get(1).getFrameHashes());
        Assertions.assertEquals(2, machines.get(0).getExecuted());
        Assertions.assertEquals(2, machines.get(1).getExecuted());
        Assertions.assertEquals(0, machines.get(0).getSession().getDesyncMonitor().getMismatchCount());
        Assertions.assertEquals(0, machines.get(1).getSession().getDesyncMonitor().getMismatchCount());
    }

    @Test
    void prepare_frame_should_wait_until_every_peer_sent_its_turn() throws IOException {
        List<LockstepGameScenario.Machine> machines = this.createMachines(LockstepGameScenario.SEED, LockstepGameScenario.SEED);

        Assertions.assertFalse(machines.get(0).step());
        Assertions.assertFalse(machines.get(0).step());
        Assertions.assertTrue(machines.get(1).step());
        Assertions.assertTrue(machines.get(0).step());
        Assertions.assertEquals(1, machines.get(0).getGame().getClock().getFrame());
        Assertions.assertEquals(machines.get(0).getFrameHashes(), machines.get(1).getFrameHashes());
    }

    @Test
    void a_peer_with_another_game_is_reported_as_a_desync_by_every_participant() throws IOException {
        List<LockstepGameScenario.Machine> machines = this.createMachines(LockstepGameScenario.SEED, LockstepGameScenario.SEED + 1);

        LockstepGameScenario.run(machines, FRAME_COUNT, () -> {
        });

        Assertions.assertTrue(machines.get(0).getSession().getDesyncMonitor().getMismatchCount() > 0);
        Assertions.assertTrue(machines.get(1).getSession().getDesyncMonitor().getMismatchCount() > 0);
    }

    @Test
    void a_peer_that_leaves_is_reported_at_the_same_game_loop_by_every_other_participant() throws IOException {
        LockstepPeer first = new LockstepPeer(0);
        LockstepPeer second = new LockstepPeer(1);
        LockstepPeer third = new LockstepPeer(2);
        InMemoryLockstepNetwork network = new InMemoryLockstepNetwork(List.of(first, second, third));
        List<LockstepGameScenario.Machine> machines = List.of(
                this.scenario.createMachine(first, network.getTransport(first), 3, LockstepGameScenario.SEED, PATROL_FRAME),
                this.scenario.createMachine(second, network.getTransport(second), 3, LockstepGameScenario.SEED, MOVE_FRAME),
                this.scenario.createMachine(third, network.getTransport(third), 3, LockstepGameScenario.SEED, THIRD_ORDER_FRAME)
        );

        machines.get(1).setLeave(LEAVE_FRAME, LEAVE_REASON);
        LockstepGameScenario.run(machines, FRAME_COUNT, () -> {
        });

        Assertions.assertEquals(machines.get(0).getFrameHashes(), machines.get(2).getFrameHashes());
        Assertions.assertEquals(LEAVE_FRAME, machines.get(1).getFrameHashes().size());
        Assertions.assertEquals(List.of(DEPARTURE_FRAME + " Peer 1 LEFT (" + LEAVE_REASON + ")"), machines.get(0).getDepartures());
        Assertions.assertEquals(machines.get(0).getDepartures(), machines.get(2).getDepartures());
        Assertions.assertEquals(List.of(), machines.get(1).getDepartures());
        Assertions.assertEquals(3, machines.get(0).getExecuted());
    }

    private @NotNull List<LockstepGameScenario.@NotNull Machine> createMachines(long firstSeed, long secondSeed) throws IOException {
        LockstepPeer first = new LockstepPeer(0);
        LockstepPeer second = new LockstepPeer(1);
        InMemoryLockstepNetwork network = new InMemoryLockstepNetwork(List.of(first, second));

        // Both peers play the scout's player, each machine's session with its own game's player object.
        return List.of(this.scenario.createMachine(first, network.getTransport(first), 2, firstSeed, PATROL_FRAME),
                       this.scenario.createMachine(second, network.getTransport(second), 2, secondSeed, MOVE_FRAME));
    }
}
