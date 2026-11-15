package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.command.CommandAction;
import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one button of a {@link CommandCard}: its shown label, its hotkey and the {@link CommandAction} it runs, for example an
 * {@link org.fogofwarlabs.sc4j.engine.client.command.OrderAction} of one order type.
 */
public final class CommandButton {
    private final @NotNull String label;
    private final @Nullable Key hotkey;
    private final @NotNull CommandAction action;

    /**
     * Creates a {@link CommandButton} instance.
     *
     * @param label  the shown label
     * @param hotkey the key that presses the button without modifiers, or {@code null} for none
     * @param action the action the button runs
     */
    public CommandButton(@NotNull String label, @Nullable Key hotkey, @NotNull CommandAction action) {
        this.label  = label;
        this.hotkey = hotkey;
        this.action = action;
    }

    /**
     * @return the shown label
     */
    public @NotNull String getLabel() {
        return this.label;
    }

    /**
     * @return the key that presses the button without modifiers, or {@code null} for none
     */
    public @Nullable Key getHotkey() {
        return this.hotkey;
    }

    /**
     * @return the action the button runs
     */
    public @NotNull CommandAction getAction() {
        return this.action;
    }
}
