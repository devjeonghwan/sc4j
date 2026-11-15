package org.fogofwarlabs.sc4j.engine.lockstep;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Checks a received turn against the local simulation: a turn that carries a sync value of its sender (a state hash, a mod's checksum record)
 * is compared with the local value of the same point. The engine {@link LockstepSession} compares the state hashes of its
 * {@link CommandFrame}s; a mod with its own turn protocol supplies its own check to a {@link DesyncMonitor}.
 * <p>
 * Determinism contract: a check only reads the turn and the local sync values; it changes no simulation state.
 */
@FunctionalInterface
public interface SyncCheck {
    /**
     * Checks one received turn.
     *
     * @param turn the received turn
     * @return the description of the mismatch, or {@code null} if the turn matches or carries no sync value
     */
    @Nullable String check(@NotNull PeerTurn turn);
}
