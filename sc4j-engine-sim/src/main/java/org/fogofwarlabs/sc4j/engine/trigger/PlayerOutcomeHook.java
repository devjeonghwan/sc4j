package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Hears that the standard trigger system decided about a player ({@link StandardTriggerSystem#OUTCOME}), for example to end the game screen of
 * a client or to stop a computer player.
 */
@FunctionalInterface
public interface PlayerOutcomeHook {
    /**
     * Called when a player's outcome changes from {@link PlayerOutcome#PLAYING}.
     *
     * @param player  the player
     * @param outcome the new outcome, {@link PlayerOutcome#VICTORY} or {@link PlayerOutcome#DEFEAT}
     */
    void onOutcome(@NotNull Player player, @NotNull PlayerOutcome outcome);
}
