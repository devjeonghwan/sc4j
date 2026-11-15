package org.fogofwarlabs.sc4j.engine.collision;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Tests whether one unit blocks another in a {@link UnitCollisionSearch}. A caller keeps one test object per rule; the search calls it for every
 * candidate in edge order.
 * <p>
 * Determinism contract: a test reads the game state only, changes nothing and draws no random value.
 *
 * @param <U> the unit class
 */
@FunctionalInterface
public interface BlockingTest<U extends Unit> {
    /**
     * Checks whether another unit blocks a unit.
     *
     * @param unit      the moving or placed unit
     * @param otherUnit the candidate unit
     * @return {@code true} if the other unit blocks the unit, {@code false} otherwise
     */
    boolean blocks(@NotNull U unit, @NotNull U otherUnit);
}
