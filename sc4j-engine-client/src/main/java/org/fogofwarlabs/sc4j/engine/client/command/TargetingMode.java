package org.fogofwarlabs.sc4j.engine.client.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the targeted action waiting for the player to point at its target: the targeting mode of an RTS interface. The
 * {@link CommandTranslator} starts it for a targeted action and issues it at the next target; the cursor and the command card read it. A
 * client whose interface enters a targeting mode by its own rules (for example its own command card) starts the action here, and the
 * translator issues it.
 */
public final class TargetingMode {
    private @Nullable CommandAction action;

    /**
     * Creates a {@link TargetingMode} instance with no action waiting.
     */
    public TargetingMode() {
        this.action = null;
    }

    /**
     * @return the targeted action waiting for its target, or {@code null}
     */
    public @Nullable CommandAction getAction() {
        return this.action;
    }

    /**
     * @return {@code true} if an action waits for its target, {@code false} otherwise
     */
    public boolean isActive() {
        return this.action != null;
    }


    /**
     * Lets an action wait for its target, in place of the one waiting.
     *
     * @param action the targeted action
     */
    public void start(@NotNull CommandAction action) {
        this.action = action;
    }

    /**
     * Drops the waiting action.
     *
     * @return {@code true} if an action was waiting, {@code false} otherwise
     */
    public boolean cancel() {
        boolean active = this.action != null;

        this.action = null;

        return active;
    }
}
