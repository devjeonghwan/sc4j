package org.fogofwarlabs.sc4j.beaconlink.test.nat;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.beaconlink.protocol.test.ManualProtocolClock;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

/**
 * Tests the loopback NAT emulator: the mapping and filtering of each NAT type, the observed public address, idle expiry, injected loss and
 * delay. Every socket binds 127.0.0.1.
 */
final class LoopbackNatTest {
    private static final @NotNull InetAddress LOOPBACK = InetAddress.ofLiteral("127.0.0.1");
    private static final long RANDOM_SEED = 20261108L;
    private static final long WAIT_MILLIS = 5_000L;
    private static final long DELAY_MILLIS = 200L;
    private static final @NotNull String NAT_HOST = "nat";
    private static final @NotNull String SERVER_HOST = "server";

    /**
     * Waits until the endpoint receives a datagram with the given text, and returns its source.
     */
    private static @NotNull SocketAddress awaitReceive(@NotNull NatEndpoint endpoint, @NotNull String text) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MILLIS);
        ByteBuffer buffer = ByteBuffer.allocate(64);

        while (System.nanoTime() < deadline) {
            buffer.clear();

            InetSocketAddress source = endpoint.receive(buffer);

            if (source != null) {
                Assertions.assertEquals(text, new String(buffer.array(), 0, buffer.position(), StandardCharsets.US_ASCII));

                return source;
            }
        }

        throw Exceptions.newAssertionError("No datagram arrived in time: {0}", text);
    }

    /**
     * Pumps the NAT until the loopback datagram has surely reached a public socket.
     */
    private static void awaitPumped(@NotNull LoopbackNat nat, @NotNull NatEndpoint endpoint) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DELAY_MILLIS);

        while (System.nanoTime() < deadline) {
            Assertions.assertNull(endpoint.receive(ByteBuffer.allocate(64)));
        }

        nat.pump();
    }

    private static @NotNull ByteBuffer text(@NotNull String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.US_ASCII));
    }

    private final @NotNull ManualProtocolClock clock;
    private final @NotNull LoopbackHosts hosts;
    private final @NotNull List<@NotNull DatagramChannel> outsideChannels;

    LoopbackNatTest() {
        this.clock           = new ManualProtocolClock();
        this.hosts           = new LoopbackHosts();
        this.outsideChannels = new ArrayList<>();
    }

    @AfterEach
    void closeOutsideChannels() {
        for (DatagramChannel channel : this.outsideChannels) {
            IgnoreException.execute(
                    channel::close
            );
        }
    }

    @Test
    void full_cone_lets_any_outside_socket_reach_the_mapping() throws IOException {
        try (LoopbackNat nat = this.openNat(NatType.FULL_CONE)) {
            NatEndpoint inside = nat.openEndpoint();
            DatagramChannel server = this.openOutside(SERVER_HOST);
            DatagramChannel stranger = this.openOutside("stranger");
            InetSocketAddress mapped = this.sendOut(inside, server);

            Assertions.assertEquals(nat.getPublicAddresses(), List.of(mapped));
            Assertions.assertNotEquals(inside.getLocalAddress(), mapped);

            stranger.send(text("in"), mapped);

            Assertions.assertEquals(stranger.getLocalAddress(), awaitReceive(inside, "in"));
        }
    }

    @Test
    void address_restricted_lets_only_contacted_hosts_reach_the_mapping() throws IOException {
        try (LoopbackNat nat = this.openNat(NatType.ADDRESS_RESTRICTED)) {
            NatEndpoint inside = nat.openEndpoint();
            DatagramChannel server = this.openOutside(SERVER_HOST);
            DatagramChannel serverOtherPort = this.openOutside(SERVER_HOST);
            DatagramChannel stranger = this.openOutside("stranger");
            InetSocketAddress mapped = this.sendOut(inside, server);

            stranger.send(text("blocked"), mapped);
            serverOtherPort.send(text("in"), mapped);

            Assertions.assertEquals(serverOtherPort.getLocalAddress(), awaitReceive(inside, "in"));
            Assertions.assertEquals(1, nat.getFilteredCount());
        }
    }

    @Test
    void port_restricted_lets_only_contacted_addresses_reach_the_mapping() throws IOException {
        try (LoopbackNat nat = this.openNat(NatType.PORT_RESTRICTED)) {
            NatEndpoint inside = nat.openEndpoint();
            DatagramChannel server = this.openOutside(SERVER_HOST);
            DatagramChannel serverOtherPort = this.openOutside(SERVER_HOST);
            InetSocketAddress mapped = this.sendOut(inside, server);

            serverOtherPort.send(text("blocked"), mapped);
            server.send(text("in"), mapped);

            Assertions.assertEquals(server.getLocalAddress(), awaitReceive(inside, "in"));
            Assertions.assertEquals(1, nat.getFilteredCount());
        }
    }

    @Test
    void symmetric_opens_a_mapping_per_destination() throws IOException {
        try (LoopbackNat nat = this.openNat(NatType.SYMMETRIC)) {
            NatEndpoint inside = nat.openEndpoint();
            DatagramChannel first = this.openOutside(SERVER_HOST);
            DatagramChannel second = this.openOutside(SERVER_HOST);
            InetSocketAddress firstMapped = this.sendOut(inside, first);
            InetSocketAddress secondMapped = this.sendOut(inside, second);

            Assertions.assertNotEquals(firstMapped, secondMapped);
            Assertions.assertEquals(List.of(firstMapped, secondMapped), nat.getPublicAddresses());
            Assertions.assertEquals(firstMapped, this.sendOut(inside, first));

            second.send(text("blocked"), firstMapped);
            first.send(text("in"), firstMapped);

            Assertions.assertEquals(first.getLocalAddress(), awaitReceive(inside, "in"));
            Assertions.assertEquals(1, nat.getFilteredCount());
        }
    }

    @Test
    void idle_mapping_closes_and_the_next_datagram_opens_a_new_one() throws IOException {
        try (LoopbackNat nat = this.openNat(NatType.PORT_RESTRICTED)) {
            NatEndpoint inside = nat.openEndpoint();
            DatagramChannel server = this.openOutside(SERVER_HOST);
            InetSocketAddress mapped = this.sendOut(inside, server);

            this.clock.advance(LoopbackNat.IDLE_MILLIS + 1L);
            server.send(text("late"), mapped);

            Assertions.assertNull(inside.receive(ByteBuffer.allocate(64)));
            Assertions.assertEquals(List.of(), nat.getPublicAddresses());
            Assertions.assertNotEquals(mapped, this.sendOut(inside, server));
        }
    }

    @Test
    void loss_and_delay_are_injected() throws IOException {
        try (LoopbackNat nat = this.openNat(NatType.FULL_CONE)) {
            NatEndpoint inside = nat.openEndpoint();
            DatagramChannel server = this.openOutside(SERVER_HOST);
            InetSocketAddress mapped = this.sendOut(inside, server);

            nat.setDelayMillis(DELAY_MILLIS);
            server.send(text("delayed"), mapped);
            awaitPumped(nat, inside);

            Assertions.assertNull(inside.receive(ByteBuffer.allocate(64)));

            this.clock.advance(DELAY_MILLIS);

            Assertions.assertEquals(server.getLocalAddress(), awaitReceive(inside, "delayed"));

            nat.setLossRate(1.0);
            inside.send(text("lost"), (InetSocketAddress) server.getLocalAddress());
            server.send(text("lost"), mapped);
            awaitPumped(nat, inside);

            Assertions.assertNull(server.receive(ByteBuffer.allocate(64)));
            Assertions.assertEquals(2, nat.getLostCount());
        }
    }

    private @NotNull LoopbackNat openNat(@NotNull NatType type) {
        return new LoopbackNat(type, NAT_HOST, this.hosts, this.clock, new SplittableRandom(RANDOM_SEED));
    }

    private @NotNull DatagramChannel openOutside(@NotNull String host) throws IOException {
        DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);

        this.outsideChannels.add(channel);
        channel.configureBlocking(false);
        channel.bind(new InetSocketAddress(LOOPBACK, 0));
        this.hosts.assign((InetSocketAddress) channel.getLocalAddress(), host);

        return channel;
    }

    /**
     * Sends a datagram from the inside endpoint to an outside socket and returns the source address the outside socket observed.
     */
    private @NotNull InetSocketAddress sendOut(@NotNull NatEndpoint inside, @NotNull DatagramChannel outside) throws IOException {
        inside.send(text("out"), (InetSocketAddress) outside.getLocalAddress());

        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MILLIS);
        ByteBuffer buffer = ByteBuffer.allocate(64);

        while (System.nanoTime() < deadline) {
            if (outside.receive(buffer) instanceof InetSocketAddress source) {
                return source;
            }
        }

        throw Exceptions.newAssertionError("The outside socket received nothing in time.");
    }
}
