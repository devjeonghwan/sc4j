package org.fogofwarlabs.sc4j.beaconlink.protocol.room;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes a room for the clients that list it: the mod fingerprint, the room and map names, the player counts,
 * whether the room is private and whether its game started, and the mod's own data about the room. A private room carries its join key: the
 * X25519 public key with which a joiner proves the room's password to the host without sending it. Nodes store and forward it inside a signed
 * {@link RoomAdvertisement} and never interpret it.
 * <p>
 * On the wire: the fingerprint, the room name and the map name (UTF-8, at most {@value #MAXIMUM_NAME_LENGTH} bytes each, the room name not
 * empty), the players and the capacity (one byte each, players at most the capacity, capacity at least {@code 1}), the flags (one byte: bit 0
 * private, bit 1 started, the other bits zero), the join key ({@value #JOIN_KEY_LENGTH} bytes, private rooms only) and the mod data (a
 * one-byte length, at most {@value #MAXIMUM_MOD_DATA_LENGTH}, and the bytes).
 */
public final class RoomDescription {
    /**
     * The largest UTF-8 length of the room name and the map name.
     */
    public static final int MAXIMUM_NAME_LENGTH = 48;
    /**
     * The largest player capacity.
     */
    public static final int MAXIMUM_CAPACITY = 255;
    /**
     * The largest length of the mod data.
     */
    public static final int MAXIMUM_MOD_DATA_LENGTH = 64;
    /**
     * The length of a private room's join key.
     */
    public static final int JOIN_KEY_LENGTH = 32;

    private static final int COUNT_LENGTH = 1;
    private static final int FLAGS_LENGTH = 1;
    private static final int MOD_DATA_LENGTH_LENGTH = 1;
    private static final int PRIVATE_FLAG = 1;
    private static final int STARTED_FLAG = 2;
    private static final int KNOWN_FLAGS = PRIVATE_FLAG | STARTED_FLAG;
    private static final byte @NotNull [] NO_MOD_DATA = new byte[0];

    /**
     * Reads a description from the wire.
     *
     * @param reader the reader
     * @return the description, or {@code null} if the reader failed or a field is out of range
     */
    public static @Nullable RoomDescription read(@NotNull WireReader reader) {
        ModFingerprint fingerprint = ModFingerprint.read(reader);
        String roomName = reader.readText(MAXIMUM_NAME_LENGTH);
        String mapName = reader.readText(MAXIMUM_NAME_LENGTH);
        int players = reader.readUnsigned8();
        int capacity = reader.readUnsigned8();
        int flags = reader.readUnsigned8();
        byte[] joinKey = (flags & PRIVATE_FLAG) == 0 ? null : reader.readBytes(JOIN_KEY_LENGTH);
        int modDataLength = reader.readUnsigned8();

        if (fingerprint == null || reader.isFailed() || roomName.isEmpty() || capacity == 0 || players > capacity ||
            (flags & ~KNOWN_FLAGS) != 0 || modDataLength > MAXIMUM_MOD_DATA_LENGTH) {
            reader.fail();

            return null;
        }

        byte[] modData = reader.readBytes(modDataLength);

        if (reader.isFailed()) {
            return null;
        }

        return new RoomDescription(fingerprint, roomName, mapName, players, capacity, (flags & STARTED_FLAG) != 0, joinKey, modData);
    }


    private final @NotNull ModFingerprint fingerprint;

    private final @NotNull String roomName;
    private final @NotNull String mapName;

    private final int players;
    private final int capacity;

    private final boolean started;
    private final byte @Nullable [] joinKey;

    private final byte @NotNull [] modData;

    /**
     * Creates a {@link RoomDescription} instance without mod data.
     *
     * @param fingerprint the fingerprint of what the room runs
     * @param roomName    the room name, not empty
     * @param mapName     the map name
     * @param players     the number of players in the room
     * @param capacity    the largest number of players
     * @throws IllegalArgumentException if a value is out of its range
     */
    public RoomDescription(@NotNull ModFingerprint fingerprint, @NotNull String roomName, @NotNull String mapName, int players, int capacity) {
        this(fingerprint, roomName, mapName, players, capacity, false, null, NO_MOD_DATA);
    }

    /**
     * Creates a {@link RoomDescription} instance of a public room whose game has not started.
     *
     * @param fingerprint the fingerprint of what the room runs
     * @param roomName    the room name, not empty
     * @param mapName     the map name
     * @param players     the number of players in the room
     * @param capacity    the largest number of players
     * @param modData     the mod's data about the room, at most {@value #MAXIMUM_MOD_DATA_LENGTH} bytes, copied
     * @throws IllegalArgumentException if a value is out of its range
     */
    public RoomDescription(@NotNull ModFingerprint fingerprint,
                           @NotNull String roomName, @NotNull String mapName,
                           int players, int capacity,
                           byte @NotNull [] modData) {
        this(fingerprint, roomName, mapName, players, capacity, false, null, modData);
    }

    /**
     * Creates a {@link RoomDescription} instance.
     *
     * @param fingerprint the fingerprint of what the room runs
     * @param roomName    the room name, not empty
     * @param mapName     the map name
     * @param players     the number of players in the room
     * @param capacity    the largest number of players
     * @param started     whether the room's game started
     * @param joinKey     the join key of a private room ({@value #JOIN_KEY_LENGTH} bytes, copied), or {@code null} for a public room
     * @param modData     the mod's data about the room, at most {@value #MAXIMUM_MOD_DATA_LENGTH} bytes, copied
     * @throws IllegalArgumentException if a value is out of its range
     */
    public RoomDescription(@NotNull ModFingerprint fingerprint,
                           @NotNull String roomName, @NotNull String mapName,
                           int players, int capacity,
                           boolean started, byte @Nullable [] joinKey,
                           byte @NotNull [] modData) {
        if (roomName.isEmpty() || WireWriter.getTextLength(roomName) > MAXIMUM_NAME_LENGTH ||
            WireWriter.getTextLength(mapName) > MAXIMUM_NAME_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The room name {0} or the map name {1} is empty or longer than {2} UTF-8 bytes.",
                                                         roomName, mapName, MAXIMUM_NAME_LENGTH);
        }

        if (capacity < 1 || capacity > MAXIMUM_CAPACITY || players < 0 || players > capacity) {
            throw Exceptions.newIllegalArgumentException("The players {0} and the capacity {1} are out of range.", players, capacity);
        }

        if (modData.length > MAXIMUM_MOD_DATA_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The mod data has {0} bytes, more than {1}.", modData.length, MAXIMUM_MOD_DATA_LENGTH);
        }

        if (joinKey != null && joinKey.length != JOIN_KEY_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The join key has {0} bytes, not {1}.", joinKey.length, JOIN_KEY_LENGTH);
        }

        this.fingerprint = fingerprint;

        this.roomName = roomName;
        this.mapName  = mapName;

        this.players  = players;
        this.capacity = capacity;

        this.started = started;
        this.joinKey = joinKey == null ? null : joinKey.clone();

        this.modData = modData.clone();
    }

    /**
     * @return the fingerprint of what the room runs
     */
    public @NotNull ModFingerprint getFingerprint() {
        return this.fingerprint;
    }

    /**
     * @return the room name
     */
    public @NotNull String getRoomName() {
        return this.roomName;
    }

    /**
     * @return the map name
     */
    public @NotNull String getMapName() {
        return this.mapName;
    }

    /**
     * @return the number of players in the room
     */
    public int getPlayers() {
        return this.players;
    }

    /**
     * @return the largest number of players
     */
    public int getCapacity() {
        return this.capacity;
    }

    /**
     * @return {@code true} if the room's game started, {@code false} otherwise
     */
    public boolean isStarted() {
        return this.started;
    }

    /**
     * @return {@code true} if the room is private: a joiner proves its password with the join key, {@code false} otherwise
     */
    public boolean isPrivate() {
        return this.joinKey != null;
    }

    /**
     * @return a copy of the private room's join key, or {@code null} for a public room
     */
    public byte @Nullable [] getJoinKey() {
        return this.joinKey == null ? null : this.joinKey.clone();
    }

    /**
     * @return a copy of the mod's data about the room, empty if the mod gave none
     */
    public byte @NotNull [] getModData() {
        return this.modData.clone();
    }

    /**
     * @return the length of the description's wire form
     */
    public int getEncodedLength() {
        return this.fingerprint.getEncodedLength() + 1 + WireWriter.getTextLength(this.roomName) + 1 + WireWriter.getTextLength(this.mapName) +
               COUNT_LENGTH * 2 + FLAGS_LENGTH + (this.joinKey == null ? 0 : JOIN_KEY_LENGTH) + MOD_DATA_LENGTH_LENGTH + this.modData.length;
    }


    /**
     * Writes the description.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        this.fingerprint.write(writer);
        writer.writeText(this.roomName, MAXIMUM_NAME_LENGTH);
        writer.writeText(this.mapName, MAXIMUM_NAME_LENGTH);
        writer.writeUnsigned8(this.players);
        writer.writeUnsigned8(this.capacity);
        writer.writeUnsigned8((this.joinKey == null ? 0 : PRIVATE_FLAG) | (this.started ? STARTED_FLAG : 0));

        if (this.joinKey != null) {
            writer.writeBytes(this.joinKey);
        }

        writer.writeUnsigned8(this.modData.length);
        writer.writeBytes(this.modData);
    }

    /**
     * @return the room name, the map and the players, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.roomName + " (" + this.mapName + ", " + this.players + "/" + this.capacity + ")";
    }
}
