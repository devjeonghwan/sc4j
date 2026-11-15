package org.fogofwarlabs.sc4j.engine.spatial;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collects the units a query returns, in the order the query found them. The caller creates one list with a fixed
 * capacity and passes it to every query, which clears it first; a query allocates nothing. Units beyond the capacity are
 * not collected.
 */
public final class UnitList {
    // Only the slots below the size hold collected units; the others are null until a query first fills them.
    private final @Nullable Unit @NotNull [] units;

    private int size;

    /**
     * Creates an empty {@link UnitList} instance.
     *
     * @param capacity the largest number of units a query collects, at least {@code 1}
     * @throws IllegalArgumentException if the capacity is below {@code 1}
     */
    public UnitList(int capacity) {
        if (capacity < 1) {
            throw Exceptions.newIllegalArgumentException("Invalid unit list capacity: {0}", capacity);
        }

        this.units = new Unit[capacity];

        this.size = 0;
    }

    /**
     * @return the largest number of units the list collects
     */
    public int getCapacity() {
        return this.units.length;
    }

    /**
     * @return the number of collected units
     */
    public int size() {
        return this.size;
    }

    /**
     * @return {@code true} if no unit is collected, {@code false} otherwise
     */
    public boolean isEmpty() {
        return this.size == 0;
    }

    /**
     * @return {@code true} if the list holds its capacity and collects no further unit, {@code false} otherwise
     */
    public boolean isFull() {
        return this.size == this.units.length;
    }

    /**
     * Returns one collected unit.
     *
     * @param position the position in collection order, below {@link #size()}
     * @return the unit
     * @throws IndexOutOfBoundsException if the position is not below the size
     */
    public @NotNull Unit get(int position) {
        if (position < 0 || position >= this.size) {
            throw Exceptions.newIndexOutOfBoundsException("No unit at {0}; the list holds {1}.", position, this.size);
        }

        return this.units[position];
    }


    /**
     * Collects a unit after the collected ones.
     *
     * @param unit the unit
     * @return {@code true} if the unit was collected, {@code false} if the list is full
     */
    public boolean add(@NotNull Unit unit) {
        if (this.isFull()) {
            return false;
        }

        this.units[this.size++] = unit;

        return true;
    }

    /**
     * Removes every collected unit; the references are kept until they are overwritten.
     */
    public void clear() {
        this.size = 0;
    }
}
