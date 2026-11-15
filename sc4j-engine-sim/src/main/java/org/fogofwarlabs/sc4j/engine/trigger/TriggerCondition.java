package org.fogofwarlabs.sc4j.engine.trigger;

import org.jetbrains.annotations.NotNull;

/**
 * Tests one kind of trigger condition. A mod registers one condition per condition type id of its trigger format in a {@link TriggerRegistry};
 * one instance may serve several types that share their test.
 * <p>
 * A condition is stateless and does not change the game; it may only update the evaluation state of its context (for example a count cache
 * that later conditions of the same evaluation reuse). It runs for every tested condition of every evaluated trigger, so it allocates nothing
 * per call. The context and record types are the ones of the trigger system that tests the condition, which receives them without a cast.
 *
 * @param <C> the context type of the trigger system
 * @param <D> the condition record type of the trigger system: the operands of one condition as the system reads them
 */
@FunctionalInterface
public interface TriggerCondition<C extends TriggerContext, D> {
    /**
     * Tests one condition of a trigger.
     *
     * @param context   the context of the evaluation
     * @param condition the condition record
     * @return {@code true} if the condition holds, {@code false} otherwise
     */
    boolean test(@NotNull C context, @NotNull D condition);
}
