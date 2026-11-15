package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Decides that a following unit uses its target instead of following it, for example by entering it or recharging at it
 * ({@link OrderSystem#FOLLOW_TARGET}).
 */
@FunctionalInterface
public interface FollowTargetHook {
    /**
     * Checks whether a following unit uses its target; a hook that answers {@code true} has given the unit the order that uses the target.
     *
     * @param unit             the following unit
     * @param target           the followed unit
     * @param instructionLimit the instruction limit of each animation program run the change of the unit's order starts
     * @return {@code true} if the unit uses its target and no longer follows it, {@code false} if this hook gives no answer
     */
    boolean useTarget(@NotNull Unit unit, @NotNull Unit target, int instructionLimit);
}
