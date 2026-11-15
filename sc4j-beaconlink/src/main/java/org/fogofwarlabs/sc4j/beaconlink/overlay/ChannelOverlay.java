package org.fogofwarlabs.sc4j.beaconlink.overlay;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.net.DatagramLink;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carries datagrams by participant over the channels of a Beacon.Link session ({@link LinkOverlay}). Every channel datagram of the overlay
 * is its port number (one byte) and the port's datagram. A port's receive first takes every datagram the participants' channels hold and
 * queues each at its port ({@value #MAXIMUM_QUEUED_DATAGRAMS} at most per port; more are lost), then returns the port's oldest one.
 */
public final class ChannelOverlay implements LinkOverlay {
    /**
     * The most datagrams queued at one port; more are lost like any datagram.
     */
    static final int MAXIMUM_QUEUED_DATAGRAMS = 1024;

    private static final int PORT_LENGTH = 1;
    private static final int PORT_MASK = 0xFF;

    private final @NotNull BeaconLinkSession session;

    private final @NotNull Map<@NotNull LinkParticipant, @NotNull ParticipantChannel> channels;
    private final @Nullable OverlayPort @NotNull [] ports;
    private final @NotNull ByteBuffer receiveBuffer;
    private boolean closed;

    /**
     * Creates a {@link ChannelOverlay} instance over a session's channels.
     *
     * @param session the session, which polls the channels
     */
    public ChannelOverlay(@NotNull BeaconLinkSession session) {
        this.session = session;

        this.channels      = new LinkedHashMap<>();
        this.ports         = new OverlayPort[MAXIMUM_PORT + 1];
        this.receiveBuffer = ByteBuffer.allocate(ParticipantChannel.MAXIMUM_PAYLOAD_LENGTH);
        this.closed        = false;
    }

    /**
     * @return the participant of the overlay's session
     */
    @Override
    public @NotNull LinkParticipant getParticipant() {
        return this.session.getParticipant();
    }

    /**
     * @return the participants added to the overlay, in the order they were added
     */
    @Override
    public @NotNull List<@NotNull LinkParticipant> getParticipants() {
        return List.copyOf(this.channels.keySet());
    }

    /**
     * @param participant the participant
     * @return the overlay's channel to the participant, or {@code null} if the participant was not added
     */
    @Override
    public @Nullable ParticipantChannel getChannel(@NotNull LinkParticipant participant) {
        return this.channels.get(participant);
    }


    /**
     * Adds a participant: the overlay takes the session's channel to it, or opens one ({@link BeaconLinkSession#openChannel}); the other
     * participant adds this one too. A participant whose channel closed gets a new channel. Adding a participant again does nothing more.
     *
     * @param participant the participant
     * @return the channel to the participant
     * @throws IllegalArgumentException if the participant is the session's own
     * @throws IllegalStateException    if the overlay is closed or the session holds the most channels it allows
     */
    @Override
    public @NotNull ParticipantChannel addParticipant(@NotNull LinkParticipant participant) {
        if (this.closed) {
            throw Exceptions.newIllegalStateException("The overlay of {0} is closed.", this.getParticipant());
        }

        ParticipantChannel existing = this.channels.get(participant);

        if (existing != null && existing.getState() != ChannelState.CLOSED) {
            return existing;
        }

        ParticipantChannel channel = this.session.openChannel(participant);

        this.channels.put(participant, channel);

        return channel;
    }

    /**
     * Removes a participant and closes its channel. Removing a participant that was not added does nothing.
     *
     * @param participant the participant
     */
    @Override
    public void removeParticipant(@NotNull LinkParticipant participant) {
        ParticipantChannel channel = this.channels.remove(participant);

        if (channel != null) {
            channel.close();
        }
    }

    /**
     * Opens a port: the link sends datagrams of the port to the added participants and receives theirs. A datagram to a participant whose
     * channel is not open is lost; a datagram is at most {@value #MAXIMUM_DATAGRAM_LENGTH} bytes. Closing the link closes the port.
     *
     * @param port the port number, {@code 0} to {@value #MAXIMUM_PORT}
     * @return the port's link
     * @throws IllegalArgumentException if the port number is out of range
     * @throws IllegalStateException    if the port is open already or the overlay is closed
     */
    @Override
    public @NotNull DatagramLink<@NotNull LinkParticipant> openPort(int port) {
        if (port < 0 || port > MAXIMUM_PORT) {
            throw Exceptions.newIllegalArgumentException("Invalid overlay port: {0}", port);
        }

        if (this.closed || this.ports[port] != null) {
            throw Exceptions.newIllegalStateException("The overlay port {0} of {1} is open already or the overlay is closed.",
                                                      port, this.getParticipant());
        }

        OverlayPort opened = new OverlayPort(this, port, MAXIMUM_QUEUED_DATAGRAMS);

        this.ports[port] = opened;

        return opened;
    }

    /**
     * Sends a datagram of a port on the channel to a participant. A datagram to a participant that was not added, whose channel is not open
     * or whose channel does not take it is dropped, as a datagram link may drop any datagram.
     *
     * @param port     the port number
     * @param datagram the datagram, read from its position to its limit
     * @param target   the receiver
     */
    void send(int port, @NotNull ByteBuffer datagram, @NotNull LinkParticipant target) {
        ParticipantChannel channel = this.channels.get(target);

        if (channel == null || channel.getState() != ChannelState.OPEN) {
            return;
        }

        ByteBuffer payload = ByteBuffer.allocate(PORT_LENGTH + datagram.remaining());

        payload.put((byte) port).put(datagram).flip();
        channel.send(payload);
    }

    /**
     * Takes every datagram the participants' channels hold and queues each at its port; a datagram of a port that is not open is dropped.
     */
    void receiveChannelDatagrams() {
        ByteBuffer buffer = this.receiveBuffer;

        for (Map.Entry<LinkParticipant, ParticipantChannel> entry : this.channels.entrySet()) {
            ParticipantChannel channel = entry.getValue();
            int length = channel.receive(buffer.clear());

            while (length != ParticipantChannel.NO_DATAGRAM) {
                OverlayPort port = length < PORT_LENGTH ? null : this.ports[buffer.get(0) & PORT_MASK];

                if (port != null) {
                    port.queue(entry.getKey(), buffer.array(), PORT_LENGTH, buffer.position() - PORT_LENGTH);
                }

                length = channel.receive(buffer.clear());
            }
        }
    }

    /**
     * Called when a port's link is closed: the port takes no more datagrams.
     *
     * @param port the closed port
     */
    void onPortClosed(@NotNull OverlayPort port) {
        if (this.ports[port.getNumber()] == port) {
            this.ports[port.getNumber()] = null;
        }
    }

    /**
     * Closes the overlay: its ports and the channels of its participants. Calling it again does nothing.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;

        for (OverlayPort port : this.ports) {
            if (port != null) {
                port.close();
            }
        }

        for (ParticipantChannel channel : List.copyOf(this.channels.values())) {
            channel.close();
        }

        this.channels.clear();
    }
}
