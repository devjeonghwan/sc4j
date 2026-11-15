package org.fogofwarlabs.sc4j.engine.combat;

import org.fogofwarlabs.sc4j.engine.content.combat.DamageKind;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

/**
 * Changes the damage of a weapon hit by its damage kind and the hit unit ({@link StandardCombatSystem#DAMAGE_SCALE}), for example half damage
 * of one kind against large units.
 */
@FunctionalInterface
public interface DamageScaleHook {
    /**
     * Returns the damage of a hit after this hook's rule.
     *
     * @param damageKind the damage kind of the weapon
     * @param targetUnit the hit unit
     * @param damage     the damage after armor, or the result of the hook before this one, in the combat system's fixed-point unit
     * @return the changed damage, or {@code damage} if the rule does not apply
     */
    int scaleDamage(@NotNull DamageKind damageKind, @NotNull Unit targetUnit, int damage);
}
