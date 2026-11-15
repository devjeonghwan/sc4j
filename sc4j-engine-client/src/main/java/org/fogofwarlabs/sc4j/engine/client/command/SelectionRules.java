package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Decides which units a selection takes: the hooks of the {@link SelectionFilter}. The engine default is {@link StandardSelectionRules}; a
 * game with its own selection rules (which units may be selected together, which unit wins a slot of a full selection) supplies its own.
 *
 * @param <U> the unit class of the game
 */
public interface SelectionRules<U extends Unit> {
    /**
     * Finds the unit that a unit stands for in a selection, for example the base of a turret.
     *
     * @param unit the unit
     * @return the unit selected for it, the unit itself if it stands for itself
     */
    @NotNull U getMainUnit(@NotNull U unit);

    /**
     * Tells whether the local player may select a unit at all: the player sees it and its kind can be selected.
     *
     * @param unit the unit
     * @return {@code true} if the unit may be selected, {@code false} otherwise
     */
    boolean isListable(@NotNull U unit);

    /**
     * Tells whether a unit may be one of several selected units, for example the local player's own mobile units.
     *
     * @param unit the unit
     * @return {@code true} if the unit joins a multiple selection, {@code false} otherwise
     */
    boolean canJoinMultipleSelection(@NotNull U unit);

    /**
     * Reads the replacement key of a unit: once a selection is full, a candidate replaces the first unit whose key is lower, both compared
     * unsigned.
     *
     * @param unit the unit
     * @return the unsigned key
     */
    int getReplacementKey(@NotNull U unit);

    /**
     * Tells whether a unit is of the same kind as a clicked unit, for the selection of every unit of a kind.
     *
     * @param unit    the unit
     * @param clicked the clicked unit
     * @return {@code true} if the unit is of the clicked unit's kind, {@code false} otherwise
     */
    boolean isSameKind(@NotNull U unit, @NotNull U clicked);
}
