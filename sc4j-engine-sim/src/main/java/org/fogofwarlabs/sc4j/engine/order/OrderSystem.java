package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.hook.HookComposition;
import org.fogofwarlabs.sc4j.engine.hook.HookPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Issues, queues and finishes the orders of units and dispatches the current order each frame. Commands, abilities, combat,
 * production and computer players issue and finish orders here; what an order does is the {@link OrderBehavior} the mod
 * registers per order type in the order system's {@link OrderRegistry}.
 * <p>
 * The order state lives on the unit: the current order type ({@link Unit#getOrderType()}), its target point and target unit, and the
 * queue of the orders that follow it ({@link Unit#getFirstOrder()}), whose {@link org.fogofwarlabs.sc4j.engine.entity.QueuedOrder}
 * members the order system pools. A target is a point, a unit or both; positions are pixels. The engine default is
 * {@link StandardOrderSystem}.
 * <p>
 * An order behavior may run animation programs (for example to return a unit to its idle animation when its order changes); the
 * operations that can change the current order take the instruction limit of each such run.
 * <p>
 * Determinism contract: the unit phase calls {@link #update} once per unit and running frame, in pool order; issuing
 * and queuing change only the named unit's queue, in call order, draw no random value and take queue nodes from the
 * implementation's pool without allocating.
 */
public interface OrderSystem extends GameSystem {
    /**
     * The key of the order system.
     */
    @NotNull SystemKey<OrderSystem> KEY = SystemKey.of("orders", OrderSystem.class);
    /**
     * The point a disabled unit, or an immobile unit that cannot attack, runs its main order at
     * ({@link HookComposition#EVERY_IN_ORDER}).
     */
    @NotNull HookPoint<DisabledOrderHook> DISABLED_ORDER = HookPoint.of("order.disabled", DisabledOrderHook.class, HookComposition.EVERY_IN_ORDER);
    /**
     * The point the order timer of a unit runs its timed order at, before the order runs ({@link HookComposition#EVERY_IN_ORDER}).
     */
    @NotNull HookPoint<OrderTimerHook> ORDER_TIMER = HookPoint.of("order.timer", OrderTimerHook.class, HookComposition.EVERY_IN_ORDER);
    /**
     * The point a following unit may use its target at instead of following it, for example by entering it ({@link HookComposition#FIRST_ANSWER};
     * the default is that the unit follows). The engine {@link FollowBehavior} asks it in each update.
     */
    @NotNull HookPoint<FollowTargetHook> FOLLOW_TARGET = HookPoint.of("order.follow-target", FollowTargetHook.class, HookComposition.FIRST_ANSWER);

    /**
     * Replaces a unit's orders with one order, which becomes the current order.
     *
     * @param unit             the unit
     * @param orderType        the order type
     * @param targetX          the x coordinate of the target point
     * @param targetY          the y coordinate of the target point
     * @param targetUnit       the target unit, or {@code null}
     * @param instructionLimit the instruction limit of each animation program run the change of the current order starts
     */
    void issue(@NotNull Unit unit, @NotNull OrderType orderType, int targetX, int targetY, @Nullable Unit targetUnit, int instructionLimit);

    /**
     * Appends an order after a unit's queued orders.
     *
     * @param unit       the unit
     * @param orderType  the order type
     * @param targetX    the x coordinate of the target point
     * @param targetY    the y coordinate of the target point
     * @param targetUnit the target unit, or {@code null}
     * @return {@code true} if the order was queued, {@code false} if the queue is full
     */
    boolean queue(@NotNull Unit unit, @NotNull OrderType orderType, int targetX, int targetY, @Nullable Unit targetUnit);

    /**
     * Ends a unit's current order; the next queued order, or the idle order of the unit's type, becomes current.
     *
     * @param unit             the unit
     * @param instructionLimit the instruction limit of each animation program run the change of the current order starts
     */
    void finishOrder(@NotNull Unit unit, int instructionLimit);

    /**
     * Runs the current order of one unit for one frame.
     *
     * @param unit             the unit
     * @param instructionLimit the instruction limit of each animation program run the order starts
     */
    void update(@NotNull Unit unit, int instructionLimit);

    /**
     * Removes the queued orders of a unit that leaves the game and returns them to the order pool.
     *
     * @param unit the unit
     */
    void removeOrders(@NotNull Unit unit);
}
