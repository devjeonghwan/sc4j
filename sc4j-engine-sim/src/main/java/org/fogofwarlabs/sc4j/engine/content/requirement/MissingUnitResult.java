package org.fogofwarlabs.sc4j.engine.content.requirement;

import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the failure of a requirement for completed units: the player lacks completed units of a unit type. A condition keeps one
 * instance and returns it from every failed check, so a check allocates nothing; the client names the missing unit type from it.
 */
public final class MissingUnitResult implements RequirementResult {
    private final @NotNull UnitType unitType;
    private final int count;

    /**
     * Creates a {@link MissingUnitResult} instance.
     *
     * @param unitType the unit type the player lacks completed units of
     * @param count    the number of completed units required
     */
    public MissingUnitResult(@NotNull UnitType unitType, int count) {
        this.unitType = unitType;
        this.count    = count;
    }

    /**
     * @return the unit type the player lacks completed units of
     */
    public @NotNull UnitType getUnitType() {
        return this.unitType;
    }

    /**
     * @return the number of completed units required
     */
    public int getCount() {
        return this.count;
    }

    /**
     * @return {@code true} if the requirement is met, {@code false} otherwise: never met
     */
    @Override
    public boolean isMet() {
        return false;
    }
}
