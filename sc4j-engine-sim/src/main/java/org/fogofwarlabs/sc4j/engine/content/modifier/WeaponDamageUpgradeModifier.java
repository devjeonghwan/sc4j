package org.fogofwarlabs.sc4j.engine.content.modifier;

import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.table.WeaponTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.UpgradeType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adds the damage bonus of a weapon type once per level of the weapon's own upgrade, both read from the weapon type
 * table: {@code value + level(upgrade) * damageBonus}. A weapon without an upgrade keeps its value. Meant for
 * {@link Stat#WEAPON_DAMAGE}.
 */
public final class WeaponDamageUpgradeModifier implements StatModifier {
    /**
     * Creates a {@link WeaponDamageUpgradeModifier} instance.
     */
    public WeaponDamageUpgradeModifier() {
        // The modifier reads everything from the tables.
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
        if (weaponType == null) {
            return value;
        }

        WeaponTypeTable weapons = content.getWeaponTypes();
        UpgradeType upgrade = weapons.getUpgrade(weaponType);

        if (upgrade == null) {
            return value;
        }

        return value + content.getProgress().getUpgradeLevel(player, upgrade) * weapons.getDamageBonus(weaponType);
    }
}
