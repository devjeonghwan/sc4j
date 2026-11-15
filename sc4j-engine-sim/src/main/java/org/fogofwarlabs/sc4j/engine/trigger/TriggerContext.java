package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Carries what a {@link TriggerCondition} or {@link TriggerAction} reads besides its own record while a trigger system evaluates triggers: the
 * game, the player whose triggers run, and in a trigger system's own context type, the further state of the evaluation (for example the mod's
 * timers, switches and locations).
 * <p>
 * A trigger system keeps one context per game and updates it while it walks the triggers; a condition or action reads it only during the
 * call and does not keep it.
 */
public interface TriggerContext {
    /**
     * @return the game whose triggers run
     */
    @NotNull Game getGame();

    /**
     * @return the player whose triggers run, or {@code null} outside the triggers of a player
     */
    @Nullable Player getCurrentPlayer();
}
