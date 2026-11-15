package org.fogofwarlabs.sc4j.engine.content.modifier;

import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.UpgradeType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adds a fixed bonus per level of one upgrade the player has, for selected unit types: {@code value + level * bonus}.
 */
public final class UpgradeLevelModifier implements StatModifier {
    private final @NotNull UpgradeType upgrade;
    private final int bonusPerLevel;
    private final @NotNull UnitTypeSelection unitTypes;

    /**
     * Creates an {@link UpgradeLevelModifier} instance.
     *
     * @param upgrade       the upgrade
     * @param bonusPerLevel the bonus added per level
     * @param unitTypes     the unit types it applies to; none applies it to every type
     */
    public UpgradeLevelModifier(@NotNull UpgradeType upgrade, int bonusPerLevel, @NotNull UnitType @NotNull ... unitTypes) {
        this.upgrade       = upgrade;
        this.bonusPerLevel = bonusPerLevel;
        this.unitTypes     = new UnitTypeSelection(unitTypes);
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
        if (!this.unitTypes.contains(unitType)) {
            return value;
        }

        return value + content.getProgress().getUpgradeLevel(player, this.upgrade) * this.bonusPerLevel;
    }
}
