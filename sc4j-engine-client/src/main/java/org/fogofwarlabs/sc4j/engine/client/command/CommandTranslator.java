package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.client.input.*;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Translates the local player's input into commands for the selected units, the input to command step of an RTS client:
 * <ul>
 * <li>a key press (or a repeat of a repeatable binding) of a bound key runs its {@link CommandAction} ({@link #run}); a targeted action waits
 * for a target as the pending action of the {@link TargetingMode};</li>
 * <li>with a pending action, a left press on the map ({@link TargetPicker}) issues it at the target (an action that refuses the target keeps
 * waiting), a right press or Escape cancels it;</li>
 * <li>without a pending action, a right press on the map issues the default action (for example move or attack) at the target.</li>
 * </ul>
 * Shift held queues the commands after the units' current orders. The commands go to the {@link CommandSink} in the order they are issued;
 * no action runs and no default action is issued while the selection is empty. An action the player cannot run now
 * ({@link CommandAction#findRefusal}) neither runs nor waits, and a pending action that became refused is dropped when it would be issued;
 * each refusal goes to the refusal listener ({@link #setRefusalListener}). A pending action started with a selection is issued even if
 * the selection emptied meanwhile: the action decides what it submits.
 * <p>
 * The picker reads the event's position: a client that hands the translator events in its own coordinates (for example map pixels,
 * {@link InputEvent#createAt}) gives it a picker of those coordinates.
 */
public final class CommandTranslator {
    private final @NotNull Player player;
    private final @NotNull CommandSink sink;
    private final @NotNull Selection selection;
    private final @NotNull KeyBindingTable<CommandAction> bindings;
    private final @NotNull TargetPicker picker;
    private final @NotNull TargetingMode targeting;

    private @Nullable CommandAction defaultAction;
    private @Nullable Consumer<@NotNull CommandRefusal> refusalListener;

    /**
     * Creates a {@link CommandTranslator} instance without a default action and with its own targeting mode.
     *
     * @param player    the local player who issues the commands
     * @param sink      the receiver of the commands
     * @param selection the player's selection
     * @param bindings  the key bindings of the actions
     * @param picker    the finder of the target the player points at
     */
    public CommandTranslator(@NotNull Player player,
                             @NotNull CommandSink sink,
                             @NotNull Selection selection,
                             @NotNull KeyBindingTable<CommandAction> bindings,
                             @NotNull TargetPicker picker) {
        this(player, sink, selection, bindings, picker, new TargetingMode());
    }

    /**
     * Creates a {@link CommandTranslator} instance without a default action.
     *
     * @param player    the local player who issues the commands
     * @param sink      the receiver of the commands
     * @param selection the player's selection
     * @param bindings  the key bindings of the actions
     * @param picker    the finder of the target the player points at
     * @param targeting the targeting mode, shared with the interface that shows it
     */
    public CommandTranslator(@NotNull Player player,
                             @NotNull CommandSink sink,
                             @NotNull Selection selection,
                             @NotNull KeyBindingTable<CommandAction> bindings,
                             @NotNull TargetPicker picker,
                             @NotNull TargetingMode targeting) {
        this.player    = player;
        this.sink      = sink;
        this.selection = selection;
        this.bindings  = bindings;
        this.picker    = picker;
        this.targeting = targeting;

        this.defaultAction   = null;
        this.refusalListener = null;
    }

    /**
     * @return the local player who issues the commands
     */
    public @NotNull Player getPlayer() {
        return this.player;
    }

    /**
     * @return the receiver of the commands
     */
    public @NotNull CommandSink getSink() {
        return this.sink;
    }

    /**
     * @return the player's selection
     */
    public @NotNull Selection getSelection() {
        return this.selection;
    }

    /**
     * @return the key bindings of the actions
     */
    public @NotNull KeyBindingTable<CommandAction> getBindings() {
        return this.bindings;
    }

    /**
     * @return the targeting mode that holds the pending action
     */
    public @NotNull TargetingMode getTargeting() {
        return this.targeting;
    }

    /**
     * @return the action of a right press on the map, or {@code null} if a right press issues nothing
     */
    public @Nullable CommandAction getDefaultAction() {
        return this.defaultAction;
    }

    /**
     * Sets the action of a right press on the map.
     *
     * @param defaultAction the action, or {@code null} if a right press issues nothing
     */
    public void setDefaultAction(@Nullable CommandAction defaultAction) {
        this.defaultAction = defaultAction;
    }

    /**
     * @return the receiver of the refusals of the actions the player tries to run, or {@code null} for none
     */
    public @Nullable Consumer<@NotNull CommandRefusal> getRefusalListener() {
        return this.refusalListener;
    }

    /**
     * Sets the receiver of the refusals of the actions the player tries to run, for example to show a message.
     *
     * @param refusalListener the receiver, or {@code null} for none
     */
    public void setRefusalListener(@Nullable Consumer<@NotNull CommandRefusal> refusalListener) {
        this.refusalListener = refusalListener;
    }

    /**
     * @return the targeted action waiting for its target, or {@code null}
     */
    public @Nullable CommandAction getPendingAction() {
        return this.targeting.getAction();
    }


    /**
     * Runs an action for the selected units: an action without a target issues its commands now, a targeted one becomes the pending action;
     * an action the player cannot run now does neither, and its refusal goes to the refusal listener. A key binding or a command card button
     * calls it.
     *
     * @param action the action
     * @param queued whether the commands queue after the units' current orders
     * @return {@code true} if the action ran, waits for its target or was refused, {@code false} if the selection is empty
     */
    public boolean run(@NotNull CommandAction action, boolean queued) {
        if (this.selection.isEmpty()) {
            return false;
        }

        if (this.refuse(action)) {
            return true;
        }

        if (action.isTargeted()) {
            this.targeting.start(action);
        } else {
            action.issue(this, null, queued);
        }

        return true;
    }

    /**
     * Drops the pending action.
     *
     * @return {@code true} if an action was pending, {@code false} otherwise
     */
    public boolean cancel() {
        return this.targeting.cancel();
    }

    /**
     * Handles an input event: a bound key runs its action, Escape or a right press cancels the pending action, a left press on the map issues
     * the pending action and a right press on the map the default action.
     *
     * @param event the event
     * @return {@code true} if the event ran, issued or cancelled an action or a pending action refused the target, {@code false} otherwise
     */
    public boolean handleInput(@NotNull InputEvent event) {
        boolean queued = event.isModifierDown(KeyModifier.SHIFT);

        if (event.isKeyDown()) {
            if (event.getKey() == Key.ESCAPE && this.cancel()) {
                return true;
            }

            KeyBinding<CommandAction> binding = this.bindings.find(event);

            return binding != null && this.run(binding.getAction(), queued);
        }

        if (event.getType() != InputEvent.Type.BUTTON_PRESSED) {
            return false;
        }

        CommandAction pending = this.targeting.getAction();

        if (event.getButton() == PointerButton.RIGHT && pending != null) {
            return this.cancel();
        }

        CommandAction action = event.getButton() == PointerButton.LEFT ? pending
                                                                       : event.getButton() == PointerButton.RIGHT ? this.defaultAction : null;

        if (action == null || pending == null && this.selection.isEmpty()) {
            return false;
        }

        CommandTarget target = this.picker.pick(event.getX(), event.getY());

        if (target == null) {
            return false;
        }

        if (action == pending && this.refuse(action)) {
            this.targeting.cancel();

            return true;
        }

        // The pending action stays pending while it is issued (it may read it) and keeps waiting if it refuses the target.
        if (action.issue(this, target, queued) && this.targeting.getAction() == action) {
            this.targeting.cancel();
        }

        return true;
    }

    /**
     * Checks whether an action is refused now and hands its refusal to the refusal listener.
     *
     * @return {@code true} if the action is refused, {@code false} otherwise
     */
    private boolean refuse(@NotNull CommandAction action) {
        CommandRefusal refusal = action.findRefusal(this);
        Consumer<CommandRefusal> listener = this.refusalListener;

        if (refusal == null) {
            return false;
        }

        if (listener != null) {
            listener.accept(refusal);
        }

        return true;
    }
}
