package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Holds the numbered control groups of a local player: each group remembers units of a {@link Selection} to recall later. A group number is a
 * plain value, {@code 0} to {@link #getCount} minus one (the digit keys of most clients).
 */
public final class ControlGroups {
    private final @NotNull List<List<Unit>> groups;

    /**
     * Creates a {@link ControlGroups} instance of empty groups.
     *
     * @param count the number of groups
     * @throws IllegalArgumentException if the count is not positive
     */
    public ControlGroups(int count) {
        if (count <= 0) {
            throw Exceptions.newIllegalArgumentException("Invalid control group count: {0}", count);
        }

        this.groups = new ArrayList<>(count);

        for (int group = 0; group < count; group++) {
            this.groups.add(new ArrayList<>());
        }
    }

    /**
     * @return the number of groups
     */
    public int getCount() {
        return this.groups.size();
    }

    /**
     * Returns the units of a group.
     *
     * @param group the group number
     * @return the units in their order, a read-only view
     * @throws IndexOutOfBoundsException if the group number is outside the groups
     */
    public @NotNull List<Unit> getUnits(int group) {
        return Collections.unmodifiableList(this.groups.get(group));
    }

    /**
     * Makes a group hold the selected units.
     *
     * @param group     the group number
     * @param selection the selection
     * @throws IndexOutOfBoundsException if the group number is outside the groups
     */
    public void assign(int group, @NotNull Selection selection) {
        List<Unit> units = this.groups.get(group);

        units.clear();
        units.addAll(selection.getUnits());
    }

    /**
     * Adds the selected units a group does not hold yet, after its units.
     *
     * @param group     the group number
     * @param selection the selection
     * @throws IndexOutOfBoundsException if the group number is outside the groups
     */
    public void add(int group, @NotNull Selection selection) {
        List<Unit> units = this.groups.get(group);

        for (Unit unit : selection.getUnits()) {
            if (!units.contains(unit)) {
                units.add(unit);
            }
        }
    }

    /**
     * Selects the units of a group alone, up to the selection's capacity.
     *
     * @param group     the group number
     * @param selection the selection
     * @return {@code true} if the group holds a unit, {@code false} otherwise (the group is empty and the selection stays unchanged)
     * @throws IndexOutOfBoundsException if the group number is outside the groups
     */
    public boolean recall(int group, @NotNull OrderedSelection selection) {
        List<Unit> units = this.groups.get(group);

        if (units.isEmpty()) {
            return false;
        }

        selection.select(units);

        return true;
    }

    /**
     * Keeps only the units that pass a test in every group, for example the ones still alive.
     *
     * @param test the test
     */
    public void retain(@NotNull Predicate<Unit> test) {
        for (List<Unit> units : this.groups) {
            units.removeIf(test.negate());
        }
    }
}
