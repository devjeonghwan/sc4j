package org.fogofwarlabs.sc4j.engine.collision;

/**
 * Answers a collision hook gives at a {@link org.fogofwarlabs.sc4j.engine.hook.HookComposition#FIRST_ANSWER} point: the other unit blocks the
 * unit, it does not, or the hook gives no answer and the next hook or the rule's default decides.
 */
public enum CollisionAnswer {
    /**
     * The other unit blocks the unit.
     */
    BLOCKS,
    /**
     * The other unit does not block the unit.
     */
    PASSES,
    /**
     * No answer: the next hook, or the rule's default when no hook answers, decides.
     */
    NONE
}
