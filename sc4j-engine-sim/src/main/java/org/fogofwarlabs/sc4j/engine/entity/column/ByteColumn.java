package org.fogofwarlabs.sc4j.engine.entity.column;

import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

/**
 * Holds one {@code byte} per entity, for small counters and flag sets.
 */
public final class ByteColumn extends Column {
    private final byte defaultValue;
    private final byte @NotNull [] values;

    /**
     * Creates a {@link ByteColumn} instance with every value at the default.
     *
     * @param key          the key the column is created for
     * @param defaultValue the default value
     * @param length       the number of values, the pool capacity plus one, or the type count of a content table
     */
    ByteColumn(@NotNull ColumnKey<ByteColumn> key, byte defaultValue, int length) {
        super(key);

        this.defaultValue = defaultValue;
        this.values       = new byte[length];

        Arrays.fill(this.values, defaultValue);
    }

    /**
     * @return the number of values, the pool capacity plus one, or the type count of a content table
     */
    @Override
    public int getLength() {
        return this.values.length;
    }

    /**
     * Returns the value of one entity.
     *
     * @param index the entity index
     * @return the value
     */
    public byte get(int index) {
        return this.values[index];
    }

    /**
     * Sets the value of one entity.
     *
     * @param index the entity index
     * @param value the value
     */
    public void set(int index, byte value) {
        this.values[index] = value;

        this.recordWrite();
    }


    /**
     * Sets the value of one entity to the column default.
     *
     * @param index the entity index
     */
    @Override
    public void reset(int index) {
        this.values[index] = this.defaultValue;

        this.recordWrite();
    }

    /**
     * Mixes the value of one entity into the hash.
     *
     * @param hash  the hash being computed
     * @param index the entity index
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash, int index) {
        hash.mix(this.values[index]);
    }

    /**
     * Writes every value of the column, in index order.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeBytes(this.values);
    }

    /**
     * Reads every value of the column back, replacing the current values; counts one write.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or holds another number of values
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readBytes(this.values);

        this.recordWrite();
    }
}
