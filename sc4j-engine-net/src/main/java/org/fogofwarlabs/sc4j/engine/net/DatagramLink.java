package org.fogofwarlabs.sc4j.engine.net;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Sends and receives datagrams between this machine and the other participants of a network without blocking, each datagram addressed by the
 * network's own address: the socket address of a UDP endpoint on a local network ({@link DatagramEndpoint}), or a participant of a Beacon.Link
 * overlay ({@link org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkOverlay}). The engine's lockstep transport
 * ({@link DatagramLockstepTransport}) runs over every link the same way. Datagrams may be lost, duplicated or reordered; the protocols above
 * handle it.
 *
 * @param <A> the type of the addresses
 */
public interface DatagramLink<A> extends Closeable {
    /**
     * Sends one datagram: the remaining bytes of the buffer.
     *
     * @param datagram the datagram, read from its position to its limit
     * @param target   the receiver's address
     * @throws IOException if the link is closed or the datagram cannot be sent
     */
    void send(@NotNull ByteBuffer datagram, @NotNull A target) throws IOException;

    /**
     * Receives the next waiting datagram into the buffer, from its position; a datagram longer than the remaining space is cut. Returns at once
     * when none is waiting.
     *
     * @param buffer the buffer that receives the datagram
     * @return the sender's address, or {@code null} if no datagram is waiting
     * @throws IOException if the link is closed
     */
    @Nullable A receive(@NotNull ByteBuffer buffer) throws IOException;

    /**
     * Closes the link; it receives and sends nothing afterwards.
     *
     * @throws IOException if the link cannot be closed
     */
    @Override
    void close() throws IOException;
}
