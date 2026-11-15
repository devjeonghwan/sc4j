package org.fogofwarlabs.sc4j.engine.net.beaconlink;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a Beacon.Link room as other participants see it in a search: the room name, the map name, the player counts, whether its game
 * started and the mod's own data about the room (for example its game type and speed), which the engine carries unread. The room's mod
 * identity (with the engine version) is the run's own; the session adds it, and whether the room is private follows from its password
 * ({@link BeaconLinkSession#publishRoom(RoomSettings, String)}).
 */
public final class RoomSettings {
    /**
     * The largest UTF-8 length of the room name and of the map name.
     */
    public static final int MAXIMUM_NAME_LENGTH = 48;
    /**
     * The largest length of the mod's data about a room.
     */
    public static final int MAXIMUM_MOD_DATA_LENGTH = 64;

    private static final byte @NotNull [] NO_MOD_DATA = new byte[0];

    private final @NotNull String roomName;
    private final @NotNull String mapName;

    private final int players;
    private final int capacity;

    private final boolean started;

    private final byte @NotNull [] modData;

    /**
     * Creates a {@link RoomSettings} instance without mod data.
     *
     * @param roomName the room name, not empty
     * @param mapName  the map name
     * @param players  the number of players in the room, {@code 0} to the capacity
     * @param capacity the largest number of players, at least {@code 1}
     * @throws IllegalArgumentException if the room name is empty or a count is out of range
     */
    public RoomSettings(@NotNull String roomName, @NotNull String mapName, int players, int capacity) {
        this(roomName, mapName, players, capacity, false, NO_MOD_DATA);
    }

    /**
     * Creates a {@link RoomSettings} instance of a room whose game has not started.
     *
     * @param roomName the room name, not empty
     * @param mapName  the map name
     * @param players  the number of players in the room, {@code 0} to the capacity
     * @param capacity the largest number of players, at least {@code 1}
     * @param modData  the mod's data about the room, at most {@value #MAXIMUM_MOD_DATA_LENGTH} bytes, copied
     * @throws IllegalArgumentException if the room name is empty, a count is out of range or the mod data is too long
     */
    public RoomSettings(@NotNull String roomName, @NotNull String mapName, int players, int capacity, byte @NotNull [] modData) {
        this(roomName, mapName, players, capacity, false, modData);
    }

    /**
     * Creates a {@link RoomSettings} instance.
     *
     * @param roomName the room name, not empty
     * @param mapName  the map name
     * @param players  the number of players in the room, {@code 0} to the capacity
     * @param capacity the largest number of players, at least {@code 1}
     * @param started  whether the room's game started
     * @param modData  the mod's data about the room, at most {@value #MAXIMUM_MOD_DATA_LENGTH} bytes, copied
     * @throws IllegalArgumentException if the room name is empty, a count is out of range or the mod data is too long
     */
    public RoomSettings(@NotNull String roomName, @NotNull String mapName, int players, int capacity, boolean started, byte @NotNull [] modData) {
        if (roomName.isEmpty()) {
            throw Exceptions.newIllegalArgumentException("A room needs a name.");
        }

        if (capacity < 1 || players < 0 || players > capacity) {
            throw Exceptions.newIllegalArgumentException("The players {0} and the capacity {1} are out of range.", players, capacity);
        }

        if (modData.length > MAXIMUM_MOD_DATA_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The mod data has {0} bytes, more than {1}.", modData.length, MAXIMUM_MOD_DATA_LENGTH);
        }

        this.roomName = roomName;
        this.mapName  = mapName;

        this.players  = players;
        this.capacity = capacity;

        this.started = started;

        this.modData = modData.clone();
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
     * @return a copy of the mod's data about the room, empty if the mod gave none
     */
    public byte @NotNull [] getModData() {
        return this.modData.clone();
    }


    /**
     * @return the room name, the map and the players, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.roomName + " (" + this.mapName + ", " + this.players + "/" + this.capacity + ")";
    }
}
