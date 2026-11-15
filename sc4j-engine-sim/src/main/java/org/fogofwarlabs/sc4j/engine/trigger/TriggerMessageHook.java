package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Receives the messages of standard trigger actions ({@link StandardTriggerSystem#MESSAGE}): the event a client shows as text. A message is an
 * event of the simulation, not state; a hook must not change the game.
 */
@FunctionalInterface
public interface TriggerMessageHook {
    /**
     * Called when a trigger displays a message to a player.
     *
     * @param player the player the message is for
     * @param text   the text
     */
    void onMessage(@NotNull Player player, @NotNull String text);
}
