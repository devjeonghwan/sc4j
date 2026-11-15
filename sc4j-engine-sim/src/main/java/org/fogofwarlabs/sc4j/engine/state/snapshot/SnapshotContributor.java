package org.fogofwarlabs.sc4j.engine.state.snapshot;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Represents a part of the simulation state that a state snapshot ({@link StateSnapshot}) holds: it writes every value the later frames of its
 * game depend on and reads them back into the same part of another game created from the same definition and setup.
 * <p>
 * Every part that contributes to the state hash is a snapshot contributor too, and so is a part with state the hash leaves out (an order
 * queue pool, the sorted edges of a spatial index, a computer player's plan). A part that holds no state between frames (caches rebuilt every
 * frame, values derived from the definition or the setup) writes nothing.
 * <p>
 * Determinism contract: a contributor writes the same values in the same order for the same state, iterates arrays and lists by index, and
 * reads exactly what it wrote, in the same order. After {@link #readSnapshot} the part behaves in every later frame as the written part did.
 * Object references are written as their slot or type index ({@link StateSnapshotWriter#writeEntity}, {@link StateSnapshotWriter#writeType},
 * {@link StateSnapshotWriter#writePlayer}) and read back as the objects of the reading game.
 */
public interface SnapshotContributor {
    /**
     * Writes this part of the simulation state.
     *
     * @param writer the writer of the snapshot
     */
    void writeSnapshot(@NotNull StateSnapshotWriter writer);

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or its values do not fit this part (another size or an unknown reference)
     */
    void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException;
}
