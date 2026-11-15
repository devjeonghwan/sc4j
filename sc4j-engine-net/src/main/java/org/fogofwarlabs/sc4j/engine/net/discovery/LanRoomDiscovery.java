package org.fogofwarlabs.sc4j.engine.net.discovery;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.DatagramEndpoint;
import org.fogofwarlabs.sc4j.engine.net.NetworkClock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the game rooms of a LAN over UDP and announces the local one: the engine's default room discovery. A client sends a query to an
 * address (the LAN's broadcast address, or a host's address directly) and every host there answers with its room; a host may also announce its
 * room unasked. Each found room carries the address of its network lobby and how its mod identity differs from the local one; a room that
 * was not announced again within {@link #ROOM_EXPIRY_MILLIS} is forgotten.
 * <p>
 * Wire format, big-endian: {@link #MAGIC} (two bytes), {@link #PROTOCOL_VERSION} (one byte) and the message type (one byte), then for a room
 * (type 2) the room's {@link ModIdentity} in its wire form, its name and its host's name (modified UTF-8), the lobby port (two bytes), the
 * member count, the capacity and the open flag (one byte each). A query (type 1) has no body. Other datagrams are ignored.
 * <p>
 * The discovery is not thread-safe and never blocks; the caller polls it ({@link #poll()}).
 */
public final class LanRoomDiscovery {
    /**
     * The engine's default UDP port of the room discovery.
     */
    public static final int DEFAULT_PORT = 6120;
    /**
     * The first two bytes of every discovery datagram.
     */
    public static final int MAGIC = 0x5344;
    /**
     * The protocol version of the discovery datagrams.
     */
    public static final int PROTOCOL_VERSION = 1;
    /**
     * The time after its last announcement after which a found room is forgotten.
     */
    public static final long ROOM_EXPIRY_MILLIS = 10000L;

    private static final int HEADER_LENGTH = 4;
    private static final int QUERY_TYPE = 1;
    private static final int ROOM_TYPE = 2;
    private static final int RECEIVE_BUFFER_LENGTH = 0x10000;

    private final @NotNull DatagramEndpoint endpoint;
    private final @NotNull NetworkClock clock;
    private final @NotNull ModIdentity identity;

    private final @NotNull ByteBuffer receiveBuffer;
    private final @NotNull List<@NotNull DiscoveredRoom> rooms;
    private @Nullable LanRoom room;

    /**
     * Creates a {@link LanRoomDiscovery} instance without a hosted room and without found rooms.
     *
     * @param endpoint the local datagram endpoint, which the caller closes
     * @param clock    the clock of the room expiry
     * @param identity the local mod identity, which found rooms are compared with
     */
    public LanRoomDiscovery(@NotNull DatagramEndpoint endpoint, @NotNull NetworkClock clock, @NotNull ModIdentity identity) {
        this.endpoint = endpoint;
        this.clock    = clock;
        this.identity = identity;

        this.receiveBuffer = ByteBuffer.allocate(RECEIVE_BUFFER_LENGTH);
        this.rooms         = new ArrayList<>();
        this.room          = null;
    }

    /**
     * @return the hosted room the discovery answers queries with, or {@code null} if it hosts none
     */
    public @Nullable LanRoom getRoom() {
        return this.room;
    }

    /**
     * Sets the hosted room the discovery answers queries with.
     *
     * @param room the room, or {@code null} to answer no query
     */
    public void setRoom(@Nullable LanRoom room) {
        this.room = room;
    }

    /**
     * @return the rooms found and not expired at the latest poll, in the order they were first found
     */
    public @NotNull List<@NotNull DiscoveredRoom> getRooms() {
        return List.copyOf(this.rooms);
    }


    /**
     * Sends a query: every host at the address answers with its room.
     *
     * @param target the address, the LAN's broadcast address with the discovery port or a host's address
     * @throws IOException if the query cannot be sent
     */
    public void query(@NotNull InetSocketAddress target) throws IOException {
        ByteBuffer datagram = ByteBuffer.allocate(HEADER_LENGTH);

        datagram.putShort((short) MAGIC).put((byte) PROTOCOL_VERSION).put((byte) QUERY_TYPE);
        this.endpoint.send(datagram.flip(), target);
    }

    /**
     * Announces the hosted room unasked.
     *
     * @param target the address, the LAN's broadcast address with the discovery port or a client's address
     * @throws IOException           if the announcement cannot be sent
     * @throws IllegalStateException if the discovery hosts no room
     */
    public void announce(@NotNull InetSocketAddress target) throws IOException {
        LanRoom hosted = this.room;

        if (hosted == null) {
            throw Exceptions.newIllegalStateException("The room discovery hosts no room to announce.");
        }

        this.endpoint.send(ByteBuffer.wrap(this.encodeRoom(hosted)), target);
    }

    /**
     * Handles the waiting datagrams: answers queries with the hosted room, takes the announced rooms, and forgets the expired ones.
     *
     * @throws IOException if the endpoint cannot receive or answer
     */
    public void poll() throws IOException {
        long now = this.clock.getMillis();

        while (true) {
            ByteBuffer buffer = this.receiveBuffer.clear();
            InetSocketAddress source = this.endpoint.receive(buffer);

            if (source == null) {
                break;
            }

            buffer.flip();
            this.handleDatagram(source, buffer, now);
        }

        this.rooms.removeIf(found -> now - found.getSeenTime() > ROOM_EXPIRY_MILLIS);
    }

    private void handleDatagram(@NotNull InetSocketAddress source, @NotNull ByteBuffer datagram, long now) throws IOException {
        if (datagram.remaining() < HEADER_LENGTH || Short.toUnsignedInt(datagram.getShort()) != MAGIC ||
            Byte.toUnsignedInt(datagram.get()) != PROTOCOL_VERSION) {
            return;
        }

        int type = Byte.toUnsignedInt(datagram.get());
        LanRoom hosted = this.room;

        if (type == QUERY_TYPE && hosted != null) {
            this.endpoint.send(ByteBuffer.wrap(this.encodeRoom(hosted)), source);
        } else if (type == ROOM_TYPE) {
            byte[] body = new byte[datagram.remaining()];

            datagram.get(body);

            LanRoom announced = this.decodeRoom(body);

            if (announced != null) {
                this.addRoom(new DiscoveredRoom(announced, new InetSocketAddress(source.getAddress(), announced.getLobbyPort()),
                                                announced.getIdentity().findMismatch(this.identity), now));
            }
        }
    }

    private void addRoom(@NotNull DiscoveredRoom found) {
        for (int index = 0; index < this.rooms.size(); index++) {
            if (this.rooms.get(index).getLobbyAddress().equals(found.getLobbyAddress())) {
                this.rooms.set(index, found);

                return;
            }
        }

        this.rooms.add(found);
    }

    private byte @NotNull [] encodeRoom(@NotNull LanRoom hosted) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);

        output.writeShort(MAGIC);
        output.writeByte(PROTOCOL_VERSION);
        output.writeByte(ROOM_TYPE);
        hosted.getIdentity().write(output);
        output.writeUTF(hosted.getName());
        output.writeUTF(hosted.getHostName());
        output.writeShort(hosted.getLobbyPort());
        output.writeByte(hosted.getMemberCount());
        output.writeByte(hosted.getCapacity());
        output.writeBoolean(hosted.isOpen());

        return bytes.toByteArray();
    }

    /**
     * Decodes an announced room.
     *
     * @param body the datagram's bytes after the header
     * @return the room, or {@code null} if the bytes hold no room of the wire form
     */
    private @Nullable LanRoom decodeRoom(byte @NotNull [] body) {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(body));

        try {
            ModIdentity announced = ModIdentity.read(input);
            String name = input.readUTF();
            String hostName = input.readUTF();
            int lobbyPort = input.readUnsignedShort();
            int memberCount = input.readUnsignedByte();
            int capacity = input.readUnsignedByte();
            boolean open = input.readBoolean();

            return new LanRoom(name, hostName, lobbyPort, memberCount, capacity, open, announced);
        } catch (IOException | IllegalArgumentException exception) {
            return null;
        }
    }
}
