package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Carries the messages of a network lobby over the Beacon.Link overlay to one participant, reliably and in order: the stream of messages (each
 * its length, four bytes, and its bytes, as on TCP) is cut into numbered segments that fit one datagram of the lobby port; the receiver
 * acknowledges the segments it holds without gap, and the sender keeps at most {@value #WINDOW_SEGMENTS} segments unacknowledged and sends
 * every one again after {@value #RESEND_MILLIS} ms. The connection is established once the participant's channel is open, and ends when the
 * channel closes or the other end closes it.
 * <p>
 * Wire format, big-endian: the segment type (one byte) and the segment number (four bytes), then for a data segment (1) its stream bytes; an
 * acknowledgement (2) carries the number of segments the sender holds without gap, a close (3) the number of data segments its sender sent.
 * A malformed datagram is ignored.
 */
final class LinkLobbyConnection implements LobbyConnection {
    /**
     * The most data segments sent and not acknowledged.
     */
    static final int WINDOW_SEGMENTS = 64;
    /**
     * The time after which an unacknowledged segment is sent again.
     */
    static final long RESEND_MILLIS = 250L;

    private static final int HEADER_LENGTH = 1 + Integer.BYTES;
    private static final int DATA = 1;
    private static final int ACKNOWLEDGEMENT = 2;
    private static final int CLOSE = 3;

    /**
     * Holds one sent data segment with the time it was last sent.
     */
    private static final class Segment {
        private final byte @NotNull [] datagram;
        private long sentMillis;

        private Segment(byte @NotNull [] datagram, long sentMillis) {
            this.datagram   = datagram;
            this.sentMillis = sentMillis;
        }
    }


    private final @NotNull LinkLobbyPort port;
    private final @NotNull LinkParticipant participant;

    private final @NotNull ArrayDeque<@NotNull ByteBuffer> writes;
    private final @NotNull ArrayDeque<@NotNull Segment> unacknowledged;
    private int nextSegment;

    private final @NotNull TreeMap<@NotNull Integer, byte @NotNull []> early;
    private final @NotNull ArrayDeque<@NotNull ByteBuffer> arrived;
    private final @NotNull ByteBuffer header;
    private @Nullable ByteBuffer body;
    private int expectedSegment;

    private @Nullable JoinRefusal refusal;
    private boolean closeAfterWrites;
    private boolean closedByPeer;
    private boolean closed;

    /**
     * Creates a {@link LinkLobbyConnection} instance to a participant of the port's overlay.
     *
     * @param port        the lobby port
     * @param participant the other end
     */
    LinkLobbyConnection(@NotNull LinkLobbyPort port, @NotNull LinkParticipant participant) {
        this.port        = port;
        this.participant = participant;

        this.writes         = new ArrayDeque<>();
        this.unacknowledged = new ArrayDeque<>();
        this.nextSegment    = 0;

        this.early           = new TreeMap<>();
        this.arrived         = new ArrayDeque<>();
        this.header          = ByteBuffer.allocate(Integer.BYTES);
        this.body            = null;
        this.expectedSegment = 0;

        this.refusal          = null;
        this.closeAfterWrites = false;
        this.closedByPeer     = false;
        this.closed           = false;
    }

    /**
     * @return the other end
     */
    @NotNull LinkParticipant getParticipant() {
        return this.participant;
    }

    /**
     * @return {@code true} if the connection ended or was closed, {@code false} otherwise
     */
    @Override
    public boolean isClosed() {
        ParticipantChannel channel = this.port.getOverlay().getChannel(this.participant);

        return this.closed || channel == null || channel.getState() == ChannelState.CLOSED;
    }

    /**
     * @return why the other end refused the connection before the first message, or {@code null} if it did not
     * @implSpec The host's refusal of the join of its room, which closed the channel.
     */
    @Override
    public @Nullable JoinRefusal getRefusal() {
        return this.refusal;
    }

    /**
     * @return the address of the other end on a local network, or {@code null} for a connection over Beacon.Link
     */
    @Override
    public @Nullable InetSocketAddress getRemoteAddress() {
        return null;
    }


    /**
     * Completes a pending connect.
     *
     * @return {@code true} if the connection is established, {@code false} otherwise (it is pending)
     * @throws IOException if the connect failed
     * @implSpec The connection is established once the channel to the participant is open; a closed channel fails the connect with its reason.
     */
    @Override
    public boolean finishConnect() throws IOException {
        ParticipantChannel channel = this.port.getOverlay().getChannel(this.participant);

        if (channel == null || channel.getState() == ChannelState.CLOSED) {
            ChannelFailure failure = channel == null ? null : channel.getFailure();

            this.refusal = channel == null ? null : channel.getJoinRefusal();

            throw Exceptions.newIOException("No connection to the host {0}: {1}",
                                            this.participant, failure == null ? "the channel was closed." : failure.getDescription());
        }

        return channel.getState() == ChannelState.OPEN;
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
        if (this.closed) {
            return;
        }

        long nowMillis = this.port.getClock().getMillis();

        for (Segment segment : this.unacknowledged) {
            if (nowMillis - segment.sentMillis >= RESEND_MILLIS) {
                this.port.send(ByteBuffer.wrap(segment.datagram), this.participant);
                segment.sentMillis = nowMillis;
            }
        }

        while (this.unacknowledged.size() < WINDOW_SEGMENTS && !this.writes.isEmpty()) {
            ByteBuffer datagram = ByteBuffer.allocate(LinkOverlay.MAXIMUM_DATAGRAM_LENGTH);

            datagram.put((byte) DATA).putInt(this.nextSegment);

            while (datagram.hasRemaining() && !this.writes.isEmpty()) {
                ByteBuffer next = this.writes.getFirst();
                int length = Math.min(datagram.remaining(), next.remaining());

                datagram.put(next.array(), next.position(), length);
                next.position(next.position() + length);

                if (!next.hasRemaining()) {
                    this.writes.removeFirst();
                }
            }

            byte[] bytes = new byte[datagram.position()];

            datagram.flip().get(bytes);
            this.unacknowledged.addLast(new Segment(bytes, nowMillis));
            this.nextSegment++;
            this.port.send(ByteBuffer.wrap(bytes), this.participant);
        }

        if (this.closeAfterWrites && this.writes.isEmpty() && this.unacknowledged.isEmpty()) {
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

        if (this.closed) {
            return messages;
        }

        this.port.receive();

        while (!this.arrived.isEmpty()) {
            ByteBuffer chunk = this.arrived.getFirst();
            ByteBuffer target = this.body != null ? this.body : this.header;
            int length = Math.min(chunk.remaining(), target.remaining());

            target.put(chunk.array(), chunk.position(), length);
            chunk.position(chunk.position() + length);

            if (!chunk.hasRemaining()) {
                this.arrived.removeFirst();
            }

            if (target.hasRemaining()) {
                continue;
            }

            ByteBuffer complete = this.body;

            if (complete == null) {
                int messageLength = this.header.flip().getInt();

                if (messageLength < 0 || messageLength > MAXIMUM_MESSAGE_LENGTH) {
                    throw Exceptions.newIOException("Invalid lobby message length: {0}", messageLength);
                }

                this.body = ByteBuffer.allocate(messageLength);
                this.header.clear();
            } else {
                messages.add(complete.array());
                this.body = null;
            }
        }

        if (this.closedByPeer || this.isClosed()) {
            this.close();
        }

        return messages;
    }

    /**
     * Takes one datagram the other end sent: stores a data segment in order and acknowledges it, takes an acknowledgement or a close.
     *
     * @param datagram the datagram, from its position to its limit
     */
    void handleDatagram(@NotNull ByteBuffer datagram) {
        if (this.closed || datagram.remaining() < HEADER_LENGTH) {
            return;
        }

        int type = Byte.toUnsignedInt(datagram.get());
        int number = datagram.getInt();

        switch (type) {
            case DATA -> this.handleData(number, datagram);
            case ACKNOWLEDGEMENT -> {
                while (!this.unacknowledged.isEmpty() && this.nextSegment - this.unacknowledged.size() < number) {
                    this.unacknowledged.removeFirst();
                }
            }
            case CLOSE -> this.closedByPeer = true;
            default -> {
                // An unknown segment type is ignored like any malformed datagram.
            }
        }
    }

    private void handleData(int number, @NotNull ByteBuffer datagram) {
        if (number >= this.expectedSegment && number - this.expectedSegment < WINDOW_SEGMENTS && !this.early.containsKey(number)) {
            byte[] bytes = new byte[datagram.remaining()];

            datagram.get(bytes);
            this.early.put(number, bytes);
        }

        byte[] next = this.early.remove(this.expectedSegment);

        while (next != null) {
            this.arrived.addLast(ByteBuffer.wrap(next));
            this.expectedSegment++;
            next = this.early.remove(this.expectedSegment);
        }

        this.sendControl(ACKNOWLEDGEMENT, this.expectedSegment);
    }

    private void sendControl(int type, int number) {
        try {
            this.port.send(ByteBuffer.allocate(HEADER_LENGTH).put((byte) type).putInt(number).flip(), this.participant);
        } catch (IOException exception) {
            // The lobby port is closed: the acknowledgement or close is lost, and the other end sees its channel close.
        }
    }

    /**
     * Closes the connection.
     *
     * @implSpec The other end is told; the overlay closes the channel to it.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        if (!this.isClosed()) {
            this.sendControl(CLOSE, this.nextSegment);
        }

        this.closed = true;
        this.writes.clear();
        this.unacknowledged.clear();
        this.early.clear();
        this.arrived.clear();
        this.port.onClosed(this);
    }
}
