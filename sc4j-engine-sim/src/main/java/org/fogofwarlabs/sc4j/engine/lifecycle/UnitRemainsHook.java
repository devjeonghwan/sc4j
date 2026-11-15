package org.fogofwarlabs.sc4j.engine.lifecycle;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Decides whether a dying unit stays in the world as something else, at the point of its death after it left its owner
 * ({@link UnitLifecycle#UNIT_REMAINS}); a unit that remains ends its death there.
 */
@FunctionalInterface
public interface UnitRemainsHook {
    /**
     * Called while a unit in the world dies, after it left its owner. A hook that keeps the unit turns it into what remains of it.
     *
     * @param unit             the dying unit
     * @param instructionLimit the instruction limit of each animation program run the hook starts
     * @return {@code true} if the hook kept the unit in the world, which ends its death, {@code false} for no answer
     */
    boolean keepsUnit(@NotNull Unit unit, int instructionLimit);
}
