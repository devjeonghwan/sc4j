package org.fogofwarlabs.sc4j.engine.lockstep;

import org.jetbrains.annotations.NotNull;

/**
 * Supplies the local participant's outgoing turns: each call takes the commands queued since the previous call as the next turn.
 */
@FunctionalInterface
public interface TurnSource {
    /**
     * Takes the next outgoing turn: the queued commands, which the source no longer holds afterwards.
     *
     * @return the turn bytes
     */
    byte @NotNull [] takeTurn();
}
