package org.fogofwarlabs.sc4j.engine.lifecycle;

import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.table.PlayerProgressTable;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.economy.EconomySystem;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.order.OrderSystem;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.spatial.SpatialIndex;
import org.jetbrains.annotations.NotNull;

/**
 * Removes units from the game and gives them to other players with the bookkeeping of the engine defaults: the owner's unit counts in the player
 * progress table and the supply of the unit's faction in the economy system, the spatial index, the orders and the movement. The engine combat
 * default removes its dead units here, the trigger and harvest defaults the units they remove or give away.
 * <p>
 * A unit counts towards its owner's unit count from its creation and towards the completed unit count and the supply it provides once it is
 * complete; a unit the production system has under construction ({@link ProductionSystem#isUnderConstruction}) is not complete. The supply a
 * unit requires counts from its creation.
 * <p>
 * Determinism contract: the operations draw no random value, allocate nothing and call the lifecycle hooks in registration order.
 */
public final class UnitAccountingUtils {
    /**
     * Removes a unit from the game: the unit lifecycle announces a death ({@link UnitLifecycle#notifyDying}) for a dying unit and the removal
     * ({@link UnitLifecycle#notifyRemoved}); the production system clears the unit's production ({@link ProductionSystem#clearProduction}); the
     * owner loses the unit's counts and supply, the unit leaves the spatial index, its orders ({@link OrderSystem#removeOrders}) and its movement
     * ({@link MovementSystem#stop}), loses its owner and its pool slot is freed. The systems the game does not have are skipped.
     *
     * @param game  the game
     * @param unit  the unit, allocated in the game's unit pool
     * @param death {@code true} if the unit dies, {@code false} if it is taken out of the game
     * @throws IllegalStateException if the unit's slot is already free
     */
    public static void removeUnit(@NotNull Game game, @NotNull Unit unit, boolean death) {
        UnitLifecycle lifecycle = game.getUnitLifecycle();
        Player owner = unit.getOwner();

        if (death) {
            lifecycle.notifyDying(unit);
        }

        lifecycle.notifyRemoved(unit, death);

        if (game.hasSystem(ProductionSystem.KEY)) {
            game.getProduction().clearProduction(unit);
        }

        if (owner != null) {
            changeCounts(game, unit, owner, -1);
        }

        if (game.hasSystem(SpatialIndex.KEY)) {
            game.getSpatialIndex().remove(unit);
        }

        if (game.hasSystem(OrderSystem.KEY)) {
            game.getOrders().removeOrders(unit);
        }

        if (game.hasSystem(MovementSystem.KEY)) {
            game.getMovement().stop(unit);
        }

        unit.setOwner(null);
        game.getUnits().free(unit);
    }

    /**
     * Gives a unit to another player: the counts and supply move from the old owner to the new one, the unit gets the new owner, its orders are
     * replaced by its idle order for the new owner ({@link OrderSystem#removeOrders}, {@link OrderSystem#finishOrder}), it stops, and the unit
     * lifecycle announces the change ({@link UnitLifecycle#notifyOwnerChanged}). Giving a unit to its owner does nothing.
     *
     * @param game             the game
     * @param unit             the unit
     * @param newOwner         the new owner
     * @param instructionLimit the instruction limit of each animation program run the change starts
     */
    public static void transferUnit(@NotNull Game game, @NotNull Unit unit, @NotNull Player newOwner, int instructionLimit) {
        Player owner = unit.getOwner();

        if (owner == newOwner) {
            return;
        }

        if (owner != null) {
            changeCounts(game, unit, owner, -1);
        }

        unit.setOwner(newOwner);
        changeCounts(game, unit, newOwner, 1);

        if (game.hasSystem(MovementSystem.KEY)) {
            game.getMovement().stop(unit);
        }

        if (game.hasSystem(OrderSystem.KEY)) {
            game.getOrders().removeOrders(unit);
            game.getOrders().finishOrder(unit, instructionLimit);
        }

        game.getUnitLifecycle().notifyOwnerChanged(unit, instructionLimit);
    }

    /**
     * Adds a unit's counts and supply to a player, or takes them away with a negative sign.
     */
    private static void changeCounts(@NotNull Game game, @NotNull Unit unit, @NotNull Player player, int sign) {
        UnitType type = unit.getType();

        if (type == null) {
            return;
        }

        UnitTypeTable unitTypes = game.getContent().getUnitTypes();
        PlayerProgressTable progress = game.getContent().getProgress();
        boolean completed = !game.hasSystem(ProductionSystem.KEY) || !game.getProduction().isUnderConstruction(unit);
        Faction faction = unitTypes.getFaction(type);

        progress.setUnitCount(player, type, progress.getUnitCount(player, type) + sign);

        if (completed) {
            progress.setCompletedUnitCount(player, type, progress.getCompletedUnitCount(player, type) + sign);
        }

        if (faction != null && game.hasSystem(EconomySystem.KEY)) {
            EconomySystem economy = game.getEconomy();

            economy.addSupplyUsed(player, faction, sign * unitTypes.getSupplyRequired(type));

            if (completed) {
                economy.addSupplyProvided(player, faction, sign * unitTypes.getSupplyProvided(type));
            }
        }
    }

    private UnitAccountingUtils() {
    }
}
