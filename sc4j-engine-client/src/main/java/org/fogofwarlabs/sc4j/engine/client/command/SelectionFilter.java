package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses the units of a selection from candidate units, by the hooks of its {@link SelectionRules}: the engine's selection filter for a box,
 * for every unit of a clicked unit's kind, and for adding units to a selection. Each filter keeps the candidates' order, takes each unit
 * once and at most {@link #getCapacity} units; once full, a later unit replaces the first taken unit with a lower replacement key. The filters
 * read the units and change nothing.
 *
 * @param <U> the unit class of the game
 */
public final class SelectionFilter<U extends Unit> {
    private final int capacity;
    private final @NotNull SelectionRules<U> rules;

    /**
     * Creates a {@link SelectionFilter} instance.
     *
     * @param capacity the most units a selection holds
     * @param rules    the selection rules
     * @throws IllegalArgumentException if the capacity is not positive
     */
    public SelectionFilter(int capacity, @NotNull SelectionRules<U> rules) {
        if (capacity <= 0) {
            throw Exceptions.newIllegalArgumentException("Invalid selection capacity {0}", capacity);
        }

        this.capacity = capacity;
        this.rules    = rules;
    }

    /**
     * @return the most units a selection holds
     */
    public int getCapacity() {
        return this.capacity;
    }


    /**
     * Chooses the units of a box. Each candidate stands for its main unit; the units that may be selected and join a multiple selection are
     * taken. When none of them does, the last unit that may be selected (for example a building or another player's unit) is the result alone.
     *
     * @param listed the units inside the box, in the game's search order
     * @return the chosen units, at most {@link #getCapacity}
     */
    public @NotNull List<U> filterBox(@NotNull List<U> listed) {
        List<U> output = new ArrayList<>();
        U fallback = null;

        for (U candidate : listed) {
            U unit = this.rules.getMainUnit(candidate);

            if (output.contains(unit) || !this.rules.isListable(unit)) {
                continue;
            }

            if (!this.rules.canJoinMultipleSelection(unit)) {
                fallback = unit;

                continue;
            }

            if (output.size() < this.capacity) {
                output.add(unit);

                continue;
            }

            this.replaceLower(output, 0, unit);
        }

        if (output.isEmpty() && fallback != null) {
            output.add(fallback);
        }

        return output;
    }

    /**
     * Chooses the units of a clicked unit's kind: the clicked unit first, kept even when it does not join a multiple selection (a building
     * selects only itself), then the candidates of its kind that may be selected and join a multiple selection. The clicked unit is never
     * replaced.
     *
     * @param listed  the candidate units, for example those on the screen, in the game's search order
     * @param clicked the clicked unit
     * @return the chosen units, at most {@link #getCapacity}
     */
    public @NotNull List<U> filterType(@NotNull List<U> listed, @NotNull U clicked) {
        List<U> output = new ArrayList<>();

        output.add(clicked);

        for (U candidate : listed) {
            U unit = this.rules.getMainUnit(candidate);

            if (output.contains(unit) || !this.rules.isListable(unit) || !this.rules.canJoinMultipleSelection(unit) ||
                !this.rules.isSameKind(unit, clicked)) {
                continue;
            }

            if (output.size() < this.capacity) {
                output.add(unit);

                continue;
            }

            this.replaceLower(output, 1, unit);
        }

        return output;
    }

    /**
     * Adds units to a selection: the new units join when the first selected unit and the first new unit both join a multiple selection; once
     * the selection is full a new unit replaces only an earlier added unit with a lower key.
     *
     * @param selection the current selection in order
     * @param added     the newly chosen units
     * @return the merged selection; the current selection itself when nothing can be added
     */
    public @NotNull List<U> merge(@NotNull List<U> selection, @NotNull List<U> added) {
        if (selection.size() >= this.capacity || added.isEmpty() || selection.isEmpty()) {
            return selection;
        }

        if (!this.rules.canJoinMultipleSelection(added.getFirst()) || !this.rules.canJoinMultipleSelection(selection.getFirst())) {
            return selection;
        }

        List<U> merged = new ArrayList<>(selection);
        int firstAdded = merged.size();

        for (U unit : added) {
            if (merged.contains(unit)) {
                continue;
            }

            if (merged.size() < this.capacity) {
                merged.add(unit);

                continue;
            }

            this.replaceLower(merged, firstAdded, unit);
        }

        return merged;
    }

    /**
     * Replaces the first unit from a slot on whose key is lower, compared unsigned, than the candidate's.
     */
    private void replaceLower(@NotNull List<U> units, int firstSlot, @NotNull U candidate) {
        int key = this.rules.getReplacementKey(candidate);

        for (int slot = firstSlot; slot < units.size(); slot++) {
            if (Integer.compareUnsigned(this.rules.getReplacementKey(units.get(slot)), key) < 0) {
                units.set(slot, candidate);

                return;
            }
        }
    }
}
