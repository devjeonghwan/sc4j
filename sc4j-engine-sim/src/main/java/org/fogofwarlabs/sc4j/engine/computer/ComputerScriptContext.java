package org.fogofwarlabs.sc4j.engine.computer;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Carries what a {@link ComputerInstruction} reads while a computer script interpreter runs a script: the game, the player the running
 * script plays for, and in an interpreter's own context type, the further state of the running script (for example its position in the
 * program, its wait and its area).
 * <p>
 * An interpreter keeps one context and fills it before it executes an instruction; an instruction reads it only during the call and does
 * not keep it.
 */
public interface ComputerScriptContext {
    /**
     * @return the game whose computer players run
     */
    @NotNull Game getGame();

    /**
     * @return the player the running script plays for
     */
    @NotNull Player getPlayer();
}
