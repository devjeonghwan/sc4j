package org.fogofwarlabs.sc4j.engine.content.combat;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one hit while the damage layers of the hit unit take their parts ({@link DamageLayer#absorbDamage}): the hit unit, the attacking unit,
 * the damage kind and the damage that is left. A combat system keeps one instance and sets it for every hit ({@link #set}).
 */
public final class DamageHit {
    private @Nullable Unit targetUnit;
    private @Nullable Unit attackerUnit;
    private @Nullable DamageKind damageKind;
    private int damage;

    /**
     * Creates a {@link DamageHit} instance without a hit unit; it is set before use ({@link #set}).
     */
    public DamageHit() {
        this.targetUnit   = null;
        this.attackerUnit = null;
        this.damageKind   = null;
        this.damage       = 0;
    }

    /**
     * @return the hit unit
     * @throws IllegalStateException if no hit is set
     */
    public @NotNull Unit getTargetUnit() {
        Unit unit = this.targetUnit;

        if (unit == null) {
            throw Exceptions.newIllegalStateException("The damage hit is not set.");
        }

        return unit;
    }

    /**
     * @return the attacking unit, or {@code null} without attacker
     */
    public @Nullable Unit getAttackerUnit() {
        return this.attackerUnit;
    }

    /**
     * @return the damage kind of the hit, or {@code null} for a hit without one
     */
    public @Nullable DamageKind getDamageKind() {
        return this.damageKind;
    }

    /**
     * @return the damage that is left, in the combat system's fixed-point unit
     */
    public int getDamage() {
        return this.damage;
    }

    /**
     * Sets the damage that is left.
     *
     * @param damage the damage, in the combat system's fixed-point unit
     */
    public void setDamage(int damage) {
        this.damage = damage;
    }

    /**
     * Sets the hit.
     *
     * @param targetUnit   the hit unit
     * @param attackerUnit the attacking unit, or {@code null} without attacker
     * @param damageKind   the damage kind of the hit, or {@code null} for a hit without one
     * @param damage       the damage, in the combat system's fixed-point unit
     */
    public void set(@NotNull Unit targetUnit, @Nullable Unit attackerUnit, @Nullable DamageKind damageKind, int damage) {
        this.targetUnit   = targetUnit;
        this.attackerUnit = attackerUnit;
        this.damageKind   = damageKind;
        this.damage       = damage;
    }
}
