package org.fogofwarlabs.sc4j.engine.projectile;

import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.jetbrains.annotations.NotNull;

/**
 * Flies the projectiles of weapons: the combat system launches one when a weapon with a projectile fires, and the projectile hits its target
 * when it arrives ({@link org.fogofwarlabs.sc4j.engine.combat.CombatSystem#dealWeaponDamage}). A mod without projectiles registers none; its
 * weapons then hit at once.
 * <p>
 * Determinism contract: a launch takes the first free slot of the game's projectile pool and draws no random value; the projectiles move in
 * slot order once per running frame of the system's phase, and a projectile hits in its own step, in that order.
 */
public interface ProjectileSystem extends GameSystem {
    /**
     * The key of the projectile system.
     */
    @NotNull SystemKey<ProjectileSystem> KEY = SystemKey.of("projectiles", ProjectileSystem.class);

    /**
     * @return the number of projectiles in flight
     */
    int getActiveCount();

    /**
     * @return the number of direction steps of a full turn of the projectiles' headings, clockwise from up
     */
    int getDirectionCount();


    /**
     * Launches the projectile of a weapon from a unit at a target unit.
     *
     * @param weapon           the weapon, which names the projectile's motion profile
     * @param sourceUnit       the attacking unit, where the projectile starts
     * @param targetUnit       the target unit
     * @param instructionLimit the instruction limit of each animation program run the launch starts
     * @return {@code true} if a projectile was launched, {@code false} if the weapon has no projectile or the projectile pool is full
     */
    boolean launch(@NotNull WeaponType weapon, @NotNull Unit sourceUnit, @NotNull Unit targetUnit, int instructionLimit);
}
