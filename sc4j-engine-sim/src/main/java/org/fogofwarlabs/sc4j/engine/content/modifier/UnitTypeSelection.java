package org.fogofwarlabs.sc4j.engine.content.modifier;

import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the unit types a modifier applies to: the listed types, or every type when none is listed.
 */
final class UnitTypeSelection {
    private final @NotNull UnitType @NotNull [] unitTypes;

    /**
     * Creates a {@link UnitTypeSelection} instance.
     *
     * @param unitTypes the selected unit types, copied; none selects every type
     */
    UnitTypeSelection(@NotNull UnitType @NotNull [] unitTypes) {
        this.unitTypes = unitTypes.clone();
    }

    /**
     * Checks whether a unit type is selected. The identities are compared by identity, so the selection can be created
     * before the types are registered.
     *
     * @param unitType the unit type, or {@code null} for a value without one
     * @return {@code true} if the selection is empty or lists the type, {@code false} otherwise
     */
    boolean contains(@Nullable UnitType unitType) {
        if (this.unitTypes.length == 0) {
            return true;
        }

        for (UnitType selected : this.unitTypes) {
            if (selected == unitType) {
                return true;
            }
        }

        return false;
    }
}
