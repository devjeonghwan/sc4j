package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Carries the messages of a network lobby over one non-blocking TCP connection on a local network: each message is its length (four bytes,
 * big-endian) and its bytes. Reads and writes never block; a write that does not complete is finished by later calls.
 */
final class TcpLobbyConnection implements LobbyConnection {
    private final @NotNull SocketChannel channel;

    private final @NotNull ByteBuffer header;
    private final @NotNull ArrayDeque<@NotNull ByteBuffer> writes;
    private @Nullable ByteBuffer body;
    private boolean closeAfterWrites;
    private boolean closed;

    /**
     * Creates a {@link TcpLobbyConnection} instance over a non-blocking channel, connected or connecting.
     *
     * @param channel the channel, which the connection closes
     */
    TcpLobbyConnection(@NotNull SocketChannel channel) {
        this.channel = channel;

        this.header           = ByteBuffer.allocate(Integer.BYTES);
        this.writes           = new ArrayDeque<>();
        this.body             = null;
        this.closeAfterWrites = false;
        this.closed           = false;
    }

    /**
     * @return {@code true} if the connection ended or was closed, {@code false} otherwise
     */
    @Override
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * @return why the other end refused the connection before the first message, or {@code null} if it did not
     * @implSpec A TCP connection has no refusal of its own: the host refuses with the lobby's refusal message.
     */
    @Override
    public @Nullable JoinRefusal getRefusal() {
        return null;
    }

    /**
     * @return the address of the other end on a local network, or {@code null} for a connection over Beacon.Link
     * @throws IOException if the connection is closed
     */
    @Override
    public @NotNull InetSocketAddress getRemoteAddress() throws IOException {
        SocketAddress address = this.channel.getRemoteAddress();

        if (!(address instanceof InetSocketAddress remote)) {
            throw Exceptions.newIOException("The lobby connection is closed.");
        }

        return remote;
    }


    /**
     * Completes a pending connect.
     *
     * @return {@code true} if the connection is established, {@code false} otherwise (it is pending)
     * @throws IOException if the connect failed
     */
    @Override
    public boolean finishConnect() throws IOException {
        return this.channel.finishConnect();
    }

    /**
     * Queues a message and writes what the connection takes now.
     *
     * @param message the message bytes
     * @throws IOException if the connection fails
     */
    @Override
    public void send(byte @NotNull [] message) throws IOException {
        if (this.closed) {
            return;
        }

        this.writes.addLast(ByteBuffer.allocate(Integer.BYTES + message.length).putInt(message.length).put(message).flip());
        this.flush();
    }

    /**
     * Writes the queued messages as far as the connection takes them; a connection marked to close closes once they are written.
     *
     * @throws IOException if the connection fails
     */
    @Override
    public void flush() throws IOException {
        while (!this.closed && !this.writes.isEmpty()) {
            ByteBuffer next = this.writes.getFirst();

            this.channel.write(next);

            if (next.hasRemaining()) {
                return;
            }

            this.writes.removeFirst();
        }

        if (this.closeAfterWrites) {
            this.close();
        }
    }

    /**
     * Closes the connection once the queued messages are written.
     *
     * @throws IOException if the connection fails
     */
    @Override
    public void closeAfterWrites() throws IOException {
        this.closeAfterWrites = true;
        this.flush();
    }

    /**
     * Reads every complete message the connection holds now; at the end of the stream the connection is closed.
     *
     * @return the messages in their order
     * @throws IOException if the connection fails or a message is longer than {@link #MAXIMUM_MESSAGE_LENGTH}
     */
    @Override
    public @NotNull List<byte @NotNull []> receive() throws IOException {
        List<byte[]> messages = new ArrayList<>();

        while (!this.closed) {
            ByteBuffer target = this.body != null ? this.body : this.header;

            if (this.channel.read(target) < 0) {
                this.close();

                break;
            }

            if (target.hasRemaining()) {
                break;
            }

            ByteBuffer complete = this.body;

            if (complete == null) {
                int length = this.header.flip().getInt();

                if (length < 0 || length > MAXIMUM_MESSAGE_LENGTH) {
                    throw Exceptions.newIOException("Invalid lobby message length: {0}", length);
                }

                this.body = ByteBuffer.allocate(length);
                this.header.clear();
            } else {
                messages.add(complete.array());
                this.body = null;
            }
        }

        return messages;
    }

    /**
     * Closes the connection.
     *
     * @throws IOException if the connection cannot be closed
     */
    @Override
    public void close() throws IOException {
        this.closed = true;
        this.writes.clear();
        this.channel.close();
    }
}
