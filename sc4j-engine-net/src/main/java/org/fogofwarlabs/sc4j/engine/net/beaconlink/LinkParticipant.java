package org.fogofwarlabs.sc4j.engine.net.beaconlink;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Identifies one Beacon.Link connection of a player: the player ({@link LinkPlayer}, its account key) and the connection number its session
 * chose at its start. One account may hold several connections at once (the launcher, a game, another computer); channels, rooms, the
 * overlay, the lobby and the lockstep transport address one connection. Two participants are equal when their players and their connection
 * numbers are equal.
 * <p>
 * Wire form ({@link #getBytes()}, {@link #of(byte[])}): the player key ({@value LinkPlayer#KEY_LENGTH} bytes) and the connection number
 * (eight bytes, big-endian), {@value #LENGTH} bytes.
 */
public final class LinkParticipant {
    /**
     * The byte length of a participant's wire form: the player key and the connection number.
     */
    public static final int LENGTH = LinkPlayer.KEY_LENGTH + Long.BYTES;

    /**
     * Creates a {@link LinkParticipant} instance.
     *
     * @param player     the player
     * @param connection the connection number
     * @return the created {@link LinkParticipant} instance
     */
    public static @NotNull LinkParticipant of(@NotNull LinkPlayer player, long connection) {
        return new LinkParticipant(player, connection);
    }

    /**
     * Creates a {@link LinkParticipant} instance from its wire form.
     *
     * @param bytes the player key and the connection number, {@value #LENGTH} bytes
     * @return the created {@link LinkParticipant} instance
     * @throws IllegalArgumentException if the length is not {@value #LENGTH}
     */
    public static @NotNull LinkParticipant of(byte @NotNull [] bytes) {
        if (bytes.length != LENGTH) {
            throw Exceptions.newIllegalArgumentException("A participant has {0} bytes, not {1}.", LENGTH, bytes.length);
        }

        return new LinkParticipant(LinkPlayer.of(Arrays.copyOf(bytes, LinkPlayer.KEY_LENGTH)),
                                   ByteBuffer.wrap(bytes, LinkPlayer.KEY_LENGTH, Long.BYTES).getLong());
    }


    private final @NotNull LinkPlayer player;
    private final long connection;

    private LinkParticipant(@NotNull LinkPlayer player, long connection) {
        this.player     = player;
        this.connection = connection;
    }

    /**
     * @return the player whose connection this is
     */
    public @NotNull LinkPlayer getPlayer() {
        return this.player;
    }

    /**
     * @return the connection number the player's session chose at its start
     */
    public long getConnection() {
        return this.connection;
    }

    /**
     * @return the wire form: the player key and the connection number, {@value #LENGTH} bytes
     */
    public byte @NotNull [] getBytes() {
        return ByteBuffer.allocate(LENGTH).put(this.player.getKey()).putLong(this.connection).array();
    }


    /**
     * @param other the other object
     * @return {@code true} if the other object is a participant with the same player and connection number, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof LinkParticipant participant && this.player.equals(participant.player) && this.connection == participant.connection;
    }

    /**
     * @return the hash code of the player and the connection number
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.connection);
    }

    /**
     * @return the player and the connection number in hexadecimal, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.player + "/" + Long.toHexString(this.connection);
    }
}
