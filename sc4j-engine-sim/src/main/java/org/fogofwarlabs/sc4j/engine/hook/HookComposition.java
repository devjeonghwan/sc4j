package org.fogofwarlabs.sc4j.engine.hook;

/**
 * Says how the system that calls a hook point combines its hooks; every hook point declares one. With no hook, the
 * system runs its own default behavior at the point, whatever the composition.
 */
public enum HookComposition {
    /**
     * Every hook runs, in registration order; a hook returns nothing the system reads. Used for notifications.
     */
    EVERY_IN_ORDER,
    /**
     * Every hook runs, in registration order; each receives the result of the hook before it, the first receives the
     * system's default result, and the system uses the result of the last hook.
     */
    CHAINED,
    /**
     * The hooks are asked in registration order until one gives an answer; the system uses that answer, or its default
     * when no hook answers. The hooks after the answering one do not run.
     */
    FIRST_ANSWER,
    /**
     * At most one hook may be registered; registering a second fails. The hook replaces the default behavior at the
     * point.
     */
    AT_MOST_ONE
}
