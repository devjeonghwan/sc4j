package org.fogofwarlabs.sc4j.engine.combat;

/**
 * Names which units around a hit the splash damage of the standard combat system reaches ({@link StandardCombatSystem.Builder#setSplashTargets}).
 * The primary target of the hit always takes its share; the attacking unit never does.
 */
public enum SplashTargets {
    /**
     * Only the enemies of the attacker ({@link CombatSystem#isEnemy}); without an attacker, every unit.
     */
    ENEMIES,
    /**
     * Every unit, the attacker's own and allied units included.
     */
    ALL
}
