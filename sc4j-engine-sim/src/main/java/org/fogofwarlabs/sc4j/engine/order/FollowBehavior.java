package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.hook.HookList;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.jetbrains.annotations.NotNull;

/**
 * Follows the target unit of the current order: the engine default behavior of a follow order. A mod registers it for its follow order type
 * with the hooks of {@link OrderSystem#FOLLOW_TARGET}.
 * {@snippet :
 * new StandardOrderSystem((game, behaviors) -> behaviors.register(follow, new FollowBehavior(game.getHooks(OrderSystem.FOLLOW_TARGET), 96)));
 *}
 * <p>
 * Each update first ends the order when the target is gone (the order system's next order or idle order follows); otherwise it asks the follow
 * target hooks in registration order whether the unit uses its target, and stops there when one answers. A unit that does not use its target
 * stops within the follow range of it and otherwise moves after it ({@link MovementSystem#setTargetUnit}). The distance is the Euclidean distance
 * between the unit positions.
 * <p>
 * Determinism contract: the behavior draws no random value and allocates nothing; the hooks run in registration order until one answers.
 */
public final class FollowBehavior implements OrderBehavior<OrderContext, Unit> {
    private final @NotNull HookList<FollowTargetHook> hooks;
    private final int followRange;

    /**
     * Creates a {@link FollowBehavior} instance.
     *
     * @param hooks       the hooks of {@link OrderSystem#FOLLOW_TARGET} of the game
     * @param followRange the distance in pixels within which the unit stops after its target, at least {@code 0}
     */
    public FollowBehavior(@NotNull HookList<FollowTargetHook> hooks, int followRange) {
        this.hooks       = hooks;
        this.followRange = followRange;
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
        Unit target = unit.getOrderTargetUnit();

        if (target == null) {
            game.getOrders().finishOrder(unit, context.getInstructionLimit());

            return;
        }

        for (int index = 0; index < this.hooks.size(); index++) {
            if (this.hooks.get(index).useTarget(unit, target, context.getInstructionLimit())) {
                return;
            }
        }

        long distanceX = target.getPositionX() - unit.getPositionX();
        long distanceY = target.getPositionY() - unit.getPositionY();
        long range = this.followRange;
        MovementSystem movement = game.getMovement();

        if (distanceX * distanceX + distanceY * distanceY <= range * range) {
            movement.stop(unit);
        } else {
            movement.setTargetUnit(unit, target);
        }
    }
}
