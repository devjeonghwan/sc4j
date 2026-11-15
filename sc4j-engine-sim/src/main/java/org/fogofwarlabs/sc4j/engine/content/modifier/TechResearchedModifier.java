package org.fogofwarlabs.sc4j.engine.content.modifier;

import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.type.TechType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adds a fixed bonus once the player has researched one tech, for selected unit types.
 */
public final class TechResearchedModifier implements StatModifier {
    private final @NotNull TechType tech;
    private final int bonus;
    private final @NotNull UnitTypeSelection unitTypes;

    /**
     * Creates a {@link TechResearchedModifier} instance.
     *
     * @param tech      the tech
     * @param bonus     the bonus added while the tech is researched
     * @param unitTypes the unit types it applies to; none applies it to every type
     */
    public TechResearchedModifier(@NotNull TechType tech, int bonus, @NotNull UnitType @NotNull ... unitTypes) {
        this.tech      = tech;
        this.bonus     = bonus;
        this.unitTypes = new UnitTypeSelection(unitTypes);
    }

    /**
     * Applies this modifier to a value.
     *
     * @param content    the content of the game
     * @param player     the owner
     * @param unitType   the unit type, or {@code null} for a weapon stat without a unit
     * @param weaponType the weapon type for a weapon stat, otherwise {@code null}
     * @param value      the value before this modifier
     * @return the value after this modifier
     */
    @Override
    public int apply(@NotNull GameContent content, @NotNull Player player, @Nullable UnitType unitType, @Nullable WeaponType weaponType, int value) {
        if (!this.unitTypes.contains(unitType) || !content.getProgress().isTechResearched(player, this.tech)) {
            return value;
        }

        return value + this.bonus;
    }
}
