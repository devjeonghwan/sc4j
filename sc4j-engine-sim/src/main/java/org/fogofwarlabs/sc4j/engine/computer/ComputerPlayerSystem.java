package org.fogofwarlabs.sc4j.engine.computer;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhase;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.hook.HookPoint;
import org.fogofwarlabs.sc4j.engine.lifecycle.UnitLifecycle;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs the computer players of a game: one {@link ComputerController} per computer slot, run in the system's frame phase. The engine fixes
 * the form: the system is a frame phase of the mod's phase list, a computer player is a {@link ComputerController} that acts through the
 * game's systems and submits no commands, and a computer player hears the simulation through the hook points of the other systems (for
 * example {@link UnitLifecycle#UNIT_REMOVED} and {@link UnitLifecycle#UNIT_ADOPTED}; a hook that is a system is attached with the systems,
 * {@link HookPoint}). What a computer player decides, how its decisions are scheduled within the phase and whether it runs scripts
 * ({@link ComputerInstruction}) are the mod's. A mod without computer players registers no computer player system.
 * <p>
 * Determinism contract: {@link #run} runs at the system's position in the frame phase list, in every frame its schedule names. It runs the
 * controllers in an order that depends only on the game state (the engine default: slot order), draws random values only from the game's
 * generator and allocates nothing per frame.
 */
public interface ComputerPlayerSystem extends GameSystem, FramePhase {
    /**
     * The key of the computer player system.
     */
    @NotNull SystemKey<ComputerPlayerSystem> KEY = SystemKey.of("computer-players", ComputerPlayerSystem.class);

    /**
     * Finds the controller of a player.
     *
     * @param player the player
     * @return the controller that plays the player, or {@code null} if no computer plays it
     */
    @Nullable ComputerController getController(@NotNull Player player);

    /**
     * Runs the computer players of the current frame.
     *
     * @param game the game
     */
    @Override
    void run(@NotNull Game game);
}
