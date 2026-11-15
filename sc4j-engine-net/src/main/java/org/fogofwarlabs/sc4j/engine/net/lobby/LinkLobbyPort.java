package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.engine.net.DatagramLink;
import org.fogofwarlabs.sc4j.engine.net.NetworkClock;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkOverlay;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkParticipant;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the lobby port ({@link LinkOverlay#LOBBY_PORT}) of a network lobby over a Beacon.Link overlay with the connection to each member: it
 * hands every datagram of the port to the connection of its sender; a datagram of a participant without connection is dropped.
 */
final class LinkLobbyPort {
    private final @NotNull LinkOverlay overlay;
    private final @NotNull DatagramLink<@NotNull LinkParticipant> port;
    private final @NotNull NetworkClock clock;

    private final @NotNull Map<@NotNull LinkParticipant, @NotNull LinkLobbyConnection> connections;
    private final @NotNull ByteBuffer receiveBuffer;

    /**
     * Creates a {@link LinkLobbyPort} instance: opens the overlay's lobby port.
     *
     * @param overlay the overlay
     * @param clock   the clock of the connections' resends
     * @throws IllegalStateException if the overlay's lobby port is open already
     */
    LinkLobbyPort(@NotNull LinkOverlay overlay, @NotNull NetworkClock clock) {
        this.overlay = overlay;
        this.port    = overlay.openPort(LinkOverlay.LOBBY_PORT);
        this.clock   = clock;

        this.connections   = new LinkedHashMap<>();
        this.receiveBuffer = ByteBuffer.allocate(LinkOverlay.MAXIMUM_DATAGRAM_LENGTH);
    }

    /**
     * @return the overlay of the port
     */
    @NotNull LinkOverlay getOverlay() {
        return this.overlay;
    }

    /**
     * @return the clock of the connections' resends
     */
    @NotNull NetworkClock getClock() {
        return this.clock;
    }


    /**
     * Adds a participant to the overlay and creates the lobby connection to it.
     *
     * @param participant the other end
     * @return the created {@link LinkLobbyConnection} instance
     */
    @NotNull LinkLobbyConnection connect(@NotNull LinkParticipant participant) {
        this.overlay.addParticipant(participant);

        LinkLobbyConnection connection = new LinkLobbyConnection(this, participant);

        this.connections.put(participant, connection);

        return connection;
    }

    /**
     * Hands every waiting datagram of the port to the connection of its sender.
     *
     * @throws IOException if the port is closed
     */
    void receive() throws IOException {
        while (true) {
            ByteBuffer buffer = this.receiveBuffer.clear();
            LinkParticipant sender = this.port.receive(buffer);

            if (sender == null) {
                return;
            }

            LinkLobbyConnection connection = this.connections.get(sender);

            if (connection != null) {
                connection.handleDatagram(buffer.flip());
            }
        }
    }

    /**
     * Sends a datagram of a connection.
     *
     * @param datagram the datagram, read from its position to its limit
     * @param target   the other end of the connection
     * @throws IOException if the port is closed
     */
    void send(@NotNull ByteBuffer datagram, @NotNull LinkParticipant target) throws IOException {
        this.port.send(datagram, target);
    }

    /**
     * Called when a connection closed: the port forgets it and the overlay closes the channel to its other end.
     *
     * @param connection the closed connection
     */
    void onClosed(@NotNull LinkLobbyConnection connection) {
        if (this.connections.remove(connection.getParticipant(), connection)) {
            this.overlay.removeParticipant(connection.getParticipant());
        }
    }

    /**
     * Closes the port. A port that cannot be closed is logged and ignored.
     */
    void close() {
        IgnoreException.execute(
                this.port::close
        );
    }
}
