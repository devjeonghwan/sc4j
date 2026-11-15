package org.fogofwarlabs.sc4j.engine.economy;

import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.order.OrderBehavior;
import org.fogofwarlabs.sc4j.engine.order.OrderContext;
import org.jetbrains.annotations.NotNull;

/**
 * Brings what a worker carries to the nearest drop-off with the standard harvest system and sends the worker back to its container: the engine
 * default behavior of a worker's return order. A mod registers it for its return order type with the gather order ({@link GatherBehavior}).
 * <p>
 * When the order starts, the nearest drop-off ({@link StandardHarvestSystem#findDropOff}) becomes the order's target unit. Each update: a worker
 * that carries nothing goes back to gathering; a worker whose drop-off is gone looks for the nearest one again and ends the order without one; a
 * worker out of reach walks to the drop-off; a worker in reach delivers ({@link StandardHarvestSystem#deliver}) and gets the gather order on its
 * container. Going back to gathering: a worker whose container still holds resources gets the gather order on it
 * ({@link StandardHarvestSystem#getGatherTarget}); any other gets the gather order at its own position without a container, so that it looks
 * for the nearest container from there.
 * <p>
 * Determinism contract: the behavior draws no random value and allocates nothing.
 */
public final class ReturnResourcesBehavior implements OrderBehavior<OrderContext, Unit> {
    private final @NotNull StandardHarvestSystem harvest;
    private final @NotNull OrderType gatherOrder;

    /**
     * Creates a {@link ReturnResourcesBehavior} instance.
     *
     * @param harvest     the harvest system of the game
     * @param gatherOrder the order type a worker gets to go back to its container
     */
    public ReturnResourcesBehavior(@NotNull StandardHarvestSystem harvest, @NotNull OrderType gatherOrder) {
        this.harvest     = harvest;
        this.gatherOrder = gatherOrder;
    }

    /**
     * Starts the order of a unit when it becomes the unit's current order.
     * <p>
     * The nearest drop-off of the worker's owner becomes the order's target unit.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order starts
     */
    @Override
    public void start(@NotNull OrderContext context, @NotNull Unit unit) {
        unit.setOrderTargetUnit(this.harvest.findDropOff(unit));
    }

    /**
     * Runs the order of one unit for one frame.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order runs
     */
    @Override
    public void update(@NotNull OrderContext context, @NotNull Unit unit) {
        Game game = context.getGame();

        if (this.harvest.getCarriedAmount(unit) == 0) {
            this.returnToGathering(context, unit);

            return;
        }

        Unit dropOff = unit.getOrderTargetUnit();

        if (dropOff == null || dropOff.getOwner() != unit.getOwner()) {
            dropOff = this.harvest.findDropOff(unit);

            unit.setOrderTargetUnit(dropOff);

            if (dropOff == null) {
                game.getOrders().finishOrder(unit, context.getInstructionLimit());

                return;
            }
        }

        MovementSystem movement = game.getMovement();

        if (!this.harvest.isInReach(unit, dropOff)) {
            if (unit.getMovementDestinationX() != dropOff.getPositionX() || unit.getMovementDestinationY() != dropOff.getPositionY()) {
                movement.setDestination(unit, dropOff.getPositionX(), dropOff.getPositionY());
            } else if (movement.isAtDestination(unit)) {
                game.getOrders().finishOrder(unit, context.getInstructionLimit());
            }

            return;
        }

        movement.stop(unit);
        this.harvest.deliver(unit);
        this.returnToGathering(context, unit);
    }

    private void returnToGathering(@NotNull OrderContext context, @NotNull Unit unit) {
        Unit container = this.harvest.getGatherTarget(unit);
        int targetX = container != null ? container.getPositionX() : unit.getPositionX();
        int targetY = container != null ? container.getPositionY() : unit.getPositionY();

        context.getGame().getOrders().issue(unit, this.gatherOrder, targetX, targetY, container, context.getInstructionLimit());
    }
}
