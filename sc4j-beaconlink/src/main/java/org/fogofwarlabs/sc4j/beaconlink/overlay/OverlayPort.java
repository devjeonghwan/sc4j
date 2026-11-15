package org.fogofwarlabs.sc4j.beaconlink.overlay;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.net.DatagramLink;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkOverlay;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkParticipant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Sends and receives the datagrams of one port of a {@link ChannelOverlay}, addressed by participant. A datagram to a participant whose
 * channel is not open is lost.
 */
final class OverlayPort implements DatagramLink<LinkParticipant> {
    /**
     * Holds one received datagram of the port with its sender.
     */
    private static final class Arrival {
        private final @NotNull LinkParticipant sender;
        private final byte @NotNull [] datagram;

        private Arrival(@NotNull LinkParticipant sender, byte @NotNull [] datagram) {
            this.sender   = sender;
            this.datagram = datagram;
        }
    }


    private final @NotNull ChannelOverlay overlay;
    private final int number;
    private final int capacity;

    private final @NotNull ArrayDeque<@NotNull Arrival> arrivals;
    private boolean closed;

    /**
     * Creates an {@link OverlayPort} instance.
     *
     * @param overlay  the overlay of the port
     * @param number   the port number
     * @param capacity the most datagrams the port queues
     */
    OverlayPort(@NotNull ChannelOverlay overlay, int number, int capacity) {
        this.overlay  = overlay;
        this.number   = number;
        this.capacity = capacity;

        this.arrivals = new ArrayDeque<>();
        this.closed   = false;
    }

    /**
     * @return the port number
     */
    int getNumber() {
        return this.number;
    }


    /**
     * Sends one datagram: the remaining bytes of the buffer. A datagram to a participant not added to the overlay, or whose channel is not
     * open, is lost.
     *
     * @param datagram the datagram, read from its position to its limit
     * @param target   the receiver's address
     * @throws IOException if the link is closed or the datagram is longer than {@value LinkOverlay#MAXIMUM_DATAGRAM_LENGTH} bytes
     */
    @Override
    public void send(@NotNull ByteBuffer datagram, @NotNull LinkParticipant target) throws IOException {
        if (this.closed) {
            throw Exceptions.newIOException("The overlay port {0} is closed.", this.number);
        }

        if (datagram.remaining() > LinkOverlay.MAXIMUM_DATAGRAM_LENGTH) {
            throw Exceptions.newIOException("An overlay datagram has at most {0} bytes, not {1}.",
                                            LinkOverlay.MAXIMUM_DATAGRAM_LENGTH, datagram.remaining());
        }

        this.overlay.send(this.number, datagram, target);
    }

    /**
     * Receives the next waiting datagram into the buffer, from its position; a datagram longer than the remaining space is cut. Returns at once
     * when none is waiting.
     *
     * @param buffer the buffer that receives the datagram
     * @return the sender's address, or {@code null} if no datagram is waiting
     * @throws IOException if the link is closed
     */
    @Override
    public @Nullable LinkParticipant receive(@NotNull ByteBuffer buffer) throws IOException {
        if (this.closed) {
            throw Exceptions.newIOException("The overlay port {0} is closed.", this.number);
        }

        this.overlay.receiveChannelDatagrams();

        Arrival arrival = this.arrivals.pollFirst();

        if (arrival == null) {
            return null;
        }

        buffer.put(arrival.datagram, 0, Math.min(arrival.datagram.length, buffer.remaining()));

        return arrival.sender;
    }

    /**
     * Queues a datagram the overlay received for the port; a full queue loses it.
     *
     * @param sender the sender
     * @param bytes  the bytes holding the datagram
     * @param offset the datagram's first byte
     * @param length the datagram's length
     */
    void queue(@NotNull LinkParticipant sender, byte @NotNull [] bytes, int offset, int length) {
        if (!this.closed && this.arrivals.size() < this.capacity) {
            this.arrivals.addLast(new Arrival(sender, Arrays.copyOfRange(bytes, offset, offset + length)));
        }
    }

    /**
     * Closes the link; it receives and sends nothing afterwards.
     */
    @Override
    public void close() {
        this.closed = true;
        this.arrivals.clear();
        this.overlay.onPortClosed(this);
    }
}
