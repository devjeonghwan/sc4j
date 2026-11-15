package org.fogofwarlabs.sc4j.launcher.nodes;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.beaconlink.peer.MeshPeer;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.Envelope;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.MessageEncoder;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.PongMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests the launcher's node ping against a node started in the test JVM on 127.0.0.1: a reachable node with its version and observed
 * address, the pinned key of a hand-written node list entry ignored, a dead node timed out after every attempt, a lost first ping answered
 * after the resend, and forged or unrelated answers dropped.
 */
final class NodePingerTest {
    private static final @NotNull InetAddress LOOPBACK = InetAddress.ofLiteral("127.0.0.1");
    private static final long RANDOM_SEED = 20261108L;
    private static final long ATTEMPT_MILLIS = 300L;
    private static final int ATTEMPTS = 3;
    private static final long POLL_MILLIS = 5L;
    private static final int TEST_SECONDS = 30;
    private static final int RECEIVE_LENGTH = Envelope.MAXIMUM_DATAGRAM_LENGTH + 1;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    /**
     * The number of the ping that the responder answers; it drops the pings before it.
     */
    private static final int ANSWERED_PING = 2;

    private final @NotNull List<@NotNull AutoCloseable> resources;
    private final @NotNull NodePinger pinger;

    NodePingerTest() {
        this.resources = new ArrayList<>();
        this.pinger    = new NodePinger(ATTEMPT_MILLIS, ATTEMPTS, ProtocolClock.SYSTEM);
    }

    @AfterEach
    void closeResources() throws Exception {
        for (AutoCloseable resource : this.resources.reversed()) {
            resource.close();
        }
    }

    @Test
    @Timeout(TEST_SECONDS)
    void ping_should_report_a_reachable_node_with_its_version_and_the_observed_address() throws IOException {
        PeerSocket node = this.startNode();
        NodePingResult result = this.pinger.ping(List.of(createListedNode(node, null))).getFirst();
        InetSocketAddress observed = result.getObservedAddress();

        Assertions.assertEquals(NodePingStatus.REACHABLE, result.getStatus());
        Assertions.assertEquals(MeshPeer.SOFTWARE, result.getSoftware());
        Assertions.assertNotNull(observed);
        Assertions.assertEquals(LOOPBACK, observed.getAddress());
        Assertions.assertTrue(observed.getPort() > 0);
        Assertions.assertTrue(result.getRoundTripMillis() >= 0L && result.getRoundTripMillis() < ATTEMPT_MILLIS);
    }

    @Test
    @Timeout(TEST_SECONDS)
    void ping_should_ignore_a_pinned_key() throws IOException {
        PeerSocket node = this.startNode();
        IdentityKey otherKey = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();
        List<NodePingResult> results = this.pinger.ping(List.of(createListedNode(node, node.getPeer().getKey()), createListedNode(node, otherKey)));

        Assertions.assertEquals(NodePingStatus.REACHABLE, results.get(0).getStatus());
        Assertions.assertEquals(NodePingStatus.REACHABLE, results.get(1).getStatus());
    }

    @Test
    @Timeout(TEST_SECONDS)
    void ping_should_time_out_a_dead_node_after_every_attempt_and_still_answer_the_live_one() throws IOException {
        PeerSocket node = this.startNode();
        DatagramSocket dead = this.open(new DatagramSocket(new InetSocketAddress(LOOPBACK, 0)));
        ConfiguredNode deadNode = ConfiguredNode.of(LOOPBACK.getHostAddress(), dead.getLocalPort(), null);
        long startNanos = System.nanoTime();
        List<NodePingResult> results = this.pinger.ping(List.of(deadNode, createListedNode(node, null)));
        long elapsedMillis = (System.nanoTime() - startNanos) / NANOS_PER_MILLI;

        Assertions.assertEquals(NodePingStatus.NOT_ANSWERING, results.get(0).getStatus());
        Assertions.assertEquals(NodePingResult.NO_ROUND_TRIP, results.get(0).getRoundTripMillis());
        Assertions.assertNull(results.get(0).getSoftware());
        Assertions.assertEquals(NodePingStatus.REACHABLE, results.get(1).getStatus());
        Assertions.assertTrue(elapsedMillis >= ATTEMPT_MILLIS * ATTEMPTS, Long.toString(elapsedMillis));
        Assertions.assertEquals(deadNode, results.get(0).getNode());
    }

    @Test
    @Timeout(TEST_SECONDS)
    void ping_should_resend_and_drop_forged_and_unrelated_answers() throws IOException {
        IdentityKeyPair responderKey = IdentityKeyPair.generate(new SecureRandom());
        DatagramSocket responder = this.open(new DatagramSocket(new InetSocketAddress(LOOPBACK, 0)));
        AtomicInteger received = new AtomicInteger();

        this.startResponder(responder, responderKey, received);

        NodePingResult result = this.pinger.ping(List.of(ConfiguredNode.of(LOOPBACK.getHostAddress(), responder.getLocalPort(), null))).getFirst();

        Assertions.assertEquals(NodePingStatus.REACHABLE, result.getStatus());
        Assertions.assertEquals("responder", result.getSoftware());
        Assertions.assertEquals(ANSWERED_PING, received.get());
    }

    /**
     * Starts a node on 127.0.0.1 polled by a thread of the test.
     */
    private @NotNull PeerSocket startNode() throws IOException {
        PeerSocket node = PeerSocket.open(PeerConfiguration.createDefault(), IdentityKeyPair.generate(new SecureRandom()),
                                          new InetSocketAddress(LOOPBACK, 0), ProtocolClock.SYSTEM, new SplittableRandom(RANDOM_SEED));
        Thread thread = new Thread(() -> poll(node), "test-node");

        this.resources.add(node);
        this.resources.add(() -> {
            node.stop();
            thread.join();
        });
        thread.setDaemon(true);
        thread.start();

        return node;
    }

    private static void poll(@NotNull PeerSocket node) {
        try {
            while (!node.isStopped()) {
                node.poll(POLL_MILLIS);
            }
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }
    }

    /**
     * Starts a responder that drops the first ping and answers the second one first with a forged pong, then with a pong of another request,
     * then with the valid pong.
     */
    private void startResponder(@NotNull DatagramSocket socket, @NotNull IdentityKeyPair key, @NotNull AtomicInteger received) {
        MessageEncoder encoder = new MessageEncoder(key, ProtocolClock.SYSTEM, new SplittableRandom(RANDOM_SEED));
        IdentityKeyPair forgerKey = IdentityKeyPair.generate(new SecureRandom());
        MessageEncoder forger = new MessageEncoder(forgerKey, ProtocolClock.SYSTEM, new SplittableRandom(RANDOM_SEED));
        Thread thread = new Thread(() -> respond(socket, encoder, forger, received), "test-responder");

        this.resources.add(() -> {
            socket.close();
            thread.join();
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void respond(@NotNull DatagramSocket socket, @NotNull MessageEncoder encoder, @NotNull MessageEncoder forger,
                                @NotNull AtomicInteger received) {
        DatagramPacket packet = new DatagramPacket(new byte[RECEIVE_LENGTH], RECEIVE_LENGTH);

        try {
            while (received.get() < ANSWERED_PING) {
                packet.setLength(RECEIVE_LENGTH);
                socket.receive(packet);

                Envelope request = Envelope.decode(packet.getData(), packet.getLength()).getEnvelope();
                InetSocketAddress source = (InetSocketAddress) packet.getSocketAddress();

                if (request == null || received.incrementAndGet() < ANSWERED_PING) {
                    continue;
                }

                PongMessage pong = new PongMessage(source, "responder");
                byte[] forged = encoder.encodeReply(pong, request.getRequestNumber());

                forged[forged.length - 1] ^= 1;
                send(socket, forged, source);
                send(socket, encoder.encodeReply(pong, request.getRequestNumber() + 1L), source);
                send(socket, forger.encodeReply(new PongMessage(source, "forger"), request.getRequestNumber() + 2L), source);
                send(socket, encoder.encodeReply(pong, request.getRequestNumber()), source);
            }
        } catch (SocketException exception) {
            // The test closed the socket.
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }
    }

    private static void send(@NotNull DatagramSocket socket, byte @NotNull [] datagram, @NotNull InetSocketAddress target) throws IOException {
        socket.send(new DatagramPacket(datagram, datagram.length, target));
    }

    private @NotNull DatagramSocket open(@NotNull DatagramSocket socket) {
        this.resources.add(() -> IgnoreException.execute(
                socket::close
        ));

        return socket;
    }

    private static @NotNull ConfiguredNode createListedNode(@NotNull PeerSocket node, @Nullable IdentityKey key) throws IOException {
        return ConfiguredNode.of(LOOPBACK.getHostAddress(), node.getLocalAddress().getPort(), key);
    }
}
