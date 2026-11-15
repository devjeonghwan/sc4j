package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.jetbrains.annotations.NotNull;

/**
 * Stops a unit: the engine default behavior of a stop order. A mod registers it for its stop order type.
 * {@snippet :
 * new StandardOrderSystem((game, behaviors) -> behaviors.register(stop, new StopBehavior()));
 *}
 * <p>
 * The first update stops the unit where it is ({@link MovementSystem#stop}) and ends the order; the order system's next order or idle order
 * follows.
 * <p>
 * Determinism contract: the behavior draws no random value and allocates nothing.
 */
public final class StopBehavior implements OrderBehavior<OrderContext, Unit> {
    /**
     * Creates a {@link StopBehavior} instance.
     */
    public StopBehavior() {
    }

    /**
     * Runs the order of one unit for one frame.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order runs
     */
    @Override
    public void update(@NotNull OrderContext context, @NotNull Unit unit) {
        context.getGame().getMovement().stop(unit);
        context.getGame().getOrders().finishOrder(unit, context.getInstructionLimit());
    }
}
