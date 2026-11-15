package org.fogofwarlabs.sc4j.engine.combat;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.order.OrderBehavior;
import org.fogofwarlabs.sc4j.engine.order.OrderContext;
import org.jetbrains.annotations.NotNull;

/**
 * Attacks the target unit of the current order with the standard combat system: the engine default behavior of an attack order. A mod
 * registers it for its attack order type.
 * {@snippet :
 * new StandardOrderSystem((game, behaviors) -> behaviors.register(attack, new AttackBehavior((StandardCombatSystem) game.getCombat())));
 *}
 * <p>
 * Each update ends the order when there is no target or the unit cannot attack it ({@link CombatSystem#canAttack}); the order system's next
 * order or idle order follows. A target in range is fired at whenever the weapon is ready ({@link StandardCombatSystem#fireWeapon}); a target out
 * of range is pursued ({@link CombatSystem#pursueTarget}), and the order ends when the pursuit ends.
 * <p>
 * Determinism contract: the behavior draws no random value and allocates nothing.
 */
public final class AttackBehavior implements OrderBehavior<OrderContext, Unit> {
    private final @NotNull StandardCombatSystem combat;

    /**
     * Creates an {@link AttackBehavior} instance.
     *
     * @param combat the combat system of the game
     */
    public AttackBehavior(@NotNull StandardCombatSystem combat) {
        this.combat = combat;
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
        int instructionLimit = context.getInstructionLimit();

        if (target == null || !this.combat.canAttack(unit, target)) {
            game.getOrders().finishOrder(unit, instructionLimit);

            return;
        }

        if (!this.combat.pursueTarget(unit, target, instructionLimit)) {
            game.getOrders().finishOrder(unit, instructionLimit);

            return;
        }

        this.combat.fireWeapon(unit, target, instructionLimit);
    }
}
