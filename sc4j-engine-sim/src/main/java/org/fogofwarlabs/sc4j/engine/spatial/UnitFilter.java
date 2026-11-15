package org.fogofwarlabs.sc4j.engine.spatial;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Selects the units a spatial query returns. A caller keeps one filter object per query kind; the query calls it for
 * every candidate in index order.
 * <p>
 * Determinism contract: a filter reads the game state only, changes nothing and draws no random value.
 */
@FunctionalInterface
public interface UnitFilter {
    /**
     * A filter that accepts every unit.
     */
    @NotNull UnitFilter ANY = unit -> true;

    /**
     * Checks whether a query returns a unit.
     *
     * @param unit the candidate unit
     * @return {@code true} if the query returns the unit, {@code false} otherwise
     */
    boolean accepts(@NotNull Unit unit);
}
