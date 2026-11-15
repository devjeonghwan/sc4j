package org.fogofwarlabs.sc4j.engine.combat;

import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decides whether a weapon hit misses its target ({@link StandardCombatSystem#MISS}), for example a shot from low ground at a unit on high
 * ground. A hook that draws a random value draws it from the game's generator, so that the rule stays deterministic.
 */
@FunctionalInterface
public interface MissHook {
    /**
     * Answers whether a hit misses.
     *
     * @param weapon       the weapon
     * @param attackerUnit the attacking unit, or {@code null} if it no longer lives
     * @param targetUnit   the target unit
     * @return {@link MissAnswer#MISS} or {@link MissAnswer#HIT}, or {@link MissAnswer#NONE} to leave the decision to the next hook
     */
    @NotNull MissAnswer answerMiss(@NotNull WeaponType weapon, @Nullable Unit attackerUnit, @NotNull Unit targetUnit);
}
