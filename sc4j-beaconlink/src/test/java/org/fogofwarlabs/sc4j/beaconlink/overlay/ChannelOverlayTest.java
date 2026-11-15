package org.fogofwarlabs.sc4j.beaconlink.overlay;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.client.BeaconLinkClient;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.test.client.ClientScenario;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.DatagramLink;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the overlay over participant channels on 127.0.0.1 behind emulated NATs: datagrams go by participant and port, a datagram of a port
 * nobody opened or to a participant not added is dropped, and a removed participant's channel closes. The engine's lobby and lockstep
 * transport over the overlay are tested by {@link OverlayLobbyTest}.
 */
final class ChannelOverlayTest {
    private static final @NotNull ModIdentity IDENTITY = ClientScenario.createIdentity("1.0.0");
    private static final long CHANNEL_STEP_MILLIS = 100L;
    private static final long CONNECT_MILLIS = 20_000L;
    private static final int TEST_PORT = 9;

    private static @NotNull String receiveText(@NotNull DatagramLink<@NotNull LinkParticipant> port, @NotNull LinkParticipant expectedSender) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(LinkOverlay.MAXIMUM_DATAGRAM_LENGTH);
        LinkParticipant sender = port.receive(buffer);

        if (sender == null) {
            return "";
        }

        Assertions.assertEquals(expectedSender, sender, "the datagram's sender");

        return new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8);
    }

    private static void sendText(@NotNull DatagramLink<@NotNull LinkParticipant> port, @NotNull LinkParticipant target, @NotNull String text) throws IOException {
        port.send(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)), target);
    }


    private final @NotNull ClientScenario scenario;

    ChannelOverlayTest() {
        this.scenario = new ClientScenario();
    }

    @AfterEach
    void closeScenario() {
        this.scenario.close();
    }

    private @NotNull List<@NotNull BeaconLinkClient> openClients(@NotNull NatType @NotNull ... types) throws IOException {
        PeerSocket node = this.scenario.openNode(ClientScenario.createNodeConfiguration());
        List<BeaconLinkClient> clients = new ArrayList<>();

        for (NatType type : types) {
            BeaconLinkClient client = this.scenario.openClient(IDENTITY, List.of(ClientScenario.listed(node)),
                                                               this.scenario.openNat(type).openEndpoint());

            this.scenario.await(() -> client.getObservedAddress() != null, "the client registers");
            clients.add(client);
        }

        return clients;
    }

    /**
     * Adds every client to every other client's overlay and waits until every channel is open.
     */
    private void connectMesh(@NotNull List<@NotNull BeaconLinkClient> clients) throws IOException {
        List<ParticipantChannel> channels = new ArrayList<>();

        for (BeaconLinkClient client : clients) {
            for (BeaconLinkClient other : clients) {
                if (other != client) {
                    channels.add(client.getOverlay().addParticipant(other.getParticipant()));
                }
            }
        }

        this.scenario.advanceUntil(() -> channels.stream().allMatch(channel -> channel.getState() == ChannelState.OPEN), CONNECT_MILLIS,
                                   CHANNEL_STEP_MILLIS, "every channel of the mesh opens");
    }

    @Test
    void ports_carry_datagrams_by_participant_and_drop_datagrams_nobody_waits_for() throws IOException {
        List<BeaconLinkClient> clients = this.openClients(NatType.FULL_CONE, NatType.PORT_RESTRICTED, NatType.FULL_CONE);
        BeaconLinkClient first = clients.get(0);
        BeaconLinkClient second = clients.get(1);
        LinkParticipant stranger = clients.get(2).getParticipant();

        this.connectMesh(List.of(first, second));

        DatagramLink<LinkParticipant> firstPort = first.getOverlay().openPort(TEST_PORT);
        DatagramLink<LinkParticipant> firstOther = first.getOverlay().openPort(TEST_PORT + 1);
        DatagramLink<LinkParticipant> secondPort = second.getOverlay().openPort(TEST_PORT);

        sendText(firstOther, second.getParticipant(), "to a port nobody opened");
        sendText(firstPort, stranger, "to a participant not added");
        sendText(firstPort, second.getParticipant(), "over the overlay");
        this.scenario.advanceUntil(() -> {
            try {
                return receiveText(secondPort, first.getParticipant()).equals("over the overlay");
            } catch (IOException exception) {
                throw Exceptions.newUncheckedIOException(exception);
            }
        }, CONNECT_MILLIS, CHANNEL_STEP_MILLIS, "the datagram arrives at the port");

        sendText(secondPort, first.getParticipant(), "back");
        this.scenario.advanceUntil(() -> {
            try {
                return receiveText(firstPort, second.getParticipant()).equals("back");
            } catch (IOException exception) {
                throw Exceptions.newUncheckedIOException(exception);
            }
        }, CONNECT_MILLIS, CHANNEL_STEP_MILLIS, "the answer arrives");

        Assertions.assertEquals("", receiveText(secondPort, first.getParticipant()), "nothing of the other port");
        Assertions.assertEquals(List.of(second.getParticipant()), first.getOverlay().getParticipants());
        Assertions.assertThrows(IllegalStateException.class, () -> first.getOverlay().openPort(TEST_PORT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> first.getOverlay().openPort(LinkOverlay.MAXIMUM_PORT + 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> first.getOverlay().addParticipant(first.getParticipant()));

        ParticipantChannel secondChannel = second.getOverlay().getChannel(first.getParticipant());

        Assertions.assertNotNull(secondChannel);
        first.getOverlay().removeParticipant(second.getParticipant());
        this.scenario.advanceUntil(() -> secondChannel.getState() == ChannelState.CLOSED, CONNECT_MILLIS, CHANNEL_STEP_MILLIS,
                                   "the removed participant's channel closes");
        Assertions.assertEquals(ChannelFailure.CLOSED_BY_PEER, secondChannel.getFailure());
        Assertions.assertNull(first.getOverlay().getChannel(second.getParticipant()));

        firstPort.close();
        Assertions.assertThrows(IOException.class, () -> sendText(firstPort, second.getParticipant(), "closed"));
        Assertions.assertDoesNotThrow(() -> first.getOverlay().openPort(TEST_PORT).close(), "a closed port opens again");
    }
}
