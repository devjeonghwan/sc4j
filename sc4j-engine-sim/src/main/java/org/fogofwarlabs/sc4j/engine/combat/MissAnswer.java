package org.fogofwarlabs.sc4j.engine.combat;

/**
 * Answers a miss hook gives at the {@link org.fogofwarlabs.sc4j.engine.hook.HookComposition#FIRST_ANSWER} point
 * {@link StandardCombatSystem#MISS}: the hit misses, it hits, or the hook gives no answer and the next hook or the default (the hit hits) decides.
 */
public enum MissAnswer {
    /**
     * The hit misses: neither the target nor any unit around it takes damage.
     */
    MISS,
    /**
     * The hit hits.
     */
    HIT,
    /**
     * No answer: the next hook, or the default when no hook answers, decides.
     */
    NONE
}
