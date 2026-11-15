package org.fogofwarlabs.sc4j.engine.client.game;

import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates the read-only {@link GameView} a {@link GameSession} shows its game through: the engine default
 * {@link org.fogofwarlabs.sc4j.engine.client.view.StandardGameView} ({@link GameSessionFactory}'s standard constructor) or the mod's own
 * view, such as one that reads the mod's own systems.
 */
@FunctionalInterface
public interface GameViewFactory {
    /**
     * Creates the view of a session's game.
     *
     * @param game        the game
     * @param localPlayer the player of the person at this machine, or {@code null} for a session without local player
     * @return the created {@link GameView} instance
     */
    @NotNull GameView createView(@NotNull Game game, @Nullable Player localPlayer);
}
