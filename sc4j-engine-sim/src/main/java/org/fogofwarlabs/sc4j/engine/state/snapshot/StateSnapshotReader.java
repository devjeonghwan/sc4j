package org.fogofwarlabs.sc4j.engine.state.snapshot;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.TypeRegistry;
import org.fogofwarlabs.sc4j.engine.entity.Entity;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Reads the values of a state snapshot back into one game ({@link StateSnapshot#restore}), for its {@link SnapshotContributor}s, in the
 * encoding of {@link StateSnapshotWriter}. References are read back as the objects of the reading game: an entity of a named pool, a content
 * type of a named registry, a player of the game.
 * <p>
 * Every read checks what it reads: an array must have the length of the part it is read into, a reference must name an object of the game.
 * A snapshot that ends early is reported as an {@link IOException} too.
 */
public final class StateSnapshotReader {
    private final @NotNull Game game;
    private final @NotNull DataInputStream input;

    /**
     * Creates a {@link StateSnapshotReader} instance over the values of a snapshot.
     *
     * @param game   the game the snapshot is read into
     * @param values the values of the snapshot
     */
    StateSnapshotReader(@NotNull Game game, byte @NotNull [] values) {
        this.game  = game;
        this.input = new DataInputStream(new ByteArrayInputStream(values));
    }

    /**
     * @return the game the snapshot is read into, whose players and pools the references name
     */
    public @NotNull Game getGame() {
        return this.game;
    }

    /**
     * Reads an {@code int} value.
     *
     * @return the value
     * @throws IOException if the snapshot ends
     */
    public int readInt() throws IOException {
        try {
            return this.input.readInt();
        } catch (EOFException exception) {
            throw Exceptions.newIOException(exception, "The state snapshot ends early.");
        }
    }

    /**
     * Reads a {@code long} value.
     *
     * @return the value
     * @throws IOException if the snapshot ends
     */
    public long readLong() throws IOException {
        try {
            return this.input.readLong();
        } catch (EOFException exception) {
            throw Exceptions.newIOException(exception, "The state snapshot ends early.");
        }
    }

    /**
     * Reads a {@code boolean} value.
     *
     * @return the value
     * @throws IOException if the snapshot ends or the byte is neither {@code 0} nor {@code 1}
     */
    public boolean readBoolean() throws IOException {
        int value;

        try {
            value = this.input.readUnsignedByte();
        } catch (EOFException exception) {
            throw Exceptions.newIOException(exception, "The state snapshot ends early.");
        }

        if (value > 1) {
            throw Exceptions.newIOException("Invalid boolean value {0} in the state snapshot.", value);
        }

        return value == 1;
    }

    /**
     * Reads the length of an array and checks it.
     *
     * @param expectedLength the length of the array the values are read into
     * @return the length
     * @throws IOException if the snapshot ends or the length differs
     */
    public int readLength(int expectedLength) throws IOException {
        int length = this.readInt();

        if (length != expectedLength) {
            throw Exceptions.newIOException("The state snapshot holds {0} values where the game holds {1}.", length, expectedLength);
        }

        return length;
    }

    /**
     * Reads an {@code int} array written by {@link StateSnapshotWriter#writeInts} into an array of the same length.
     *
     * @param values receives the values
     * @throws IOException if the snapshot ends or the written array has another length
     */
    public void readInts(int @NotNull [] values) throws IOException {
        this.readLength(values.length);

        for (int index = 0; index < values.length; index++) {
            values[index] = this.readInt();
        }
    }

    /**
     * Reads an {@code int} array written by {@link StateSnapshotWriter#writeInts} whose length the snapshot decides.
     *
     * @return the created array
     * @throws IOException if the snapshot ends before the values or the length is negative
     */
    public int @NotNull [] readIntArray() throws IOException {
        int length = this.readInt();

        if (length < 0 || length > this.input.available() / Integer.BYTES) {
            throw Exceptions.newIOException("Invalid array length {0} in the state snapshot.", length);
        }

        int[] values = new int[length];

        for (int index = 0; index < length; index++) {
            values[index] = this.readInt();
        }

        return values;
    }

    /**
     * Reads a {@code boolean} array written by {@link StateSnapshotWriter#writeBooleans} into an array of the same length.
     *
     * @param values receives the values
     * @throws IOException if the snapshot ends, the written array has another length or a value is no {@code boolean}
     */
    public void readBooleans(boolean @NotNull [] values) throws IOException {
        this.readLength(values.length);

        for (int index = 0; index < values.length; index++) {
            values[index] = this.readBoolean();
        }
    }

    /**
     * Reads a {@code byte} array written by {@link StateSnapshotWriter#writeBytes} into an array of the same length.
     *
     * @param values receives the values
     * @throws IOException if the snapshot ends or the written array has another length
     */
    public void readBytes(byte @NotNull [] values) throws IOException {
        this.readLength(values.length);

        try {
            this.input.readFully(values);
        } catch (EOFException exception) {
            throw Exceptions.newIOException(exception, "The state snapshot ends early.");
        }
    }

    /**
     * Reads a reference to an entity of a pool.
     *
     * @param <E>  the entity class of the pool
     * @param pool the pool the reference names an entity of
     * @return the entity, or {@code null} if the reference names none
     * @throws IOException if the snapshot ends or the slot index is outside the pool
     */
    public <E extends Entity> @Nullable E readEntity(@NotNull EntityPool<E> pool) throws IOException {
        int index = this.readInt();

        if (index == StateSnapshotWriter.NO_ENTITY) {
            return null;
        }

        if (index < 1 || index > pool.getCapacity()) {
            throw Exceptions.newIOException("The state snapshot names the slot {0} of a pool of {1}.", index, pool.getCapacity());
        }

        return pool.get(index);
    }

    /**
     * Reads a reference to a content type of a registry.
     *
     * @param <T>   the content kind
     * @param types the registry the reference names a type of
     * @return the type, or {@code null} if the reference names none
     * @throws IOException if the snapshot ends or the index names no type of the registry
     */
    public <T extends ContentType> @Nullable T readType(@NotNull TypeRegistry<T> types) throws IOException {
        int index = this.readInt();

        if (index == ContentType.NO_INDEX) {
            return null;
        }

        if (index < 0 || index >= types.getCount()) {
            throw Exceptions.newIOException("The state snapshot names the {0} {1} of {2}.", types.getKindName(), index, types.getCount());
        }

        return types.get(index);
    }

    /**
     * Reads a reference to a player of the game.
     *
     * @return the player, or {@code null} if the reference names none
     * @throws IOException if the snapshot ends or the index names no player of the game
     */
    public @Nullable Player readPlayer() throws IOException {
        int index = this.readInt();

        if (index == StateSnapshotWriter.NO_PLAYER) {
            return null;
        }

        if (index < 0 || index >= this.game.getPlayerCount()) {
            throw Exceptions.newIOException("The state snapshot names the player {0} of {1}.", index, this.game.getPlayerCount());
        }

        return this.game.getPlayer(index);
    }

    /**
     * @return {@code true} if every value of the snapshot was read, {@code false} otherwise
     * @throws IOException if the values cannot be read
     */
    boolean isFinished() throws IOException {
        return this.input.available() == 0;
    }
}
