package org.fogofwarlabs.sc4j.engine.net;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Sends and receives the datagrams of one local address without blocking: the engine network's link to the operating system
 * ({@link UdpDatagramEndpoint}), or a wrapper of it (a test link that loses, duplicates or reorders datagrams), addressed by socket address.
 * Datagrams may be lost, duplicated or reordered; the protocols above ({@link UdpLockstepTransport}, the room discovery and the lobby) handle
 * it.
 */
public interface DatagramEndpoint extends DatagramLink<InetSocketAddress> {
    /**
     * @return the local address the endpoint receives at
     * @throws IOException if the endpoint is closed
     */
    @NotNull InetSocketAddress getLocalAddress() throws IOException;


    /**
     * Sends one datagram: the remaining bytes of the buffer.
     *
     * @param datagram the datagram, read from its position to its limit
     * @param target   the receiver's address
     * @throws IOException if the endpoint is closed or the datagram cannot be sent
     */
    @Override
    void send(@NotNull ByteBuffer datagram, @NotNull InetSocketAddress target) throws IOException;

    /**
     * Receives the next waiting datagram into the buffer, from its position; a datagram longer than the remaining space is cut. Returns at once
     * when none is waiting.
     *
     * @param buffer the buffer that receives the datagram
     * @return the sender's address, or {@code null} if no datagram is waiting
     * @throws IOException if the endpoint is closed
     */
    @Override
    @Nullable InetSocketAddress receive(@NotNull ByteBuffer buffer) throws IOException;

    /**
     * Closes the endpoint; it receives and sends nothing afterwards.
     *
     * @throws IOException if the endpoint cannot be closed
     */
    @Override
    void close() throws IOException;
}
