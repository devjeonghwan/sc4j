package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.demo.order.DemoOrders;
import org.fogofwarlabs.sc4j.engine.client.command.CommandAction;
import org.fogofwarlabs.sc4j.engine.client.command.CommandTarget;
import org.fogofwarlabs.sc4j.engine.client.command.CommandTranslator;
import org.fogofwarlabs.sc4j.engine.client.command.RallyAction;
import org.fogofwarlabs.sc4j.engine.combat.CombatSystem;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommandType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Orders the selected units by what a right press points at, the demo's rule of the default action: a worker on a gold mine gathers, a
 * unit on an enemy attacks it, and every other press moves there. One order command goes out per selected unit of the local player that is
 * not a building; the selected producers of the player set their rally point there ({@link RallyAction}).
 */
public final class DemoSmartAction implements CommandAction {
    private final @NotNull Game game;
    private final @NotNull DemoOrders orders;
    private final @NotNull RallyAction rally;

    /**
     * Creates a {@link DemoSmartAction} instance.
     *
     * @param game   the game the selected units belong to
     * @param orders the demo's orders and command stream
     * @param rally  the rally action of the command card, which sets the producers' rally points
     */
    public DemoSmartAction(@NotNull Game game, @NotNull DemoOrders orders, @NotNull RallyAction rally) {
        this.game   = game;
        this.orders = orders;
        this.rally  = rally;
    }

    /**
     * @return {@code true} if the action needs a target the player points at, {@code false} if it runs at once: the action needs its target
     */
    @Override
    public boolean isTargeted() {
        return true;
    }

    /**
     * Submits the action's commands. A targeted action is still the waiting action of the translator's
     * {@link org.fogofwarlabs.sc4j.engine.client.command.TargetingMode} while it is issued; it may refuse the target (for example a place a
     * building cannot stand on), and then it keeps waiting for another.
     * <p>
     * Submits one order command for each selected unit of the local player that is not a building, and one rally command for each selected
     * producer of the player.
     *
     * @param translator the translator: the local player, the selection and the command sink
     * @param target     the target the player pointed at, or {@code null} for an action without a target
     * @param queued     whether the commands queue after the units' current orders (Shift held)
     * @return {@code true} if the action took the target (or ran without one), {@code false} if it refused the target: here without a target
     */
    @Override
    public boolean issue(@NotNull CommandTranslator translator, @Nullable CommandTarget target, boolean queued) {
        if (target == null) {
            return false;
        }

        Player player = translator.getPlayer();
        IssueOrderCommandType command = this.orders.getOrderCommand();
        UnitTypeTable table = this.game.getContent().getUnitTypes();
        boolean building = false;

        for (Unit unit : translator.getSelection().getUnits()) {
            UnitType type = unit.getType();

            if (unit.getOwner() != player || type == null) {
                continue;
            }

            if (table.hasTrait(type, UnitTrait.BUILDING)) {
                building = true;

                continue;
            }

            Unit targetUnit = target.getUnit();
            OrderType order = this.chooseOrder(unit, type, targetUnit);

            translator.getSink().submit(player, command, command.create(this.game, unit, order, target.getX(), target.getY(),
                                                                        order == this.orders.getMove() ? null : targetUnit, queued));
        }

        if (building) {
            this.rally.issue(translator, target, queued);
        }

        return true;
    }

    private @NotNull OrderType chooseOrder(@NotNull Unit unit, @NotNull UnitType type, @Nullable Unit targetUnit) {
        UnitType targetType = targetUnit != null ? targetUnit.getType() : null;

        if (targetUnit == null || targetType == null) {
            return this.orders.getMove();
        }

        UnitTypeTable table = this.game.getContent().getUnitTypes();

        if (table.hasTrait(type, UnitTrait.WORKER) && table.hasTrait(targetType, UnitTrait.RESOURCE_CONTAINER)) {
            return this.orders.getGather();
        }

        if (this.game.hasSystem(CombatSystem.KEY) && this.game.getCombat().isEnemy(unit, targetUnit) &&
            this.game.getCombat().canAttack(unit, targetUnit)) {
            return this.orders.getAttack();
        }

        return this.orders.getMove();
    }
}
