package org.fogofwarlabs.sc4j.engine.content.modifier;

import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.UpgradeType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adds a fixed bonus per level of the armor upgrade of a unit type, read from the unit type table:
 * {@code value + level(armorUpgrade) * bonus}. A unit type without an armor upgrade keeps its value. Meant for
 * {@link Stat#ARMOR}.
 */
public final class ArmorUpgradeModifier implements StatModifier {
    private final int bonusPerLevel;

    /**
     * Creates an {@link ArmorUpgradeModifier} instance.
     *
     * @param bonusPerLevel the armor added per level
     */
    public ArmorUpgradeModifier(int bonusPerLevel) {
        this.bonusPerLevel = bonusPerLevel;
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
        UpgradeType upgrade = unitType != null ? content.getUnitTypes().getArmorUpgrade(unitType) : null;

        if (upgrade == null) {
            return value;
        }

        return value + content.getProgress().getUpgradeLevel(player, upgrade) * this.bonusPerLevel;
    }
}
