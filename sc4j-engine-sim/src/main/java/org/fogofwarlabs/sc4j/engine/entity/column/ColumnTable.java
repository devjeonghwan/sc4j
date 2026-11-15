package org.fogofwarlabs.sc4j.engine.entity.column;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * Holds the extension columns of one pool or one content type table in registration order.
 * <p>
 * Determinism contract: allocation resets the {@link ColumnReset#ON_ALLOCATE} columns of the slot and the state hash
 * mixes every column of a slot, both in registration order; the state snapshot holds every column whole, in registration order.
 */
public final class ColumnTable implements SnapshotContributor {
    private final @NotNull ColumnKey<?> @NotNull [] keys;
    private final @NotNull Column @NotNull [] columns;
    private final @NotNull Column @NotNull [] resetColumns;
    private final int length;

    /**
     * Creates a {@link ColumnTable} instance with one column per key.
     *
     * @param keys   the keys in registration order
     * @param length the number of values of each column, the pool capacity plus one, or the type count of a content table
     * @throws IllegalArgumentException if a key is listed twice
     */
    public ColumnTable(@NotNull List<@NotNull ColumnKey<?>> keys, int length) {
        this.keys    = keys.toArray(new ColumnKey<?>[0]);
        this.columns = new Column[this.keys.length];

        int resetCount = 0;

        for (int index = 0; index < this.keys.length; index++) {
            for (int earlier = 0; earlier < index; earlier++) {
                if (this.keys[earlier] == this.keys[index]) {
                    throw Exceptions.newIllegalArgumentException("The column {0} is registered twice.", this.keys[index]);
                }
            }

            this.columns[index] = this.keys[index].createColumn(length);

            if (this.keys[index].getReset() == ColumnReset.ON_ALLOCATE) {
                resetCount++;
            }
        }

        this.resetColumns = new Column[resetCount];
        this.length       = length;

        int resetIndex = 0;

        for (Column column : this.columns) {
            if (column.getKey().getReset() == ColumnReset.ON_ALLOCATE) {
                this.resetColumns[resetIndex] = column;
                resetIndex++;
            }
        }
    }

    /**
     * @return the number of columns
     */
    public int getCount() {
        return this.columns.length;
    }

    /**
     * @return the number of values of each column, also when the table has no column
     */
    public int getLength() {
        return this.length;
    }

    /**
     * Returns the key of one column.
     *
     * @param index the column position in registration order
     * @return the key
     */
    public @NotNull ColumnKey<?> getKey(int index) {
        return this.keys[index];
    }

    /**
     * Returns the column of a key. A system calls this once, when it is attached, and keeps the column.
     *
     * @param <C> the column type
     * @param key the key
     * @return the column
     * @throws IllegalArgumentException if the key is not registered for this pool
     */
    public <C extends Column> @NotNull C get(@NotNull ColumnKey<C> key) {
        for (int index = 0; index < this.keys.length; index++) {
            if (this.keys[index] == key) {
                return key.getColumnType().cast(this.columns[index]);
            }
        }

        throw Exceptions.newIllegalArgumentException("The column {0} is not registered for this pool.", key);
    }

    /**
     * Checks whether a key is registered for this pool.
     *
     * @param key the key
     * @return {@code true} if the key is registered, {@code false} otherwise
     */
    public boolean contains(@NotNull ColumnKey<?> key) {
        for (ColumnKey<?> registered : this.keys) {
            if (registered == key) {
                return true;
            }
        }

        return false;
    }


    /**
     * Sets the {@link ColumnReset#ON_ALLOCATE} columns of one slot to their defaults, in registration order.
     *
     * @param index the slot index
     */
    public void resetSlot(int index) {
        for (Column column : this.resetColumns) {
            column.reset(index);
        }
    }

    /**
     * Returns the number of writes to the columns of this table: the sum of their {@link Column#getWriteCount()}, which
     * only grows.
     *
     * @return the write count
     */
    public long getWriteCount() {
        long writeCount = 0L;

        for (Column column : this.columns) {
            writeCount += column.getWriteCount();
        }

        return writeCount;
    }

    /**
     * Mixes every column value of one slot into the hash, in registration order.
     *
     * @param hash  the hash being computed
     * @param index the slot index
     */
    public void contributeStateHash(@NotNull StateHash hash, int index) {
        for (Column column : this.columns) {
            column.contributeStateHash(hash, index);
        }
    }

    /**
     * Writes every column whole, in registration order.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInt(this.columns.length);

        for (Column column : this.columns) {
            column.writeSnapshot(writer);
        }
    }

    /**
     * Reads every column back, in registration order, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or holds other columns
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readLength(this.columns.length);

        for (Column column : this.columns) {
            column.readSnapshot(reader);
        }
    }
}
