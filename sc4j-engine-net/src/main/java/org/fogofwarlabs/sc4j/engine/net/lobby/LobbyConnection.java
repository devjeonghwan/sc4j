package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Carries the messages of a network lobby between the host and one member, reliably and in order, without blocking: over a TCP connection
 * on a local network ({@link TcpLobbyConnection}) or over the Beacon.Link overlay ({@link LinkLobbyConnection}). Each message is its length
 * (four bytes, big-endian) and its bytes on the stream; a write that does not complete is finished by later calls.
 */
interface LobbyConnection {
    /**
     * The longest message a lobby connection takes; the start message carries the game setup with its map digest.
     */
    int MAXIMUM_MESSAGE_LENGTH = 1 << 24;

    /**
     * @return {@code true} if the connection ended or was closed, {@code false} otherwise
     */
    boolean isClosed();

    /**
     * @return why the other end refused the connection before the first message, or {@code null} if it did not
     */
    @Nullable JoinRefusal getRefusal();

    /**
     * @return the address of the other end on a local network, or {@code null} for a connection over Beacon.Link
     * @throws IOException if the connection is closed
     */
    @Nullable InetSocketAddress getRemoteAddress() throws IOException;


    /**
     * Completes a pending connect.
     *
     * @return {@code true} if the connection is established, {@code false} otherwise (it is pending)
     * @throws IOException if the connect failed
     */
    boolean finishConnect() throws IOException;

    /**
     * Queues a message and writes what the connection takes now.
     *
     * @param message the message bytes
     * @throws IOException if the connection fails
     */
    void send(byte @NotNull [] message) throws IOException;

    /**
     * Writes the queued messages as far as the connection takes them; a connection marked to close closes once they are written.
     *
     * @throws IOException if the connection fails
     */
    void flush() throws IOException;

    /**
     * Closes the connection once the queued messages are written.
     *
     * @throws IOException if the connection fails
     */
    void closeAfterWrites() throws IOException;

    /**
     * Reads every complete message the connection holds now; at the end of the stream the connection is closed.
     *
     * @return the messages in their order
     * @throws IOException if the connection fails or a message is longer than {@link #MAXIMUM_MESSAGE_LENGTH}
     */
    @NotNull List<byte @NotNull []> receive() throws IOException;

    /**
     * Closes the connection.
     *
     * @throws IOException if the connection cannot be closed
     */
    void close() throws IOException;
}
