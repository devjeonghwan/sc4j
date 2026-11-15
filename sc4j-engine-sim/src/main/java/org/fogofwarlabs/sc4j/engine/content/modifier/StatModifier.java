package org.fogofwarlabs.sc4j.engine.content.modifier;

import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Changes the value of one {@link Stat}: how an upgrade or a tech changes a type value for a player. A mod registers
 * modifiers per stat in the content; {@link GameContent#applyModifiers} applies them.
 * <p>
 * Determinism contract: the modifiers of a stat are applied in registration order, each to the result of the previous
 * one, starting from the table value. A modifier is immutable and shared by every game of a definition; it reads only
 * the content it is given, draws no random value, changes no state and allocates nothing. Effects of a unit's own state
 * (status effects, abilities) are not modifiers: the systems owning that state apply them.
 */
@FunctionalInterface
public interface StatModifier {
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
    int apply(@NotNull GameContent content, @NotNull Player player, @Nullable UnitType unitType, @Nullable WeaponType weaponType, int value);
}
