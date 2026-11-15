package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.requirement.MissingUnitResult;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementAction;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementResult;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementSystem;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.economy.EconomySystem;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Checks whether a player may make a unit type with an acting unit, the shared check of the actions that train and build ({@link TrainAction},
 * {@link ConstructAction}): the requirement system's build check with the actor, then each resource of the type's cost, then, when the
 * production takes it now, the supply of the type's faction. It holds the type's cost from the unit type table and one refusal per resource kind
 * of the cost, so a check allocates nothing unless a requirement names a unit type the player lacks.
 */
public final class ProductionCheck {
    private final @NotNull Game game;
    private final @NotNull UnitType unitType;
    private final @NotNull ResourceCost cost;
    private final int supplyCost;
    private final @NotNull CommandRefusal @NotNull [] resourceRefusals;

    private @Nullable MissingUnitResult lastMissing;
    private @Nullable CommandRefusal lastMissingRefusal;

    /**
     * Creates a {@link ProductionCheck} instance.
     *
     * @param game     the game
     * @param unitType the unit type to make
     */
    public ProductionCheck(@NotNull Game game, @NotNull UnitType unitType) {
        UnitTypeTable unitTypes = game.getContent().getUnitTypes();
        List<ResourceKind> kinds = game.getContent().getDefinition().getResourceKinds().getTypes();
        ResourceCost.Builder builder = new ResourceCost.Builder();

        for (ResourceKind kind : kinds) {
            int amount = unitTypes.getCost(unitType, kind);

            if (amount > 0) {
                builder.add(kind, amount);
            }
        }

        this.game             = game;
        this.unitType         = unitType;
        this.cost             = builder.build();
        this.supplyCost       = unitTypes.getSupplyRequired(unitType);
        this.resourceRefusals = new CommandRefusal[this.cost.getCount()];

        for (int index = 0; index < this.cost.getCount(); index++) {
            this.resourceRefusals[index] = CommandRefusal.ofMissingResource(this.cost.getKind(index));
        }

        this.lastMissing        = null;
        this.lastMissingRefusal = null;
    }

    /**
     * @return the unit type to make
     */
    public @NotNull UnitType getUnitType() {
        return this.unitType;
    }

    /**
     * @return the resources making the unit type costs, the resource kinds it costs nothing of left out
     */
    public @NotNull ResourceCost getCost() {
        return this.cost;
    }

    /**
     * @return the supply the unit type takes
     */
    public int getSupplyCost() {
        return this.supplyCost;
    }


    /**
     * Checks the requirement of making the unit type with an acting unit; a game without requirement system allows it.
     *
     * @param player the player
     * @param actor  the acting unit
     * @return {@code true} if the requirement is met, {@code false} otherwise
     */
    public boolean isAllowed(@NotNull Player player, @NotNull Unit actor) {
        return !this.game.hasSystem(RequirementSystem.KEY) ||
               this.game.getRequirements().check(RequirementAction.BUILD_UNIT, this.unitType, player, actor).isMet();
    }

    /**
     * Finds why the player may not make the unit type with an acting unit now: the requirement, then each resource of the cost, then the
     * supply when it is checked.
     *
     * @param player      the player
     * @param actor       the acting unit
     * @param supplyTaken whether making the unit takes its supply now, so missing supply refuses it
     * @return the refusal, or {@code null} if the player may make it
     */
    public @Nullable CommandRefusal findRefusal(@NotNull Player player, @NotNull Unit actor, boolean supplyTaken) {
        if (this.game.hasSystem(RequirementSystem.KEY)) {
            RequirementResult result = this.game.getRequirements().check(RequirementAction.BUILD_UNIT, this.unitType, player, actor);

            if (!result.isMet()) {
                return this.findRequirementRefusal(result);
            }
        }

        if (!this.game.hasSystem(EconomySystem.KEY)) {
            return null;
        }

        EconomySystem economy = this.game.getEconomy();

        for (int index = 0; index < this.cost.getCount(); index++) {
            if (economy.getAmount(player, this.cost.getKind(index)) < this.cost.getAmount(index)) {
                return this.resourceRefusals[index];
            }
        }

        Faction faction = this.game.getContent().getUnitTypes().getFaction(this.unitType);

        if (supplyTaken && this.supplyCost > 0 && faction != null &&
            economy.getSupplyUsed(player, faction) + this.supplyCost > economy.getSupplyProvided(player, faction)) {
            return CommandRefusal.MISSING_SUPPLY;
        }

        return null;
    }

    private @NotNull CommandRefusal findRequirementRefusal(@NotNull RequirementResult result) {
        if (!(result instanceof MissingUnitResult missing)) {
            return CommandRefusal.NOT_AVAILABLE;
        }

        CommandRefusal refusal = this.lastMissingRefusal;

        if (missing != this.lastMissing || refusal == null) {
            refusal                 = CommandRefusal.ofMissingUnit(missing.getUnitType());
            this.lastMissing        = missing;
            this.lastMissingRefusal = refusal;
        }

        return refusal;
    }
}
