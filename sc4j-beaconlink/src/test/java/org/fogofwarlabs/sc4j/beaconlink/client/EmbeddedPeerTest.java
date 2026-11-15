package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.peer.DropReason;
import org.fogofwarlabs.sc4j.beaconlink.peer.MeshPeer;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.fogofwarlabs.sc4j.beaconlink.test.client.ClientScenario;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.UdpDatagramEndpoint;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.List;

/**
 * Tests a Beacon.Link client with an embedded peer on its one socket in one JVM: the peer federates with the client's node at the client's
 * address, serves other clients' registrations, signals and rooms there, and its room reaches the clients of the federated node, while the
 * client itself stays registered at its node.
 */
final class EmbeddedPeerTest {
    private static final @NotNull ModIdentity IDENTITY = ClientScenario.createIdentity("1.0.0");
    private static final @NotNull RoomSettings SETTINGS = new RoomSettings("Embedded game", "Ashen Ford", 1, 4);
    private static final long GOSSIP_MILLIS = 1_000L;
    private static final long FEDERATION_MILLIS = 30_000L;

    private static boolean isRegistered(@NotNull BeaconLinkClient client) {
        return client.getNodes().stream().allMatch(node -> node.getState() == LinkNodeState.REGISTERED);
    }

    private static @NotNull ConfiguredNode listedAt(@NotNull InetSocketAddress address) {
        return ConfiguredNode.parse("127.0.0.1:" + address.getPort(), null);
    }

    private final @NotNull ClientScenario scenario;
    private final @NotNull PeerConfiguration configuration;

    EmbeddedPeerTest() {
        this.scenario      = new ClientScenario();
        this.configuration = ClientScenario.createNodeBuilder().setGossipIntervalMillis(GOSSIP_MILLIS).build();
    }

    @AfterEach
    void closeScenario() {
        this.scenario.close();
    }

    /**
     * Opens a client at a node with an embedded peer on the client's socket and waits until the peer and the node federate.
     */
    private @NotNull BeaconLinkClient openHost(@NotNull PeerSocket node, @NotNull UdpDatagramEndpoint endpoint) throws IOException {
        BeaconLinkClient host = this.scenario.openClient(IDENTITY, List.of(ClientScenario.listed(node)), endpoint);
        MeshPeer peer = host.attachPeer(this.configuration, IdentityKeyPair.generate(new SecureRandom()));
        NodeEndpoint peerEndpoint = new NodeEndpoint(peer.getKey(), endpoint.getLocalAddress());
        NodeEndpoint nodeEndpoint = new NodeEndpoint(node.getPeer().getKey(), node.getLocalAddress());

        this.scenario.advanceUntil(() -> isRegistered(host) && node.getPeer().getLiveNodes().contains(peerEndpoint) &&
                                         peer.getLiveNodes().contains(nodeEndpoint),
                                   FEDERATION_MILLIS, "the embedded peer and the node federate");

        return host;
    }

    @Test
    void embedded_peer_federates_at_the_address_of_its_client() throws IOException {
        PeerSocket node = this.scenario.openNode(this.configuration);
        UdpDatagramEndpoint endpoint = UdpDatagramEndpoint.open(new InetSocketAddress(ClientScenario.LOOPBACK, 0));
        BeaconLinkClient host = this.openHost(node, endpoint);
        MeshPeer peer = host.getPeer();

        Assertions.assertNotNull(peer);
        Assertions.assertNotEquals(host.getKey(), peer.getKey());
        Assertions.assertEquals(endpoint.getLocalAddress(), host.getObservedAddress());
        Assertions.assertEquals(List.of(new NodeEndpoint(peer.getKey(), endpoint.getLocalAddress())), node.getPeer().getLiveNodes());
        Assertions.assertEquals(0, host.getDroppedCount());
        Assertions.assertEquals(0L, peer.getStatistics().getDroppedCount(DropReason.UNEXPECTED_REPLY));
    }

    @Test
    void clients_meet_through_the_embedded_peer_of_another_client() throws IOException {
        PeerSocket node = this.scenario.openNode(this.configuration);
        UdpDatagramEndpoint endpoint = UdpDatagramEndpoint.open(new InetSocketAddress(ClientScenario.LOOPBACK, 0));
        BeaconLinkClient host = this.openHost(node, endpoint);
        List<ConfiguredNode> atPeer = List.of(listedAt(endpoint.getLocalAddress()));
        BeaconLinkClient first = this.scenario.openClient(IDENTITY, atPeer);
        BeaconLinkClient second = this.scenario.openClient(IDENTITY, atPeer);

        this.scenario.await(() -> isRegistered(first) && isRegistered(second), "the clients register at the embedded peer");

        ParticipantChannel firstChannel = first.openChannel(second.getParticipant());
        ParticipantChannel secondChannel = second.openChannel(first.getParticipant());

        this.scenario.advanceUntil(() -> firstChannel.getState() == ChannelState.OPEN && secondChannel.getState() == ChannelState.OPEN,
                                   FEDERATION_MILLIS, "the channel opens through signals of the embedded peer");

        Assertions.assertTrue(isRegistered(host));
    }

    @Test
    void room_hosted_at_an_embedded_peer_reaches_the_clients_of_the_federated_node() throws IOException {
        PeerSocket node = this.scenario.openNode(this.configuration);
        UdpDatagramEndpoint endpoint = UdpDatagramEndpoint.open(new InetSocketAddress(ClientScenario.LOOPBACK, 0));
        BeaconLinkClient host = this.openHost(node, endpoint);
        BeaconLinkClient player = this.scenario.openClient(IDENTITY, List.of(listedAt(endpoint.getLocalAddress())));
        BeaconLinkClient searcher = this.scenario.openClient(IDENTITY, List.of(ClientScenario.listed(node)));
        HostedRoom room = player.publishRoom(SETTINGS);

        this.scenario.await(() -> room.getState() == HostedRoomState.PUBLISHED && isRegistered(searcher), "the room is published at the peer");
        this.scenario.advanceUntil(() -> this.findRooms(searcher).stream().anyMatch(found -> found.getHost().equals(player.getParticipant())),
                                   FEDERATION_MILLIS, "the room reaches the node through its topic");

        Assertions.assertTrue(isRegistered(host));
    }

    private @NotNull List<@NotNull FoundRoom> findRooms(@NotNull BeaconLinkClient client) {
        RoomSearch search = client.searchRooms();

        try {
            this.scenario.await(search::isComplete, "the search completes");
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }

        return search.getRooms();
    }
}
