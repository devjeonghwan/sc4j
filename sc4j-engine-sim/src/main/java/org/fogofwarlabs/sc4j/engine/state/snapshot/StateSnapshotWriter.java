package org.fogofwarlabs.sc4j.engine.state.snapshot;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.entity.Entity;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Writes the values of a state snapshot in memory, for the {@link SnapshotContributor}s of one game ({@link StateSnapshot#capture}).
 * <p>
 * Encoding (big-endian, {@link DataOutputStream}): an {@code int} in four bytes, a {@code long} in eight, a {@code boolean} in one byte
 * ({@code 1} or {@code 0}); an array as its length (four bytes) and its values. References are written as plain values: an entity as its slot
 * index ({@code 0} for none), a content type as its type index ({@link ContentType#NO_INDEX} for none), a player as its slot index
 * ({@link #NO_PLAYER} for none).
 */
public final class StateSnapshotWriter {
    /**
     * The value written for a missing player.
     */
    public static final int NO_PLAYER = -1;
    /**
     * The value written for a missing entity; slot indices start at {@code 1}.
     */
    public static final int NO_ENTITY = 0;

    private final @NotNull ByteArrayOutputStream bytes;
    private final @NotNull DataOutputStream output;

    /**
     * Creates a {@link StateSnapshotWriter} instance with no values.
     */
    StateSnapshotWriter() {
        this.bytes  = new ByteArrayOutputStream();
        this.output = new DataOutputStream(this.bytes);
    }

    /**
     * Writes an {@code int} value.
     *
     * @param value the value
     */
    public void writeInt(int value) {
        try {
            this.output.writeInt(value);
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }
    }

    /**
     * Writes a {@code long} value.
     *
     * @param value the value
     */
    public void writeLong(long value) {
        try {
            this.output.writeLong(value);
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }
    }

    /**
     * Writes a {@code boolean} value.
     *
     * @param value the value
     */
    public void writeBoolean(boolean value) {
        try {
            this.output.writeBoolean(value);
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }
    }

    /**
     * Writes the length and every value of an {@code int} array, in index order.
     *
     * @param values the values
     */
    public void writeInts(int @NotNull [] values) {
        this.writeInt(values.length);

        for (int value : values) {
            this.writeInt(value);
        }
    }

    /**
     * Writes the length and every value of a {@code boolean} array, in index order.
     *
     * @param values the values
     */
    public void writeBooleans(boolean @NotNull [] values) {
        this.writeInt(values.length);

        for (boolean value : values) {
            this.writeBoolean(value);
        }
    }

    /**
     * Writes the length and every value of a {@code byte} array, in index order.
     *
     * @param values the values
     */
    public void writeBytes(byte @NotNull [] values) {
        this.writeInt(values.length);

        try {
            this.output.write(values);
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }
    }

    /**
     * Writes a reference to an entity as its slot index.
     *
     * @param entity the entity, or {@code null} for none
     */
    public void writeEntity(@Nullable Entity entity) {
        this.writeInt(entity != null ? entity.getIndex() : NO_ENTITY);
    }

    /**
     * Writes a reference to a content type as its type index.
     *
     * @param type the type, or {@code null} for none
     */
    public void writeType(@Nullable ContentType type) {
        this.writeInt(ContentType.indexOf(type));
    }

    /**
     * Writes a reference to a player as its slot index.
     *
     * @param player the player, or {@code null} for none
     */
    public void writePlayer(@Nullable Player player) {
        this.writeInt(player != null ? player.getIndex() : NO_PLAYER);
    }

    /**
     * @return the values written so far
     */
    byte @NotNull [] toByteArray() {
        return this.bytes.toByteArray();
    }
}
