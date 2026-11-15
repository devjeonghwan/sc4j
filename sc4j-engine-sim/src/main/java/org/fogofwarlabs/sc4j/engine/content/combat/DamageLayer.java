package org.fogofwarlabs.sc4j.engine.content.combat;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Takes damage of a unit before its hit points do: one layer of the ordered chain a mod registers per unit type
 * ({@link org.fogofwarlabs.sc4j.engine.content.table.GameContent#addDamageLayer}), each with its own points, armour and rules. A layer keeps
 * its points where the mod stores them (a unit column, a field of the mod's unit class); the combat system asks each layer of the hit
 * unit's type in chain order for its part of a hit ({@link #absorbDamage}) and takes the absorbed part off the layer's points
 * ({@link #setPoints}). The client shows a bar per layer under the layer's name.
 * <p>
 * Points and damage are in the combat system's fixed-point unit.
 * <p>
 * Determinism contract: a layer changes only the hit and the given unit, draws no random value and allocates nothing.
 */
public interface DamageLayer {
    /**
     * @return the name of the layer that the client shows with its bar
     */
    @NotNull String getName();

    /**
     * @param unit the unit
     * @return the points of the layer that the unit holds now
     */
    int getPoints(@NotNull Unit unit);

    /**
     * @param unit the unit
     * @return the points of the layer that the unit holds when the layer is full, {@code 0} if the unit's type has none of the layer
     */
    int getMaximumPoints(@NotNull Unit unit);

    /**
     * Sets the points of the layer that a unit holds.
     *
     * @param unit   the unit
     * @param points the points
     */
    void setPoints(@NotNull Unit unit, int points);

    /**
     * Takes the layer's part of a hit: changes the hit's damage by the layer's own rules, such as its armour, and moves the part the layer
     * absorbs out of the hit's damage ({@link DamageHit#setDamage}). The layer's points do not change yet: the combat system takes the
     * returned part off them ({@link #setPoints}) when its rules say.
     *
     * @param hit the hit, whose damage is the part the layers before this one left
     * @return the absorbed part of the damage, {@code 0} if the layer takes none
     */
    int absorbDamage(@NotNull DamageHit hit);
}
