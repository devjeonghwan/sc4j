package org.fogofwarlabs.sc4j.engine.combat;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.order.OrderBehavior;
import org.fogofwarlabs.sc4j.engine.order.OrderContext;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps a unit where it stands and fires at enemies within its weapon range with the standard combat system: the engine default behavior of a
 * hold position order. A mod registers it for its hold position order type.
 * {@snippet :
 * behaviors.register(holdPosition, new HoldPositionBehavior((StandardCombatSystem) game.getCombat()));
 *}
 * <p>
 * When the order starts, the unit stops. Each update keeps the engaged enemy in the order's target unit while the unit can attack it and has it
 * in range ({@link CombatSystem#canAttack}, {@link CombatSystem#isInRange}); otherwise it takes the acquired enemy
 * ({@link StandardCombatSystem#findTarget}) if that one is in range, and fires at it when the weapon is ready
 * ({@link StandardCombatSystem#fireWeapon}). The unit never moves for an enemy, and the order does not end by itself.
 * <p>
 * Determinism contract: the behavior draws no random value and allocates nothing.
 */
public final class HoldPositionBehavior implements OrderBehavior<OrderContext, Unit> {
    private final @NotNull StandardCombatSystem combat;

    /**
     * Creates a {@link HoldPositionBehavior} instance.
     *
     * @param combat the combat system of the game
     */
    public HoldPositionBehavior(@NotNull StandardCombatSystem combat) {
        this.combat = combat;
    }

    /**
     * Starts the order of a unit when it becomes the unit's current order: the engine
     * {@link org.fogofwarlabs.sc4j.engine.order.StandardOrderSystem} calls it right after an
     * {@link org.fogofwarlabs.sc4j.engine.order.OrderSystem#issue} or {@link org.fogofwarlabs.sc4j.engine.order.OrderSystem#finishOrder} made the
     * order current, with the order's target already on the unit. An order system of a mod may run its own start rules instead.
     * <p>
     * The unit stops where it stands and engages no enemy yet.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order starts
     */
    @Override
    public void start(@NotNull OrderContext context, @NotNull Unit unit) {
        unit.setOrderTargetUnit(null);
        context.getGame().getMovement().stop(unit);
    }

    /**
     * Runs the order of one unit for one frame.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order runs
     */
    @Override
    public void update(@NotNull OrderContext context, @NotNull Unit unit) {
        Unit target = unit.getOrderTargetUnit();

        if (target == null || !this.combat.canAttack(unit, target) || !this.combat.isInRange(unit, target)) {
            target = this.combat.findTarget(unit);

            if (target != null && !this.combat.isInRange(unit, target)) {
                target = null;
            }

            unit.setOrderTargetUnit(target);
        }

        if (target != null) {
            this.combat.fireWeapon(unit, target, context.getInstructionLimit());
        }
    }
}
