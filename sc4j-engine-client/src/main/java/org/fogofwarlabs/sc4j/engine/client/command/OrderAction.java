package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommandType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Issues one order type to every selected unit through the engine order command ({@link IssueOrderCommandType}), one command per unit in
 * selection order. A targeted order goes to the pointed position and unit; an order without a target goes to each unit's own position. An
 * action may name a second order type for a pointed position without a unit, on the map or the minimap: an attack action orders attack-move
 * there, which moves to the point and fights the enemies on the way.
 */
public final class OrderAction implements CommandAction {
    private final @NotNull Game game;
    private final @NotNull IssueOrderCommandType commandType;
    private final @NotNull OrderType orderType;
    private final @Nullable OrderType pointOrderType;
    private final boolean targeted;

    /**
     * Creates an {@link OrderAction} instance with the same order type for every target.
     *
     * @param game        the game whose units the commands name
     * @param commandType the order command type of the game's command stream
     * @param orderType   the order type
     * @param targeted    whether the order needs a target the player points at
     */
    public OrderAction(@NotNull Game game, @NotNull IssueOrderCommandType commandType, @NotNull OrderType orderType, boolean targeted) {
        this(game, commandType, orderType, null, targeted);
    }

    /**
     * Creates an {@link OrderAction} instance.
     *
     * @param game           the game whose units the commands name
     * @param commandType    the order command type of the game's command stream
     * @param orderType      the order type
     * @param pointOrderType the order type of a pointed position without a unit, or {@code null} for the order type
     * @param targeted       whether the order needs a target the player points at
     */
    public OrderAction(@NotNull Game game, @NotNull IssueOrderCommandType commandType,
                       @NotNull OrderType orderType, @Nullable OrderType pointOrderType, boolean targeted) {
        this.game           = game;
        this.commandType    = commandType;
        this.orderType      = orderType;
        this.pointOrderType = pointOrderType;
        this.targeted       = targeted;
    }

    /**
     * @return the order type
     */
    public @NotNull OrderType getOrderType() {
        return this.orderType;
    }

    /**
     * @return the order type of a pointed position without a unit, or {@code null} if it is the order type
     */
    public @Nullable OrderType getPointOrderType() {
        return this.pointOrderType;
    }

    /**
     * @return {@code true} if the action needs a target the player points at, {@code false} if it runs at once
     */
    @Override
    public boolean isTargeted() {
        return this.targeted;
    }

    /**
     * Submits the action's commands: one order command for each selected unit, of the point order type if the action has one and the target
     * is a position without a unit.
     *
     * @param translator the translator: the local player, the selection and the command sink
     * @param target     the target the player pointed at, or {@code null} for an action without a target
     * @param queued     whether the commands queue after the units' current orders (Shift held)
     * @return {@code true}: an order takes every target
     */
    @Override
    public boolean issue(@NotNull CommandTranslator translator, @Nullable CommandTarget target, boolean queued) {
        OrderType issuedType = target != null && target.getUnit() == null && this.pointOrderType != null ? this.pointOrderType : this.orderType;

        for (Unit unit : translator.getSelection().getUnits()) {
            int targetX = target != null ? target.getX() : unit.getPositionX();
            int targetY = target != null ? target.getY() : unit.getPositionY();
            Unit targetUnit = target != null ? target.getUnit() : null;

            translator.getSink().submit(translator.getPlayer(), this.commandType,
                                        this.commandType.create(this.game, unit, issuedType, targetX, targetY, targetUnit, queued));
        }

        return true;
    }
}
