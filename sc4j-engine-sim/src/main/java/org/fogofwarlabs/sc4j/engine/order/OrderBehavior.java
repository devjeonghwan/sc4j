package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Defines what a unit does while it has one order type: the behavior an order system runs for the current order of a
 * unit. A mod registers one behavior per order type in an {@link OrderRegistry}; one instance may serve several order
 * types that share their behavior.
 * <p>
 * A behavior is stateless: everything that changes lives in the unit, the game and its systems. It runs inside the frame,
 * so it allocates nothing per call. The context and unit types are the ones of the order system that runs the behavior; a
 * mod whose units extend the engine {@link Unit} names its own unit type here and receives it without a cast.
 * <p>
 * Determinism contract: a behavior changes the game only through its unit, the game's systems and the random generator,
 * in the order its code states; it never depends on the identity hash or on iteration over hash-ordered collections.
 *
 * @param <C> the context type of the order system
 * @param <U> the unit type of the order system
 */
@FunctionalInterface
public interface OrderBehavior<C extends OrderContext, U extends Unit> {
    /**
     * Starts the order of a unit when it becomes the unit's current order: the engine {@link StandardOrderSystem} calls it right after an
     * {@link OrderSystem#issue} or {@link OrderSystem#finishOrder} made the order current, with the order's target already on the unit. An order
     * system of a mod may run its own start rules instead. The default does nothing.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order starts
     */
    default void start(@NotNull C context, @NotNull U unit) {
    }

    /**
     * Runs the order of one unit for one frame.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order runs
     */
    void update(@NotNull C context, @NotNull U unit);
}
