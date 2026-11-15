package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Selects by the rules of the engine default client: the default {@link SelectionRules}. Every unit stands for itself; a placed unit with an
 * owner that the local player sees may be selected; the local player's own units join a multiple selection (any unit for an observer without
 * local player); units are of one kind when they have the same owner and type. Every key is {@code 0}, so a full selection keeps its units.
 */
public final class StandardSelectionRules implements SelectionRules<Unit> {
    private final @NotNull GameView view;

    /**
     * Creates a {@link StandardSelectionRules} instance.
     *
     * @param view the game view, whose local player selects
     */
    public StandardSelectionRules(@NotNull GameView view) {
        this.view = view;
    }

    /**
     * Finds the unit that a unit stands for in a selection: the unit itself.
     *
     * @param unit the unit
     * @return the unit
     */
    @Override
    public @NotNull Unit getMainUnit(@NotNull Unit unit) {
        return unit;
    }

    /**
     * Tells whether the local player may select a unit at all: the unit is placed, has an owner and the local player sees it.
     *
     * @param unit the unit
     * @return {@code true} if the unit may be selected, {@code false} otherwise
     */
    @Override
    public boolean isListable(@NotNull Unit unit) {
        UnitView unitView = this.view.getUnitView(unit);
        Player player = this.view.getLocalPlayer();

        return unitView.isPlaced() && unitView.getOwner() != null && (player == null || this.view.isUnitVisible(player, unit));
    }

    /**
     * Tells whether a unit may be one of several selected units: the local player owns it, or there is no local player.
     *
     * @param unit the unit
     * @return {@code true} if the unit joins a multiple selection, {@code false} otherwise
     */
    @Override
    public boolean canJoinMultipleSelection(@NotNull Unit unit) {
        Player player = this.view.getLocalPlayer();

        return player == null || this.view.getUnitView(unit).getOwner() == player;
    }

    /**
     * Reads the replacement key of a unit: {@code 0} for every unit.
     *
     * @param unit the unit
     * @return {@code 0}
     */
    @Override
    public int getReplacementKey(@NotNull Unit unit) {
        return 0;
    }

    /**
     * Tells whether a unit is of the same kind as a clicked unit: the same owner and the same type.
     *
     * @param unit    the unit
     * @param clicked the clicked unit
     * @return {@code true} if both have the same owner and type, {@code false} otherwise
     */
    @Override
    public boolean isSameKind(@NotNull Unit unit, @NotNull Unit clicked) {
        UnitView unitView = this.view.getUnitView(unit);
        UnitView clickedView = this.view.getUnitView(clicked);

        return unitView.getOwner() == clickedView.getOwner() && unitView.getType() == clickedView.getType();
    }
}
