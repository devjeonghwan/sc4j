package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.jetbrains.annotations.NotNull;

/**
 * Caches the state hash digest of one content table: the hash of all its values, recomputed only after a write.
 * <p>
 * The table counts its own writes here ({@link #recordWrite()}) and adds the write counts of its extension columns; a
 * digest computed at one total write count stays valid until the total changes. Write counts only grow, so an unchanged
 * total means that no value was written since the digest was computed.
 */
final class TableDigest {
    private final @NotNull StateHash hash;

    private long writeCount;
    private boolean computed;
    private long computedWriteCount;
    private long value;

    /**
     * Creates a {@link TableDigest} instance without a digest.
     */
    TableDigest() {
        this.hash = new StateHash();

        this.writeCount         = 0L;
        this.computed           = false;
        this.computedWriteCount = 0L;
        this.value              = 0L;
    }

    /**
     * @return the number of writes the table counted
     */
    long getWriteCount() {
        return this.writeCount;
    }

    /**
     * @return the digest of the last {@link #finish}
     */
    long getValue() {
        return this.value;
    }


    /**
     * Counts one write of the table.
     */
    void recordWrite() {
        this.writeCount++;
    }

    /**
     * Checks whether the digest must be recomputed.
     *
     * @param totalWriteCount the writes of the table and of its extension columns
     * @return {@code true} if no digest was computed at this total, {@code false} otherwise
     */
    boolean needsUpdate(long totalWriteCount) {
        return !this.computed || totalWriteCount != this.computedWriteCount;
    }

    /**
     * Starts a new digest.
     *
     * @return the hash the table mixes all its values into, reset to its initial value
     */
    @NotNull StateHash start() {
        return this.hash.reset();
    }

    /**
     * Keeps the hash started by {@link #start()} as the digest at a total write count.
     *
     * @param totalWriteCount the writes of the table and of its extension columns when the digest was computed
     */
    void finish(long totalWriteCount) {
        this.value              = this.hash.getValue();
        this.computedWriteCount = totalWriteCount;
        this.computed           = true;
    }
}
