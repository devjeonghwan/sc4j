package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.beaconlink.channel.ChannelOffer;
import org.fogofwarlabs.sc4j.beaconlink.channel.EphemeralKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.MessageEncoder;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.SignalMessage;
import org.fogofwarlabs.sc4j.beaconlink.test.client.ClientScenario;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.LoopbackNat;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatEndpoint;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Tests the channels between Beacon.Link clients behind emulated NATs on 127.0.0.1: the offer and answer through a real node, the timed hole punching
 * for every pair of NAT types (direct where the NATs allow it, the failure where they do not), two connections of one account with each other and
 * with a third player, the local network behind one NAT, a participant no node knows, signals that do not come through a node, datagrams in both
 * directions, the keepalive past the NAT idle time, the idle close and the close.
 */
final class PeerChannelTest {
    private static final @NotNull ModIdentity IDENTITY = ClientScenario.createIdentity("1.0.0");
    private static final long CHANNEL_STEP_MILLIS = 100L;
    private static final long CONNECT_MILLIS = 15_000L;
    private static final long PAST_NAT_IDLE_MILLIS = LoopbackNat.IDLE_MILLIS + 15_000L;
    private static final long KEEPALIVE_STEP_MILLIS = 500L;

    private static boolean isSettled(@NotNull ParticipantChannel channel) {
        return channel.getState() != ChannelState.CONNECTING;
    }

    private static @NotNull String receiveText(@NotNull ParticipantChannel channel) {
        ByteBuffer buffer = ByteBuffer.allocate(ParticipantChannel.MAXIMUM_PAYLOAD_LENGTH);
        int length = channel.receive(buffer);

        return length == ParticipantChannel.NO_DATAGRAM ? "" : new String(buffer.array(), 0, length, StandardCharsets.UTF_8);
    }

    private static boolean sendText(@NotNull ParticipantChannel channel, @NotNull String text) {
        return channel.send(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }

    private final @NotNull ClientScenario scenario;

    PeerChannelTest() {
        this.scenario = new ClientScenario();
    }

    @AfterEach
    void closeScenario() {
        this.scenario.close();
    }

    private @NotNull BeaconLinkClient openBehind(@NotNull LoopbackNat nat, @NotNull PeerSocket node) throws IOException {
        NatEndpoint endpoint = nat.openEndpoint();

        return this.scenario.openClient(IDENTITY, List.of(ClientScenario.listed(node)), endpoint);
    }

    private void awaitRegistered(@NotNull BeaconLinkClient @NotNull ... clients) throws IOException {
        this.scenario.await(() -> {
            for (BeaconLinkClient client : clients) {
                if (client.getObservedAddress() == null) {
                    return false;
                }
            }

            return true;
        }, "the clients register");
    }

    /**
     * Opens two clients behind NATs of the given types and an open channel between them: index 0 is the first client's channel, index 1
     * the second's.
     */
    private @NotNull ParticipantChannel @NotNull [] openChannels(@NotNull LoopbackNat firstNat, @NotNull LoopbackNat secondNat) throws IOException {
        PeerSocket node = this.scenario.openNode(ClientScenario.createNodeConfiguration());
        BeaconLinkClient first = this.openBehind(firstNat, node);
        BeaconLinkClient second = this.openBehind(secondNat, node);

        this.awaitRegistered(first, second);

        ParticipantChannel firstChannel = first.openChannel(second.getParticipant());
        ParticipantChannel secondChannel = second.openChannel(first.getParticipant());

        this.awaitSettled(firstChannel, secondChannel);

        Assertions.assertEquals(ChannelState.OPEN, firstChannel.getState());
        Assertions.assertEquals(ChannelState.OPEN, secondChannel.getState());

        return new ParticipantChannel[]{firstChannel, secondChannel};
    }

    private void awaitSettled(@NotNull ParticipantChannel first, @NotNull ParticipantChannel second) throws IOException {
        this.scenario.advanceUntil(() -> isSettled(first) && isSettled(second), CONNECT_MILLIS, CHANNEL_STEP_MILLIS, "both channels settle");
    }

    @ParameterizedTest
    @CsvSource({
            "FULL_CONE, FULL_CONE, true",
            "FULL_CONE, ADDRESS_RESTRICTED, true",
            "FULL_CONE, PORT_RESTRICTED, true",
            "FULL_CONE, SYMMETRIC, true",
            "ADDRESS_RESTRICTED, ADDRESS_RESTRICTED, true",
            "ADDRESS_RESTRICTED, PORT_RESTRICTED, true",
            "ADDRESS_RESTRICTED, SYMMETRIC, true",
            "PORT_RESTRICTED, PORT_RESTRICTED, true",
            "PORT_RESTRICTED, SYMMETRIC, false",
            "SYMMETRIC, SYMMETRIC, false"
    })
    void channel_connects_directly_where_the_nat_pair_allows_it(@NotNull NatType firstType, @NotNull NatType secondType, boolean direct) throws IOException {
        PeerSocket node = this.scenario.openNode(ClientScenario.createNodeConfiguration());
        BeaconLinkClient first = this.openBehind(this.scenario.openNat(firstType), node);
        BeaconLinkClient second = this.openBehind(this.scenario.openNat(secondType), node);

        this.awaitRegistered(first, second);

        ParticipantChannel firstChannel = first.openChannel(second.getParticipant());
        ParticipantChannel secondChannel = second.openChannel(first.getParticipant());

        Assertions.assertSame(firstChannel, first.openChannel(second.getParticipant()));
        Assertions.assertEquals(ChannelState.CONNECTING, firstChannel.getState());

        this.awaitSettled(firstChannel, secondChannel);

        ChannelState expected = direct ? ChannelState.OPEN : ChannelState.CLOSED;
        ChannelFailure failure = direct ? null : ChannelFailure.PAIR_CANNOT_CONNECT;

        Assertions.assertEquals(expected, firstChannel.getState());
        Assertions.assertEquals(expected, secondChannel.getState());
        Assertions.assertEquals(failure, firstChannel.getFailure());
        Assertions.assertEquals(failure, secondChannel.getFailure());
        Assertions.assertEquals(second.getParticipant(), firstChannel.getPeer());
    }

    @Test
    void channels_connect_two_connections_of_one_account_with_each_other_and_with_a_third_player() throws IOException {
        PeerSocket node = this.scenario.openNode(ClientScenario.createNodeConfiguration());
        BeaconLinkClient launcher = this.openBehind(this.scenario.openNat(NatType.FULL_CONE), node);
        BeaconLinkClient game = this.scenario.openConnection(launcher, IDENTITY, List.of(ClientScenario.listed(node)));
        BeaconLinkClient friend = this.openBehind(this.scenario.openNat(NatType.PORT_RESTRICTED), node);

        this.awaitRegistered(launcher, game, friend);

        Assertions.assertEquals(launcher.getParticipant().getPlayer(), game.getParticipant().getPlayer());

        ParticipantChannel launcherToGame = launcher.openChannel(game.getParticipant());
        ParticipantChannel gameToLauncher = game.openChannel(launcher.getParticipant());
        ParticipantChannel launcherToFriend = launcher.openChannel(friend.getParticipant());
        ParticipantChannel friendToLauncher = friend.openChannel(launcher.getParticipant());
        ParticipantChannel gameToFriend = game.openChannel(friend.getParticipant());
        ParticipantChannel friendToGame = friend.openChannel(game.getParticipant());
        List<ParticipantChannel> channels = List.of(launcherToGame, gameToLauncher, launcherToFriend, friendToLauncher, gameToFriend, friendToGame);

        this.scenario.advanceUntil(() -> channels.stream().allMatch(PeerChannelTest::isSettled), CONNECT_MILLIS, CHANNEL_STEP_MILLIS,
                                   "every channel settles");

        for (ParticipantChannel channel : channels) {
            Assertions.assertEquals(ChannelState.OPEN, channel.getState(), channel.toString());
        }

        Assertions.assertEquals(game.getParticipant(), launcherToGame.getPeer());
        Assertions.assertTrue(sendText(launcherToGame, "to my game"));
        Assertions.assertTrue(sendText(gameToLauncher, "to my launcher"));
        Assertions.assertTrue(sendText(friendToLauncher, "to the launcher"));
        Assertions.assertTrue(sendText(friendToGame, "to the game"));
        this.scenario.await(() -> receiveText(gameToLauncher).equals("to my game"), "the game gets its launcher's datagram");
        this.scenario.await(() -> receiveText(launcherToGame).equals("to my launcher"), "the launcher gets its game's datagram");
        this.scenario.await(() -> receiveText(launcherToFriend).equals("to the launcher"), "the launcher gets the friend's datagram");
        this.scenario.await(() -> receiveText(gameToFriend).equals("to the game"), "the game gets the friend's datagram");
    }

    @Test
    void channel_connects_on_the_local_network_of_one_symmetric_nat() throws IOException {
        PeerSocket node = this.scenario.openNode(ClientScenario.createNodeConfiguration());
        LoopbackNat nat = this.scenario.openNat(NatType.SYMMETRIC);
        BeaconLinkClient first = this.openBehind(nat, node);
        BeaconLinkClient second = this.openBehind(nat, node);

        this.awaitRegistered(first, second);

        ParticipantChannel firstChannel = first.openChannel(second.getParticipant());
        ParticipantChannel secondChannel = second.openChannel(first.getParticipant());

        this.awaitSettled(firstChannel, secondChannel);

        Assertions.assertEquals(ChannelState.OPEN, firstChannel.getState());
        Assertions.assertEquals(ChannelState.OPEN, secondChannel.getState());
    }

    @Test
    void channel_to_a_participant_no_node_knows_fails_as_not_found() throws IOException {
        PeerSocket node = this.scenario.openNode(ClientScenario.createNodeConfiguration());
        BeaconLinkClient client = this.openBehind(this.scenario.openNat(NatType.FULL_CONE), node);

        this.awaitRegistered(client);

        IdentityKeyPair stranger = IdentityKeyPair.generate(new SecureRandom());

        while (stranger.getPublicKey().compareTo(client.getKey()) < 0) {
            stranger = IdentityKeyPair.generate(new SecureRandom());
        }

        ParticipantChannel channel = client.openChannel(LinkParticipant.of(LinkPlayer.of(stranger.getPublicKey().getBytes()), 1L));

        this.scenario.advanceUntil(() -> isSettled(channel), CONNECT_MILLIS, CHANNEL_STEP_MILLIS, "the channel fails");

        Assertions.assertEquals(ChannelState.CLOSED, channel.getState());
        Assertions.assertEquals(ChannelFailure.PEER_NOT_FOUND, channel.getFailure());
        Assertions.assertThrows(IllegalArgumentException.class, () -> client.openChannel(client.getParticipant()));
    }

    @Test
    void signal_that_does_not_come_through_a_node_is_dropped() throws IOException {
        PeerSocket node = this.scenario.openNode(ClientScenario.createNodeConfiguration());
        NatEndpoint endpoint = this.scenario.openNat(NatType.FULL_CONE).openEndpoint();
        BeaconLinkClient client = this.scenario.openClient(IDENTITY, List.of(ClientScenario.listed(node)), endpoint);

        this.awaitRegistered(client);

        MessageEncoder sender = new MessageEncoder(IdentityKeyPair.generate(new SecureRandom()), this.scenario.getClock(),
                                                   new SplittableRandom(ClientScenario.RANDOM_SEED));
        byte[] offer = new ChannelOffer(1L, 1L, EphemeralKeyPair.generate(new SecureRandom()).getPublicKey(), List.of(endpoint.getLocalAddress()))
                .write();
        int dropped = client.getDroppedCount();

        try (DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)) {
            channel.bind(new InetSocketAddress(ClientScenario.LOOPBACK, 0));
            SignalMessage signal = new SignalMessage(client.getKey(), client.getParticipant().getConnection(), offer);

            channel.send(ByteBuffer.wrap(sender.encodeRequest(signal, 1L)), endpoint.getLocalAddress());
            channel.send(ByteBuffer.wrap(new byte[]{'B', 'L', 'C', 'H', 0, 0, 0, 0, 0, 0, 0, 1, 1}), endpoint.getLocalAddress());
        }

        this.scenario.await(() -> client.getDroppedCount() == dropped + 2, "the signal and the channel datagram are dropped");
    }

    @Test
    void channel_carries_datagrams_in_both_directions() throws IOException {
        ParticipantChannel[] channels = this.openChannels(this.scenario.openNat(NatType.PORT_RESTRICTED),
                                                          this.scenario.openNat(NatType.ADDRESS_RESTRICTED));
        String largest = "x".repeat(ParticipantChannel.MAXIMUM_PAYLOAD_LENGTH);

        Assertions.assertEquals(ParticipantChannel.NO_DATAGRAM, channels[1].receive(ByteBuffer.allocate(1)));
        Assertions.assertTrue(sendText(channels[0], "first"));
        Assertions.assertTrue(sendText(channels[0], largest));
        Assertions.assertTrue(sendText(channels[1], "answer"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> sendText(channels[0], largest + "x"));

        this.scenario.await(() -> receiveText(channels[0]).equals("answer"), "the answer arrives");

        Assertions.assertEquals("first", receiveText(channels[1]));
        Assertions.assertEquals(largest, receiveText(channels[1]));
        Assertions.assertEquals("", receiveText(channels[1]));
        Assertions.assertNotEquals(ParticipantChannel.UNKNOWN_DELAY, channels[0].getDelayMillis());
    }

    @Test
    void keepalive_holds_the_channel_open_past_the_nat_idle_time() throws IOException {
        ParticipantChannel[] channels = this.openChannels(this.scenario.openNat(NatType.PORT_RESTRICTED),
                                                          this.scenario.openNat(NatType.PORT_RESTRICTED));

        this.scenario.advance(PAST_NAT_IDLE_MILLIS);

        Assertions.assertEquals(ChannelState.OPEN, channels[0].getState());
        Assertions.assertEquals(ChannelState.OPEN, channels[1].getState());
        Assertions.assertTrue(sendText(channels[1], "still here"));

        this.scenario.await(() -> receiveText(channels[0]).equals("still here"), "a datagram passes after the NAT idle time");
    }

    @Test
    void channel_fails_as_lost_when_nothing_arrives_for_the_idle_time() throws IOException {
        LoopbackNat silentNat = this.scenario.openNat(NatType.FULL_CONE);
        ParticipantChannel[] channels = this.openChannels(this.scenario.openNat(NatType.FULL_CONE), silentNat);

        silentNat.setLossRate(1.0);

        this.scenario.advanceUntil(() -> channels[0].getState() == ChannelState.CLOSED, CONNECT_MILLIS + CONNECT_MILLIS, KEEPALIVE_STEP_MILLIS,
                                   "the channel fails");

        Assertions.assertEquals(ChannelFailure.CONNECTION_LOST, channels[0].getFailure());
        Assertions.assertFalse(sendText(channels[0], "too late"));
    }

    @Test
    void closing_a_channel_closes_the_other_side() throws IOException {
        ParticipantChannel[] channels = this.openChannels(this.scenario.openNat(NatType.ADDRESS_RESTRICTED),
                                                          this.scenario.openNat(NatType.FULL_CONE));

        channels[0].close();
        channels[0].close();

        Assertions.assertEquals(ChannelState.CLOSED, channels[0].getState());
        Assertions.assertNull(channels[0].getFailure());

        this.scenario.await(() -> channels[1].getState() == ChannelState.CLOSED, "the other side closes");

        Assertions.assertEquals(ChannelFailure.CLOSED_BY_PEER, channels[1].getFailure());
    }
}
