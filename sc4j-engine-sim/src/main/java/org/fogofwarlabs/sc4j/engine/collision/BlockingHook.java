package org.fogofwarlabs.sc4j.engine.collision;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Decides whether one unit blocks the movement of another where the general blocking rule leaves it to the unit kinds
 * ({@link CollisionSystem#BLOCKING}).
 */
@FunctionalInterface
public interface BlockingHook {
    /**
     * Answers whether another unit blocks the movement of a unit. The rule asks after it found that both units collide at all.
     *
     * @param unit      the moving unit
     * @param otherUnit the unit in the way
     * @return {@link CollisionAnswer#BLOCKS} or {@link CollisionAnswer#PASSES}, or {@link CollisionAnswer#NONE} if this hook gives no answer
     */
    @NotNull CollisionAnswer answerBlocking(@NotNull Unit unit, @NotNull Unit otherUnit);
}
