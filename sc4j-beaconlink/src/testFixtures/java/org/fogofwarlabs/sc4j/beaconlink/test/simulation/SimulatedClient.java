package org.fogofwarlabs.sc4j.beaconlink.test.simulation;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.client.BeaconLinkClient;
import org.fogofwarlabs.sc4j.beaconlink.peer.DatagramSink;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.engine.net.DatagramEndpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Runs one Beacon.Link client on a host of a {@link SimulatedNetwork}: the client's datagram endpoint queues the datagrams that passed the
 * host's NAT and sends through the network, and the network polls the client every tick until it is closed. With prompt polling the client
 * is polled at once for every received datagram as well, as a runtime that polls every frame takes a datagram within a frame, so that the
 * round trip times it measures are not stretched to the next tick.
 */
public final class SimulatedClient implements SimulatedNetwork.Endpoint, DatagramEndpoint {
    /**
     * Holds one received datagram until the client polls it.
     */
    private static final class Received {
        private final byte @NotNull [] datagram;
        private final @NotNull InetSocketAddress source;

        private Received(byte @NotNull [] datagram, @NotNull InetSocketAddress source) {
            this.datagram = datagram;
            this.source   = source;
        }
    }


    private final @NotNull DatagramSink sink;
    private final @NotNull InetSocketAddress address;
    private final @Nullable NatType nat;
    private final @NotNull Deque<@NotNull Received> received;

    private @Nullable BeaconLinkClient client;
    private boolean promptPolling;
    private boolean closed;

    /**
     * Creates a {@link SimulatedClient} instance; the network hands it the client with {@link #setClient}.
     *
     * @param sink    the host's sink
     * @param address the host's public address
     * @param nat     the NAT in front of the host, or {@code null} for a public host
     */
    SimulatedClient(@NotNull DatagramSink sink, @NotNull InetSocketAddress address, @Nullable NatType nat) {
        this.sink     = sink;
        this.address  = address;
        this.nat      = nat;
        this.received = new ArrayDeque<>();

        this.client        = null;
        this.promptPolling = false;
        this.closed        = false;
    }

    /**
     * @return the client
     * @throws IllegalStateException if the network has not created the client yet
     */
    public @NotNull BeaconLinkClient getClient() {
        BeaconLinkClient current = this.client;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The simulated client at {0} has no client yet.", this.address);
        }

        return current;
    }

    /**
     * Sets the client that runs on the endpoint.
     *
     * @param client the client
     */
    void setClient(@NotNull BeaconLinkClient client) {
        this.client = client;
    }

    /**
     * Sets whether the client is polled at once for every received datagram, besides every tick.
     *
     * @param promptPolling whether the client is polled for every received datagram
     */
    public void setPromptPolling(boolean promptPolling) {
        this.promptPolling = promptPolling;
    }

    /**
     * @return the NAT in front of the client, or {@code null} for a public host
     */
    public @Nullable NatType getNat() {
        return this.nat;
    }

    /**
     * @return the local address the endpoint receives at: the host's public address
     */
    @Override
    public @NotNull InetSocketAddress getLocalAddress() {
        return this.address;
    }


    /**
     * Sends one datagram through the network.
     *
     * @param datagram the datagram, read from its position to its limit
     * @param target   the receiver's address
     * @throws IOException if the endpoint is closed
     */
    @Override
    public void send(@NotNull ByteBuffer datagram, @NotNull InetSocketAddress target) throws IOException {
        if (this.closed) {
            throw Exceptions.newIOException("The simulated client at {0} is closed.", this.address);
        }

        byte[] bytes = new byte[datagram.remaining()];

        datagram.get(bytes);
        this.sink.send(bytes, target);
    }

    /**
     * Receives the next queued datagram into the buffer, from its position; a datagram longer than the remaining space is cut.
     *
     * @param buffer the buffer that receives the datagram
     * @return the sender's address, or {@code null} if no datagram is queued
     * @throws IOException if the endpoint is closed
     */
    @Override
    public @Nullable InetSocketAddress receive(@NotNull ByteBuffer buffer) throws IOException {
        if (this.closed) {
            throw Exceptions.newIOException("The simulated client at {0} is closed.", this.address);
        }

        Received next = this.received.pollFirst();

        if (next == null) {
            return null;
        }

        buffer.put(next.datagram, 0, Math.min(next.datagram.length, buffer.remaining()));

        return next.source;
    }

    /**
     * Queues a datagram that passed the host's NAT, and polls the client with prompt polling.
     *
     * @param datagram the datagram, owned by the endpoint
     * @param source   the sender's public address
     */
    @Override
    public void receive(byte @NotNull [] datagram, @NotNull InetSocketAddress source) {
        if (!this.closed) {
            this.received.addLast(new Received(datagram, source));

            if (this.promptPolling) {
                this.tick();
            }
        }
    }

    /**
     * Polls the client, unless it is closed.
     */
    @Override
    public void tick() {
        BeaconLinkClient current = this.client;

        if (this.closed || current == null || current.isClosed()) {
            return;
        }

        try {
            current.poll();
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception, "The simulated client at {0} failed.", this.address);
        }
    }

    /**
     * Closes the endpoint; it receives and sends nothing afterwards.
     */
    @Override
    public void close() {
        this.closed = true;
        this.received.clear();
    }
}
