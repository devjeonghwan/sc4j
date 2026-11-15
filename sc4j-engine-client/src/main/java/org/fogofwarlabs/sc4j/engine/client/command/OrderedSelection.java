package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Holds the units a local player has selected, in selection order, each unit once and at most {@link #getCapacity} of them: the engine default
 * {@link Selection}, changed by the client's own input (a click, a selection box, a control group recall).
 */
public final class OrderedSelection implements Selection {
    private final int capacity;

    private final @NotNull List<Unit> units;

    private int changeCount;

    /**
     * Creates an empty {@link OrderedSelection} instance.
     *
     * @param capacity the most units the selection holds
     * @throws IllegalArgumentException if the capacity is not positive
     */
    public OrderedSelection(int capacity) {
        if (capacity <= 0) {
            throw Exceptions.newIllegalArgumentException("Invalid selection capacity: {0}", capacity);
        }

        this.capacity = capacity;

        this.units = new ArrayList<>(capacity);

        this.changeCount = 0;
    }

    /**
     * @return the most units the selection holds
     */
    @Override
    public int getCapacity() {
        return this.capacity;
    }

    /**
     * @return the selected units in selection order, a read-only view
     */
    @Override
    public @NotNull List<Unit> getUnits() {
        return Collections.unmodifiableList(this.units);
    }

    /**
     * @return the number of selected units
     */
    @Override
    public int getSize() {
        return this.units.size();
    }

    /**
     * @return {@code true} if no unit is selected, {@code false} otherwise
     */
    @Override
    public boolean isEmpty() {
        return this.units.isEmpty();
    }

    /**
     * @return {@code true} if the selection holds {@link #getCapacity} units, {@code false} otherwise
     */
    @Override
    public boolean isFull() {
        return this.units.size() >= this.capacity;
    }

    /**
     * Returns how often the selection changed, for a part that notices a change by comparing it with an earlier count; a call that left the
     * units as they were counts as well.
     *
     * @return the number of changing calls since the selection was created
     */
    public int getChangeCount() {
        return this.changeCount;
    }


    /**
     * Tells whether a unit is selected.
     *
     * @param unit the unit
     * @return {@code true} if the unit is selected, {@code false} otherwise
     */
    @Override
    public boolean contains(@NotNull Unit unit) {
        return this.units.contains(unit);
    }

    /**
     * Selects one unit alone.
     *
     * @param unit the unit
     */
    public void select(@NotNull Unit unit) {
        this.units.clear();
        this.units.add(unit);
        this.changeCount++;
    }

    /**
     * Selects the supplied units alone, in their order, up to the capacity; a unit listed twice is selected once.
     *
     * @param selected the units
     */
    public void select(@NotNull Collection<Unit> selected) {
        this.units.clear();
        this.addAll(selected);
        this.changeCount++;
    }

    /**
     * Adds a unit after the selected ones.
     *
     * @param unit the unit
     * @return {@code true} if the unit was added, {@code false} otherwise (it is already selected or the selection is full)
     */
    public boolean add(@NotNull Unit unit) {
        if (this.isFull() || this.units.contains(unit)) {
            return false;
        }

        this.units.add(unit);
        this.changeCount++;

        return true;
    }

    /**
     * Adds units after the selected ones, in their order, up to the capacity.
     *
     * @param added the units
     */
    public void addAll(@NotNull Collection<Unit> added) {
        for (Unit unit : added) {
            this.add(unit);
        }
    }

    /**
     * Removes a unit.
     *
     * @param unit the unit
     * @return {@code true} if the unit was selected, {@code false} otherwise
     */
    public boolean remove(@NotNull Unit unit) {
        boolean removed = this.units.remove(unit);

        if (removed) {
            this.changeCount++;
        }

        return removed;
    }

    /**
     * Adds a unit that is not selected, or removes one that is: the shift-click of most RTS clients.
     *
     * @param unit the unit
     * @return {@code true} if the unit is selected afterwards, {@code false} otherwise
     */
    public boolean toggle(@NotNull Unit unit) {
        if (this.remove(unit)) {
            return false;
        }

        return this.add(unit);
    }

    /**
     * Keeps only the units that pass a test, for example the ones still alive and owned by the player.
     *
     * @param test the test
     * @return {@code true} if a unit was removed, {@code false} otherwise
     */
    public boolean retain(@NotNull Predicate<Unit> test) {
        boolean removed = this.units.removeIf(test.negate());

        if (removed) {
            this.changeCount++;
        }

        return removed;
    }

    /**
     * Removes every unit.
     */
    public void clear() {
        this.units.clear();
        this.changeCount++;
    }
}
