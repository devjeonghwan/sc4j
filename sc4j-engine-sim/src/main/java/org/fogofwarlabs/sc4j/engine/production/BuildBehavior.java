package org.fogofwarlabs.sc4j.engine.production;

import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.order.OrderBehavior;
import org.fogofwarlabs.sc4j.engine.order.OrderContext;
import org.fogofwarlabs.sc4j.engine.path.PathfindingSystem;
import org.jetbrains.annotations.NotNull;

/**
 * Walks a worker to a building site and places the building there with the standard production system: the engine default behavior of a
 * worker's build order. A mod registers it for the build order type of its production settings
 * ({@link StandardProductionSystem.Builder#setBuildOrder}) with the reach of its workers and whether a worker stays at the building until it is
 * complete; the production system issues the order ({@link StandardProductionSystem#orderConstruction}).
 * {@snippet :
 * behaviors.register(build, new BuildBehavior((StandardProductionSystem) game.getProduction(), 8, true));
 *}
 * <p>
 * The building type is the worker's construction type ({@link StandardProductionSystem#getConstructionType}) and the site the order's target
 * point, the center of the building. Each update: a worker that constructs a building stays by it while it is under construction, when the
 * behavior keeps workers there, and ends the order otherwise or once the building is complete or gone; a worker without construction type ends
 * the order; a worker out of reach of the site ({@link StandardProductionSystem#isNearSite}) walks to it and ends the order when the site cannot
 * be reached ({@link PathfindingSystem#isReachable}, with a path finding system) or it stands still out of reach; a worker in reach stops and
 * places the building ({@link StandardProductionSystem#placeBuilding}), and ends the order when the placement fails (the site is blocked or the
 * owner cannot pay) or when it does not stay. An ended order clears the worker's construction.
 * <p>
 * Determinism contract: the behavior draws no random value and allocates nothing.
 */
public final class BuildBehavior implements OrderBehavior<OrderContext, Unit> {
    private final @NotNull StandardProductionSystem production;
    private final int reach;
    private final boolean builderStays;

    /**
     * Creates a {@link BuildBehavior} instance.
     *
     * @param production   the production system of the game
     * @param reach        the largest distance in pixels between a worker and the building's footprint at which it places the building
     * @param builderStays whether a worker stays by its building until it is complete, instead of leaving once it is placed
     */
    public BuildBehavior(@NotNull StandardProductionSystem production, int reach, boolean builderStays) {
        this.production   = production;
        this.reach        = reach;
        this.builderStays = builderStays;
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
        Unit building = this.production.getConstructedBuilding(unit);

        if (building != null) {
            if (!this.builderStays || !this.production.isUnderConstruction(building)) {
                this.end(context, unit);
            }

            return;
        }

        UnitType type = this.production.getConstructionType(unit);

        if (type == null) {
            this.end(context, unit);

            return;
        }

        int siteX = unit.getOrderTargetX();
        int siteY = unit.getOrderTargetY();
        MovementSystem movement = game.getMovement();

        if (!this.production.isNearSite(unit, type, siteX, siteY, this.reach)) {
            if (game.hasSystem(PathfindingSystem.KEY) && !game.getPathfinding().isReachable(unit, siteX, siteY)) {
                this.end(context, unit);
            } else if (unit.getMovementDestinationX() != siteX || unit.getMovementDestinationY() != siteY) {
                movement.setDestination(unit, siteX, siteY);
            } else if (movement.isAtDestination(unit)) {
                this.end(context, unit);
            }

            return;
        }

        movement.stop(unit);

        Unit placed = this.production.placeBuilding(unit, type, siteX, siteY, context.getInstructionLimit());

        if (placed == null || !this.builderStays) {
            this.end(context, unit);

            return;
        }

        this.production.setConstructedBuilding(unit, placed);
    }

    private void end(@NotNull OrderContext context, @NotNull Unit unit) {
        this.production.clearConstruction(unit);
        context.getGame().getOrders().finishOrder(unit, context.getInstructionLimit());
    }
}
