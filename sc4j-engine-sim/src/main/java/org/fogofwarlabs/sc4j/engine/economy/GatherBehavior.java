package org.fogofwarlabs.sc4j.engine.economy;

import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.order.OrderBehavior;
import org.fogofwarlabs.sc4j.engine.order.OrderContext;
import org.fogofwarlabs.sc4j.engine.path.PathfindingSystem;
import org.jetbrains.annotations.NotNull;

/**
 * Gathers resources from the target container of the current order with the standard harvest system: the engine default behavior of a worker's
 * gather order. A mod registers it for its gather order type with the return order and the range a worker looks for another container in.
 * {@snippet :
 * StandardHarvestSystem harvest = (StandardHarvestSystem) game.getModSystem(HarvestSystem.KEY);
 * behaviors.register(gather, new GatherBehavior(harvest, returnResources, 320));
 * behaviors.register(returnResources, new ReturnResourcesBehavior(harvest, gather));
 *}
 * <p>
 * When the order starts, its target unit becomes the worker's container ({@link StandardHarvestSystem#setGatherTarget}). Each update: a worker
 * that carries resources gets the return order; a worker whose container is gone or empty takes the nearest container of the same kind within
 * the search range of the order's target point, or ends the order without one; a worker out of reach walks to its container
 * ({@link MovementSystem#setDestination}) and ends the order when the container's position cannot be reached ({@link PathfindingSystem#isReachable},
 * with a path finding system) or it stands still out of reach; a worker in reach stops and
 * gathers ({@link StandardHarvestSystem#gather}), and gets the return order once its trip is complete.
 * <p>
 * Determinism contract: the behavior draws no random value and allocates nothing.
 */
public final class GatherBehavior implements OrderBehavior<OrderContext, Unit> {
    private final @NotNull StandardHarvestSystem harvest;
    private final @NotNull OrderType returnOrder;
    private final int searchRange;

    /**
     * Creates a {@link GatherBehavior} instance.
     *
     * @param harvest     the harvest system of the game
     * @param returnOrder the order type a worker gets to bring what it gathered to a drop-off
     * @param searchRange the distance in pixels from the order's target point within which a worker looks for another container
     */
    public GatherBehavior(@NotNull StandardHarvestSystem harvest, @NotNull OrderType returnOrder, int searchRange) {
        this.harvest     = harvest;
        this.returnOrder = returnOrder;
        this.searchRange = searchRange;
    }

    /**
     * Starts the order of a unit when it becomes the unit's current order.
     * <p>
     * The order's target unit becomes the worker's container and its gathering trip restarts.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order starts
     */
    @Override
    public void start(@NotNull OrderContext context, @NotNull Unit unit) {
        this.harvest.setGatherTarget(unit, unit.getOrderTargetUnit());
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
        int instructionLimit = context.getInstructionLimit();

        if (this.harvest.getCarriedAmount(unit) > 0) {
            game.getOrders().issue(unit, this.returnOrder, unit.getPositionX(), unit.getPositionY(), null, instructionLimit);

            return;
        }

        Unit container = this.harvest.getGatherTarget(unit);

        if (container == null) {
            container = this.harvest.findResourceContainer(null, unit.getOrderTargetX(), unit.getOrderTargetY(), this.searchRange);

            if (container == null) {
                game.getOrders().finishOrder(unit, instructionLimit);

                return;
            }

            unit.setOrderTargetUnit(container);
            this.harvest.setGatherTarget(unit, container);
        }

        MovementSystem movement = game.getMovement();

        if (!this.harvest.isInReach(unit, container)) {
            if (game.hasSystem(PathfindingSystem.KEY) &&
                !game.getPathfinding().isReachable(unit, container.getPositionX(), container.getPositionY())) {
                game.getOrders().finishOrder(unit, instructionLimit);
            } else if (unit.getMovementDestinationX() != container.getPositionX() || unit.getMovementDestinationY() != container.getPositionY()) {
                movement.setDestination(unit, container.getPositionX(), container.getPositionY());
            } else if (movement.isAtDestination(unit)) {
                game.getOrders().finishOrder(unit, instructionLimit);
            }

            return;
        }

        if (unit.getCurrentSpeed() != 0 || !movement.isAtDestination(unit)) {
            movement.stop(unit);
        }

        if (this.harvest.gather(unit, container)) {
            game.getOrders().issue(unit, this.returnOrder, unit.getPositionX(), unit.getPositionY(), null, instructionLimit);
        }
    }
}
