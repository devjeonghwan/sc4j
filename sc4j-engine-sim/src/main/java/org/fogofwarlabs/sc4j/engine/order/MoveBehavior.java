package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.math.IntegerMathUtils;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Moves a unit to the target point of the current order: the engine default behavior of a move order. A mod registers it for its move order
 * type with the arrival distance and, for groups, a {@link GroupArrival}.
 * {@snippet :
 * new StandardOrderSystem((game, behaviors) -> behaviors.register(move, new MoveBehavior(16)));
 *}
 * <p>
 * When the order starts, the target point becomes the unit's destination ({@link MovementSystem#setDestination}); the movement system finds the
 * way. The order ends when the unit is within the arrival distance of the target point (the unit stops there), when it arrived with its group
 * ({@link GroupArrival#hasArrived}: it touches a unit of its group that stands nearer to the point, or the structure the point lies on), so that
 * a group sent to one point does not push forever for the exact point, or when the unit stands still at its destination, which is also the
 * case when the movement stopped it because the point cannot be reached. The distance is the Euclidean distance between the unit position and
 * the point.
 * <p>
 * Determinism contract: the behavior draws no random value and allocates nothing.
 */
public final class MoveBehavior implements OrderBehavior<OrderContext, Unit> {
    private final int arrivalDistance;
    private final @Nullable GroupArrival groupArrival;

    /**
     * Creates a {@link MoveBehavior} instance without group arrival.
     *
     * @param arrivalDistance the distance in pixels from the target point within which the order ends, at least {@code 0}
     */
    public MoveBehavior(int arrivalDistance) {
        this(arrivalDistance, null);
    }

    /**
     * Creates a {@link MoveBehavior} instance.
     *
     * @param arrivalDistance the distance in pixels from the target point within which the order ends, at least {@code 0}
     * @param groupArrival    the rule of a unit that arrives with its group, or {@code null} for none
     */
    public MoveBehavior(int arrivalDistance, @Nullable GroupArrival groupArrival) {
        this.arrivalDistance = arrivalDistance;
        this.groupArrival    = groupArrival;
    }

    /**
     * Starts the order of a unit when it becomes the unit's current order.
     * <p>
     * The target point becomes the unit's destination.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order starts
     */
    @Override
    public void start(@NotNull OrderContext context, @NotNull Unit unit) {
        context.getGame().getMovement().setDestination(unit, unit.getOrderTargetX(), unit.getOrderTargetY());
    }

    /**
     * Runs the order of one unit for one frame.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order runs
     */
    @Override
    public void update(@NotNull OrderContext context, @NotNull Unit unit) {
        MovementSystem movement = context.getGame().getMovement();
        GroupArrival group = this.groupArrival;
        boolean arrived = IntegerMathUtils.isWithinDistance(unit.getPositionX(), unit.getPositionY(), unit.getOrderTargetX(), unit.getOrderTargetY(),
                                                            this.arrivalDistance) ||
                          group != null && group.hasArrived(context.getGame(), unit, unit.getOrderTargetX(), unit.getOrderTargetY());

        if (arrived || movement.isAtDestination(unit)) {
            movement.stop(unit);
            context.getGame().getOrders().finishOrder(unit, context.getInstructionLimit());
        }
    }
}
