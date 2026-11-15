package org.fogofwarlabs.sc4j.engine.client.community;

import org.fogofwarlabs.sc4j.engine.net.beaconlink.LatencyClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Holds one row of a {@link TableBox}: the texts of its columns, the colour they draw in and the latency its bars show.
 */
public final class TableRow {
    private final @NotNull List<@NotNull String> cells;
    private final int color;
    private final @Nullable LatencyClass latency;

    /**
     * Creates a {@link TableRow} instance.
     *
     * @param cells   the texts of the columns in order, copied; missing columns stay empty
     * @param color   the packed {@code 0xRRGGBBAA} colour of the texts
     * @param latency the latency the bars show, {@link LatencyClass#UNKNOWN} for no bars, or {@code null} for a row without a latency
     */
    public TableRow(@NotNull List<@NotNull String> cells, int color, @Nullable LatencyClass latency) {
        this.cells   = List.copyOf(cells);
        this.color   = color;
        this.latency = latency;
    }

    /**
     * @return the texts of the columns in order
     */
    public @NotNull List<@NotNull String> getCells() {
        return this.cells;
    }

    /**
     * @return the packed {@code 0xRRGGBBAA} colour of the texts
     */
    public int getColor() {
        return this.color;
    }

    /**
     * @return the latency the bars show, or {@code null} for a row without a latency
     */
    public @Nullable LatencyClass getLatency() {
        return this.latency;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The {@code equals} method implements an equivalence relation on non-null object references.
     *
     * @param other the reference object with which to compare
     * @return {@code true} if this object is the same as the other argument, {@code false} otherwise
     * @implSpec Two rows are equal when their cells, colours and latencies are equal.
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof TableRow row && this.cells.equals(row.cells) && this.color == row.color && this.latency == row.latency;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object
     * @implSpec Combines the hash codes of the cells, the colour and the latency, consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.cells, this.color, this.latency);
    }

    /**
     * {@return a string representation of the object}
     *
     * @implSpec Returns the cells and the latency, for logs.
     */
    @Override
    public @NotNull String toString() {
        return this.cells + " " + this.latency;
    }
}
