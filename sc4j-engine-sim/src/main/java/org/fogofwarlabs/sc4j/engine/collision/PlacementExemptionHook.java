package org.fogofwarlabs.sc4j.engine.collision;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Decides whether one unit blocks the placement of another where a pair of unit kinds is exempt from the layer rule of placement
 * ({@link CollisionSystem#PLACEMENT_EXEMPTION}): for example a producer that blocks the air units it produces, although they fly.
 */
@FunctionalInterface
public interface PlacementExemptionHook {
    /**
     * Answers whether another unit blocks a unit being put down. The rule asks before its layer rule, after it found that both units collide at
     * all.
     *
     * @param unit      the unit being put down
     * @param otherUnit the unit already there
     * @return {@link CollisionAnswer#BLOCKS} or {@link CollisionAnswer#PASSES}, or {@link CollisionAnswer#NONE} if this hook gives no answer
     */
    @NotNull CollisionAnswer answerPlacementBlocking(@NotNull Unit unit, @NotNull Unit otherUnit);
}
