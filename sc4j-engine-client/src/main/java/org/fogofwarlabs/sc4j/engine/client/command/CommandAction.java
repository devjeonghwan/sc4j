package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Turns the player's input into commands: a key binding or a button of the command card runs an action, which submits commands for the
 * selected units to the translator's {@link org.fogofwarlabs.sc4j.engine.command.CommandSink}. A targeted action first waits for the player to
 * point at its target ({@link CommandTranslator}). An action that costs something states its cost ({@link #getCost}, {@link #getSupplyCost}) and
 * why it cannot run now ({@link #findRefusal}); the translator does not run a refused action, and the command card shows it disabled. An
 * action that may run but whose result will wait says why ({@link #findWarning}), for example a unit queued without the supply it needs.
 */
public interface CommandAction {
    /**
     * @return {@code true} if the action needs a target the player points at, {@code false} if it runs at once
     */
    boolean isTargeted();

    /**
     * Submits the action's commands. A targeted action is still the waiting action of the translator's {@link TargetingMode} while it is
     * issued; it may refuse the target (for example a place a building cannot stand on), and then it keeps waiting for another.
     *
     * @param translator the translator: the local player, the selection and the command sink
     * @param target     the target the player pointed at, or {@code null} for an action without a target
     * @param queued     whether the commands queue after the units' current orders (Shift held)
     * @return {@code true} if the action took the target (or ran without one), {@code false} if it refused the target
     */
    boolean issue(@NotNull CommandTranslator translator, @Nullable CommandTarget target, boolean queued);

    /**
     * Returns the resources the action spends. The default spends none.
     *
     * @return the cost
     */
    default @NotNull ResourceCost getCost() {
        return ResourceCost.NONE;
    }

    /**
     * Returns the supply the action's unit takes. The default takes none.
     *
     * @return the supply
     */
    default int getSupplyCost() {
        return 0;
    }

    /**
     * Finds why the action cannot run now for the translator's player and selection, for example a missing requirement or resource. The
     * default is never refused.
     *
     * @param translator the translator: the local player and the selection
     * @return the refusal, or {@code null} if the action may run
     */
    default @Nullable CommandRefusal findRefusal(@NotNull CommandTranslator translator) {
        return null;
    }

    /**
     * Finds why the result of the action, which may run now, will wait, for example the missing supply of a unit that a production queues
     * without holding the supply first. The command card shows it in the button's tooltip. The default never warns.
     *
     * @param translator the translator: the local player and the selection
     * @return the reason of the wait, or {@code null} if the result will not wait for it
     */
    default @Nullable CommandRefusal findWarning(@NotNull CommandTranslator translator) {
        return null;
    }
}
