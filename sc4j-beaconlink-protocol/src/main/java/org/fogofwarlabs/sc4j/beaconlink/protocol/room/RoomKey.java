package org.fogofwarlabs.sc4j.beaconlink.protocol.room;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Identifies a room: its host's key, the connection number of the host's session that holds the room, and the room number the host chose (an
 * unsigned 32-bit value). Only the host's key can sign advertisements of its rooms, so the key cannot be taken over by another player; the
 * connection tells the rooms of two sessions of one account apart and names the session a joiner signals.
 */
public final class RoomKey implements Comparable<RoomKey> {
    /**
     * The largest room number.
     */
    public static final long MAXIMUM_NUMBER = 0xffffffffL;

    private final @NotNull IdentityKey host;
    private final long connection;
    private final long number;

    /**
     * Creates a {@link RoomKey} instance.
     *
     * @param host       the host's key
     * @param connection the connection number of the host's session that holds the room
     * @param number     the room number, {@code 0} to {@value #MAXIMUM_NUMBER}
     * @throws IllegalArgumentException if the number is out of range
     */
    public RoomKey(@NotNull IdentityKey host, long connection, long number) {
        if (number < 0L || number > MAXIMUM_NUMBER) {
            throw Exceptions.newIllegalArgumentException("The room number {0} is outside 0..{1}.", number, MAXIMUM_NUMBER);
        }

        this.host       = host;
        this.connection = connection;
        this.number     = number;
    }

    /**
     * @return the host's key
     */
    public @NotNull IdentityKey getHost() {
        return this.host;
    }

    /**
     * @return the connection number of the host's session that holds the room
     */
    public long getConnection() {
        return this.connection;
    }

    /**
     * @return the room number
     */
    public long getNumber() {
        return this.number;
    }


    /**
     * Orders room keys by host key, then by connection number (unsigned), then by room number.
     *
     * @param other the other room key
     * @return a negative value, zero or a positive value if this key orders before, equal to or after the other key
     */
    @Override
    public int compareTo(@NotNull RoomKey other) {
        int order = this.host.compareTo(other.host);

        if (order == 0) {
            order = Long.compareUnsigned(this.connection, other.connection);
        }

        return order != 0 ? order : Long.compare(this.number, other.number);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is a room key with the same host, connection and number, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof RoomKey key && this.host.equals(key.host) && this.connection == key.connection && this.number == key.number;
    }

    /**
     * @return the hash code of the host, the connection and the number
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.host, this.connection, this.number);
    }

    /**
     * @return the host, the connection and the number, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.host + "/" + Long.toHexString(this.connection) + "/" + this.number;
    }
}
