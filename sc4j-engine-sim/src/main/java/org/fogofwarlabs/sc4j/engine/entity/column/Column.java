package org.fogofwarlabs.sc4j.engine.entity.column;

import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Holds the values of one extension column of a pool, indexed by the entity index, or of a content type table, indexed
 * by the type index. In a pool, index {@code 0} exists but names no entity.
 */
public abstract sealed class Column permits IntColumn, ByteColumn, BooleanColumn {
    private final @NotNull ColumnKey<?> key;

    private long writeCount;

    /**
     * Creates a {@link Column} instance for a key.
     *
     * @param key the key the column was created for
     */
    Column(@NotNull ColumnKey<?> key) {
        this.key = key;

        this.writeCount = 0L;
    }

    /**
     * @return the key the column was created for
     */
    public @NotNull ColumnKey<?> getKey() {
        return this.key;
    }

    /**
     * @return the number of values, the pool capacity plus one, or the type count of a content table
     */
    public abstract int getLength();

    /**
     * Returns the number of writes to this column: every set and every reset counts one, whether or not it changes the
     * value. The count only grows, so a reader that remembers it can tell that the column was not written since.
     *
     * @return the write count
     */
    public final long getWriteCount() {
        return this.writeCount;
    }


    /**
     * Sets the value of one entity to the column default.
     *
     * @param index the entity index
     */
    public abstract void reset(int index);

    /**
     * Mixes the value of one entity into the hash.
     *
     * @param hash  the hash being computed
     * @param index the entity index
     */
    public abstract void contributeStateHash(@NotNull StateHash hash, int index);

    /**
     * Writes every value of the column, in index order.
     *
     * @param writer the writer of the snapshot
     */
    public abstract void writeSnapshot(@NotNull StateSnapshotWriter writer);

    /**
     * Reads every value of the column back, replacing the current values; counts one write.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or holds another number of values
     */
    public abstract void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException;

    /**
     * Counts one write; every set and reset of a subclass calls it.
     */
    final void recordWrite() {
        this.writeCount++;
    }
}
