package org.fogofwarlabs.sc4j.engine.trigger;

import org.jetbrains.annotations.NotNull;

/**
 * Runs one kind of trigger action. A mod registers one action per action type id of its trigger format in a {@link TriggerRegistry}; one
 * instance may serve several types that share their effect.
 * <p>
 * The trigger system runs the actions of a trigger whose conditions held in list order. An action that does not finish (a wait, or an effect
 * that cannot happen yet) ends the actions of the trigger for this run; the next run of the trigger resumes at that action, so an action keeps
 * what it needs between the runs in the running trigger. An action is stateless otherwise and allocates nothing per call.
 * <p>
 * Determinism contract: an action changes the game only through its systems, units and tables and the random generator, in the order its code
 * states; what it does depends only on the game, the context, the running trigger and the action record.
 *
 * @param <C> the context type of the trigger system
 * @param <T> the running trigger type of the trigger system: the trigger whose actions run, with its execution state
 * @param <A> the action record type of the trigger system
 */
@FunctionalInterface
public interface TriggerAction<C extends TriggerContext, T, A> {
    /**
     * Runs one action of a trigger.
     *
     * @param context the context of the evaluation
     * @param trigger the running trigger
     * @param action  the action record
     * @return {@code true} if the action finished and the next action runs, {@code false} if the trigger waits at this action
     */
    boolean execute(@NotNull C context, @NotNull T trigger, @NotNull A action);
}
