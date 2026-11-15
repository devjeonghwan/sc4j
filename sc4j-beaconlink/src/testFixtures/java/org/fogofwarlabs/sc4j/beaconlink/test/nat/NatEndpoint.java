package org.fogofwarlabs.sc4j.beaconlink.test.nat;

import org.fogofwarlabs.sc4j.engine.net.DatagramEndpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sends and receives the datagrams of one host behind an emulated NAT: what it sends leaves through a public mapping of its
 * {@link LoopbackNat}, and it receives what the NAT lets through, with the original source address. Its own address is a real 127.0.0.1
 * socket that stands for the address inside the NAT; datagrams sent to it directly arrive unfiltered, like datagrams of the same local network
 * (an endpoint of the same NAT, or a test socket outside every NAT).
 */
public final class NatEndpoint implements DatagramEndpoint {
    /**
     * Holds one datagram the NAT let through, until the clock reaches its delivery time.
     */
    private static final class Arrival {
        private final byte @NotNull [] bytes;
        private final @NotNull InetSocketAddress source;
        private final long deliveryMillis;

        private Arrival(byte @NotNull [] bytes, @NotNull InetSocketAddress source, long deliveryMillis) {
            this.bytes          = bytes;
            this.source         = source;
            this.deliveryMillis = deliveryMillis;
        }
    }


    private final @NotNull LoopbackNat nat;
    private final @NotNull DatagramChannel insideChannel;
    private final @NotNull InetSocketAddress insideAddress;

    private final @NotNull Deque<@NotNull Arrival> arrivals;

    /**
     * Creates a {@link NatEndpoint} instance.
     *
     * @param nat           the NAT the endpoint is behind
     * @param insideChannel the inside socket, bound to 127.0.0.1 and not blocking
     * @param insideAddress the inside socket's address
     */
    NatEndpoint(@NotNull LoopbackNat nat, @NotNull DatagramChannel insideChannel, @NotNull InetSocketAddress insideAddress) {
        this.nat           = nat;
        this.insideChannel = insideChannel;
        this.insideAddress = insideAddress;

        this.arrivals = new ArrayDeque<>();
    }

    /**
     * @return the local address the endpoint receives at: the 127.0.0.1 socket that stands for its address inside the NAT
     */
    @Override
    public @NotNull InetSocketAddress getLocalAddress() {
        return this.insideAddress;
    }

    /**
     * @return the NAT the endpoint is behind
     */
    public @NotNull LoopbackNat getNat() {
        return this.nat;
    }


    /**
     * Sends one datagram: the remaining bytes of the buffer, through the NAT's mapping for the target.
     *
     * @param datagram the datagram, read from its position to its limit
     * @param target   the receiver's address
     * @throws IOException if the endpoint is closed or the datagram cannot be sent
     */
    @Override
    public void send(@NotNull ByteBuffer datagram, @NotNull InetSocketAddress target) throws IOException {
        this.nat.send(this, datagram, target);
    }

    /**
     * Receives the next waiting datagram into the buffer, from its position; a datagram longer than the remaining space is cut. Returns at once
     * when none is waiting. The NAT's public sockets are read first, so that datagrams through the NAT arrive without another poll.
     *
     * @param buffer the buffer that receives the datagram
     * @return the sender's address, or {@code null} if no datagram is waiting
     * @throws IOException if the endpoint is closed
     */
    @Override
    public @Nullable InetSocketAddress receive(@NotNull ByteBuffer buffer) throws IOException {
        SocketAddress direct = this.receiveDirect(buffer);

        if (direct instanceof InetSocketAddress source) {
            return source;
        }

        this.nat.pump();

        Arrival arrival = this.arrivals.peekFirst();

        if (arrival == null || arrival.deliveryMillis > this.nat.getClockMillis()) {
            return null;
        }

        this.arrivals.removeFirst();
        buffer.put(arrival.bytes, 0, Math.min(arrival.bytes.length, buffer.remaining()));

        return arrival.source;
    }

    /**
     * Sends a datagram from the inside socket to an inside address of the same NAT, past the NAT.
     *
     * @param datagram the datagram, read from its position to its limit
     * @param target   the receiver's inside address
     * @throws IOException if the endpoint is closed
     */
    void sendDirect(@NotNull ByteBuffer datagram, @NotNull InetSocketAddress target) throws IOException {
        try {
            this.insideChannel.send(datagram, target);
        } catch (PortUnreachableException exception) {
            // A closed receiver; the datagram is lost like on a real network.
        }
    }

    private @Nullable SocketAddress receiveDirect(@NotNull ByteBuffer buffer) throws IOException {
        while (true) {
            try {
                return this.insideChannel.receive(buffer);
            } catch (PortUnreachableException exception) {
                // An earlier datagram met a closed port; the next waiting datagram is still readable.
            }
        }
    }

    /**
     * Queues a datagram the NAT let through.
     *
     * @param bytes          the datagram
     * @param source         its original source
     * @param deliveryMillis the clock time from which it can be received
     */
    void deliver(byte @NotNull [] bytes, @NotNull InetSocketAddress source, long deliveryMillis) {
        this.arrivals.addLast(new Arrival(bytes, source, deliveryMillis));
    }

    /**
     * Closes the endpoint and its mappings; it receives and sends nothing afterwards.
     *
     * @throws IOException if the inside socket cannot be closed
     */
    @Override
    public void close() throws IOException {
        this.nat.closeMappings(this);
        this.arrivals.clear();
        this.insideChannel.close();
    }
}
