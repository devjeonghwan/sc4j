package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.beaconlink.protocol.test.ManualProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.CommunityScenario;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.LargeSimulation;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.SimulatedClient;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.community.ChatChannel;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.community.ChatLine;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.community.CommunityListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the delivery of chat lines in a channel of 64 members on the in-memory network at 10% datagram loss: at least 99.5% of the lines
 * reach every other member, each once.
 */
final class ClientCommunitySimulationTest {
    private static final @NotNull Logger LOGGER = Logger.of(ClientCommunitySimulationTest.class);
    private static final long RANDOM_SEED = 20261112L;
    private static final double LOSS_RATE = 0.10;
    private static final int PEER_COUNT = 40;
    private static final int MEMBER_COUNT = 64;
    private static final int NODES_PER_CLIENT = 2;
    private static final int LINES_PER_MEMBER = 2;
    private static final long JOIN_INTERVAL_MILLIS = 100L;
    private static final long JOIN_MILLIS = 90_000L;
    private static final long SPEAK_SPREAD_MILLIS = 30_000L;
    private static final long SETTLE_MILLIS = 30_000L;
    private static final long STEP_MILLIS = 500L;
    private static final double REQUIRED_RATIO = 0.995;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final @NotNull String CHANNEL = "Lobby";
    private static final @Nullable NatType @NotNull [] NAT_MIX = {null, NatType.PORT_RESTRICTED, NatType.ADDRESS_RESTRICTED, NatType.SYMMETRIC};

    /**
     * Counts the lines a member's listener gets, by text.
     */
    private static final class Recorder implements CommunityListener {
        private final @NotNull Map<@NotNull String, @NotNull Integer> lines;

        private Recorder() {
            this.lines = new HashMap<>();
        }

        @Override
        public void onChatLine(@NotNull ChatChannel channel, @NotNull ChatLine line) {
            this.lines.merge(line.getText(), 1, Integer::sum);
        }
    }

    @Test
    @LargeSimulation
    void lines_reach_the_members_of_a_sixty_four_member_channel_at_ten_percent_loss() {
        long wallStart = System.nanoTime();
        CommunityScenario scenario = new CommunityScenario(RANDOM_SEED, LOSS_RATE, PEER_COUNT);
        List<ChatChannel> channels = new ArrayList<>();
        List<Recorder> recorders = new ArrayList<>();

        for (int index = 0; index < MEMBER_COUNT; index++) {
            SimulatedClient member = scenario.addClient("Member" + index, NAT_MIX[index % NAT_MIX.length], NODES_PER_CLIENT);
            Recorder recorder = new Recorder();

            member.getClient().getCommunity().addListener(recorder);
            channels.add(member.getClient().getCommunity().joinChannel(CHANNEL));
            recorders.add(recorder);
            scenario.run(JOIN_INTERVAL_MILLIS);
        }

        long sentBefore = scenario.getNetwork().getSentCount();
        long joinMillis = scenario.runUntil(() -> channels.stream().allMatch(channel -> channel.getMembers().size() == MEMBER_COUNT), JOIN_MILLIS,
                                            STEP_MILLIS, "every member sees every member");
        long joinSent = scenario.getNetwork().getSentCount() - sentBefore;
        long stepCount = SPEAK_SPREAD_MILLIS / STEP_MILLIS;
        List<int[]> speeches = new ArrayList<>();

        for (int member = 0; member < MEMBER_COUNT; member++) {
            for (int line = 0; line < LINES_PER_MEMBER; line++) {
                speeches.add(new int[]{member, line, scenario.getRandom().nextInt((int) stepCount)});
            }
        }

        sentBefore = scenario.getNetwork().getSentCount();

        for (int step = 0; step < stepCount; step++) {
            for (int[] speech : speeches) {
                if (speech[2] == step) {
                    Assertions.assertTrue(channels.get(speech[0]).say("line " + speech[1] + " of member " + speech[0]));
                }
            }

            scenario.run(STEP_MILLIS);
        }

        scenario.run(SETTLE_MILLIS);

        long chatSent = scenario.getNetwork().getSentCount() - sentBefore;
        long expected = (long) MEMBER_COUNT * (MEMBER_COUNT - 1) * LINES_PER_MEMBER;
        long delivered = 0L;
        long repeated = 0L;

        for (int member = 0; member < MEMBER_COUNT; member++) {
            for (Map.Entry<String, Integer> line : recorders.get(member).lines.entrySet()) {
                if (!line.getKey().endsWith(" of member " + member)) {
                    delivered++;
                    repeated += line.getValue() - 1;
                }
            }
        }

        double ratio = (double) delivered / expected;

        LOGGER.info("Channel simulation: {0} peers, {1} members at {2} loss; members saw each other after {3} ms ({4} datagrams); {5} of {6} " +
                    "deliveries ({7}), {8} repeated; chat {9} datagrams; {10} sent, {11} lost, {12} filtered; {13} ms virtual, {14} ms wall",
                    PEER_COUNT, MEMBER_COUNT, LOSS_RATE, joinMillis, joinSent, delivered, expected, ratio, repeated, chatSent,
                    scenario.getNetwork().getSentCount(), scenario.getNetwork().getLostCount(), scenario.getNetwork().getFilteredCount(),
                    scenario.getNetwork().getClock().getEpochMillis() - ManualProtocolClock.START_MILLIS,
                    (System.nanoTime() - wallStart) / NANOS_PER_MILLI);

        Assertions.assertTrue(ratio >= REQUIRED_RATIO, "delivery ratio " + ratio);
        Assertions.assertEquals(0L, repeated, "a line is handed out once");
    }
}
