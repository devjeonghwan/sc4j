package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.command.*;
import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementAction;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementResult;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementSystem;
import org.fogofwarlabs.sc4j.engine.content.requirement.StandardRequirementResult;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommandType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Creates the command card of a selection from the game's order types and its requirement graph: the engine default
 * {@link CommandCardSource}. The buttons, in this order, up to the card's cells:
 * <ul>
 *     <li>the mod's {@link OrderButton}s that a selected unit of the local player shows (units that are not buildings, or units of the
 *     button's trait), each an {@link OrderAction} for every selected unit;</li>
 *     <li>for the first selected unit of the local player, every unit type of its faction it may make (every type of its faction without a
 *     requirement system), in content order, also a type whose requirement still misses a unit, an upgrade or a tech (its button shows
 *     refused, {@link CommandAction#findRefusal}), but no type of another actor or one not available to the player: a building
 *     type for a worker as a {@link ConstructAction} (with the footprint of the {@link StandardProductionSystem}), any other type for a
 *     building as a {@link TrainAction};</li>
 *     <li>a rally button ({@link #getRallyAction}) when the mod has a rally command and that unit is a producer
 *     ({@link RallyAction#isProducer});</li>
 *     <li>a cancel button ({@link #getCancelAction}) when that unit has something to cancel, in the last cell of a full card: the last queue
 *     entry, or its construction; a building under construction shows only the cancel button.</li>
 * </ul>
 * An order button keeps its own hotkey; a unit type's button takes the first letter of its name that no earlier button uses; the cancel button
 * has none.
 */
public final class StandardCommandCardSource implements CommandCardSource {
    private static final @NotNull String CANCEL_LABEL = "Cancel";
    private static final @NotNull String RALLY_LABEL = "Rally";

    private final @NotNull Game game;
    private final @NotNull TrainUnitCommandType training;
    private final @NotNull ConstructBuildingCommandType construction;
    private final @NotNull CancelAction cancelAction;
    private final @Nullable RallyAction rallyAction;

    private final @NotNull List<@NotNull OrderButton> orderButtons;
    private final @NotNull List<@NotNull OrderAction> orderActions;
    private final @NotNull Function<@NotNull UnitType, @NotNull String> typeNames;
    private final int capacity;

    /**
     * Creates a {@link StandardCommandCardSource} instance.
     *
     * @param game         the game
     * @param orders       the order command of the mod's command stream
     * @param training     the training command of the mod's command stream
     * @param construction the construction command of the mod's command stream
     * @param cancellation the cancel command of the mod's command stream
     * @param rallying     the rally command of the mod's command stream, or {@code null} for no rally button
     * @param orderButtons the order buttons in their card order, copied
     * @param typeNames    the shown name of each unit type
     * @param capacity     the cells of the command card
     */
    public StandardCommandCardSource(@NotNull Game game,
                                     @NotNull IssueOrderCommandType orders,
                                     @NotNull TrainUnitCommandType training,
                                     @NotNull ConstructBuildingCommandType construction,
                                     @NotNull CancelProductionCommandType cancellation,
                                     @Nullable SetRallyPointCommandType rallying,
                                     @NotNull List<@NotNull OrderButton> orderButtons,
                                     @NotNull Function<@NotNull UnitType, @NotNull String> typeNames,
                                     int capacity) {
        this.game         = game;
        this.training     = training;
        this.construction = construction;
        this.cancelAction = new CancelAction(game, cancellation);
        this.rallyAction  = rallying != null ? new RallyAction(game, rallying) : null;

        this.orderButtons = List.copyOf(orderButtons);
        this.orderActions = new ArrayList<>(orderButtons.size());
        this.typeNames    = typeNames;
        this.capacity     = capacity;

        for (OrderButton button : orderButtons) {
            this.orderActions.add(new OrderAction(game, orders, button.getOrderType(), button.getPointOrderType(), button.isTargeted()));
        }
    }

    /**
     * @return the action of the cancel button, which the queue panel cancels its slots with
     */
    public @NotNull CancelAction getCancelAction() {
        return this.cancelAction;
    }

    /**
     * @return the action of the rally button, which a mod's right press on the map may share, or {@code null} without a rally command
     */
    public @Nullable RallyAction getRallyAction() {
        return this.rallyAction;
    }


    /**
     * Creates the buttons for a selection.
     *
     * @param player    the local player
     * @param selection the selected units in selection order, not empty
     * @return the buttons in grid order
     */
    @Override
    public @NotNull List<@NotNull CommandButton> createButtons(@NotNull Player player, @NotNull List<@NotNull Unit> selection) {
        List<CommandButton> buttons = new ArrayList<>();
        Set<Key> usedKeys = EnumSet.noneOf(Key.class);
        Unit first = null;

        for (Unit unit : selection) {
            if (unit.getOwner() == player && unit.getType() != null) {
                first = unit;

                break;
            }
        }

        if (first == null) {
            return buttons;
        }

        if (this.game.hasSystem(ProductionSystem.KEY) && this.game.getProduction().isUnderConstruction(first)) {
            this.addButton(buttons, usedKeys, CANCEL_LABEL, null, this.cancelAction);

            return buttons;
        }

        for (int index = 0; index < this.orderButtons.size(); index++) {
            OrderButton button = this.orderButtons.get(index);

            if (this.isShown(button, player, selection)) {
                this.addButton(buttons, usedKeys, button.getLabel(), button.getHotkey(), this.orderActions.get(index));
            }
        }

        this.addProductionButtons(buttons, usedKeys, player, first);

        RallyAction rally = this.rallyAction;

        if (rally != null && rally.isProducer(first)) {
            this.addButton(buttons, usedKeys, RALLY_LABEL, this.findHotkey(RALLY_LABEL, usedKeys), rally);
        }

        if (this.cancelAction.canCancel(player, first)) {
            // The cancel button takes the last cell of a full card.
            if (buttons.size() >= this.capacity) {
                buttons.removeLast();
            }

            this.addButton(buttons, usedKeys, CANCEL_LABEL, null, this.cancelAction);
        }

        return buttons;
    }

    private boolean isShown(@NotNull OrderButton button, @NotNull Player player, @NotNull List<@NotNull Unit> selection) {
        UnitTypeTable table = this.game.getContent().getUnitTypes();
        UnitTrait trait = button.getRequiredTrait();

        for (Unit unit : selection) {
            UnitType type = unit.getType();

            if (unit.getOwner() == player && type != null &&
                (trait != null ? table.hasTrait(type, trait) : !table.hasTrait(type, UnitTrait.BUILDING))) {
                return true;
            }
        }

        return false;
    }

    private void addProductionButtons(@NotNull List<@NotNull CommandButton> buttons, @NotNull Set<@NotNull Key> usedKeys,
                                      @NotNull Player player, @NotNull Unit unit) {
        UnitTypeTable table = this.game.getContent().getUnitTypes();
        UnitType unitType = unit.getType();

        if (unitType == null) {
            return;
        }

        boolean worker = table.hasTrait(unitType, UnitTrait.WORKER);
        boolean building = table.hasTrait(unitType, UnitTrait.BUILDING);
        StandardProductionSystem production = this.game.getProduction() instanceof StandardProductionSystem standard ? standard : null;
        Faction faction = table.getFaction(unitType);

        for (UnitType type : this.game.getContent().getDefinition().getUnitTypes().getTypes().getTypes()) {
            if (faction == null || table.getFaction(type) != faction || !this.isOffered(type, player, unit)) {
                continue;
            }

            String name = this.typeNames.apply(type);

            if (table.hasTrait(type, UnitTrait.BUILDING)) {
                if (worker && production != null) {
                    this.addButton(buttons, usedKeys, name, this.findHotkey(name, usedKeys),
                                   new ConstructAction(this.game, this.construction, type, production.getFootprintWidth(type),
                                                       production.getFootprintHeight(type)));
                }
            } else if (building) {
                this.addButton(buttons, usedKeys, name, this.findHotkey(name, usedKeys), new TrainAction(this.game, this.training, type));
            }
        }
    }

    /**
     * Checks whether a unit type has a button for an actor: the requirement is met, or it fails for something the player may still get (not
     * for another actor and not for a type unavailable to the player).
     */
    private boolean isOffered(@NotNull UnitType type, @NotNull Player player, @NotNull Unit actor) {
        if (!this.game.hasSystem(RequirementSystem.KEY)) {
            return true;
        }

        RequirementResult result = this.game.getRequirements().check(RequirementAction.BUILD_UNIT, type, player, actor);

        return result.isMet() || result != StandardRequirementResult.WRONG_ACTOR && result != StandardRequirementResult.UNAVAILABLE;
    }

    private void addButton(@NotNull List<@NotNull CommandButton> buttons, @NotNull Set<@NotNull Key> usedKeys,
                           @NotNull String label, @Nullable Key hotkey, @NotNull CommandAction action) {
        if (buttons.size() >= this.capacity) {
            return;
        }

        if (hotkey != null) {
            usedKeys.add(hotkey);
        }

        buttons.add(new CommandButton(label, hotkey, action));
    }

    private @Nullable Key findHotkey(@NotNull String name, @NotNull Set<@NotNull Key> usedKeys) {
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);

            if (character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z') {
                Key key = Key.valueOf(String.valueOf(character).toUpperCase(Locale.ROOT));

                if (!usedKeys.contains(key)) {
                    return key;
                }
            }
        }

        return null;
    }
}
