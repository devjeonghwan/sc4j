package org.fogofwarlabs.sc4j.engine.game.frame;

import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.jetbrains.annotations.NotNull;

/**
 * Selects one frame phase of a game system: a system whose work spans several points of the frame registers each of
 * them as its own phase. The game resolves every selector once, when it is created, after its systems exist.
 *
 * @param <S> the system type
 */
@FunctionalInterface
public interface FramePhaseSelector<S extends GameSystem> {
    /**
     * Selects the phase of a system.
     *
     * @param system the system of the game being created
     * @return the phase, which the game runs as it is in every frame
     */
    @NotNull FramePhase selectPhase(@NotNull S system);
}
