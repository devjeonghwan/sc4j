package org.fogofwarlabs.sc4j.engine.combat;

import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.order.OrderBehavior;
import org.fogofwarlabs.sc4j.engine.order.OrderContext;
import org.jetbrains.annotations.NotNull;

/**
 * Watches for enemies and attacks the first one in acquisition range with the standard combat system: the engine default behavior of an idle
 * guard order. A mod registers it for the idle order of its fighting unit types, with the attack order the unit then gets.
 * {@snippet :
 * StandardCombatSystem combat = (StandardCombatSystem) game.getCombat();
 * behaviors.register(guard, new GuardBehavior(combat, attack));
 * behaviors.register(attack, new AttackBehavior(combat));
 *}
 * <p>
 * Each update asks the combat system for a target ({@link StandardCombatSystem#findTarget}); when it finds one, the unit gets the attack order on
 * it, so after the attack ends the unit guards again.
 * <p>
 * Determinism contract: the behavior draws no random value and allocates nothing.
 */
public final class GuardBehavior implements OrderBehavior<OrderContext, Unit> {
    private final @NotNull StandardCombatSystem combat;
    private final @NotNull OrderType attackOrder;

    /**
     * Creates a {@link GuardBehavior} instance.
     *
     * @param combat      the combat system of the game
     * @param attackOrder the order type a unit gets on the target it finds
     */
    public GuardBehavior(@NotNull StandardCombatSystem combat, @NotNull OrderType attackOrder) {
        this.combat      = combat;
        this.attackOrder = attackOrder;
    }

    /**
     * Runs the order of one unit for one frame.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order runs
     */
    @Override
    public void update(@NotNull OrderContext context, @NotNull Unit unit) {
        Unit target = this.combat.findTarget(unit);

        if (target != null) {
            context.getGame().getOrders().issue(unit, this.attackOrder, target.getPositionX(), target.getPositionY(), target,
                                                context.getInstructionLimit());
        }
    }
}
